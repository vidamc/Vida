# Архитектура: bootstrap / VidaPremain

Как Vida перехватывает запуск JVM и успевает подготовить всё до первой строки Minecraft.

## Почему Java Agent, а не LaunchWrapper

Альтернатив у загрузчика модов под Java Edition ровно две: либо свой main-класс, который подгружает classpath и делегирует в `net.minecraft.client.main.Main`, либо Java Agent.

Свой main-класс означает:

- Vida обязана знать формат CLI игры и поддерживать его для каждой версии отдельно.
- Лаунчер должен явно менять `mainClass` версии — значит, инсталлятор меняет `version.json` Minecraft, а это хрупко и трудно откатить.
- Agent API (`Instrumentation`) становится недоступен — `ClassFileTransformer` приходится дёргать вручную из кастомного лоадера, что усложняет изоляцию.

Java Agent (`-javaagent:vida-loader.jar`) даёт всё это бесплатно: JVM сама зовёт нас до `main`, у нас есть `Instrumentation`, а лаунчеру достаточно прописать `-javaagent` в JVM-аргументах профиля/инстанса. `BrandingEscultor` патчит один класс Minecraft и даёт чистый F3 — без модификации JAR'а игры.

## Вход: `VidaPremain`

```java
public final class VidaPremain {
    public static void premain(String args, Instrumentation inst) {
        VidaBoot.boot(BootOptions.parse(args, System.getProperties()), inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }
}
```

`agentmain` существует для attach-time-сценариев (тесты, инструменты), но штатный путь — всегда `premain`. Код агента намеренно тонкий: не держать ссылок на неинициализированные классы, не бросать исключений из `premain` кроме случаев, когда продолжение невозможно.

Манифест агента (`loader/src/main/resources/META-INF/MANIFEST.MF`):

```
Premain-Class: dev.vida.loader.VidaPremain
Agent-Class:   dev.vida.loader.VidaPremain
Can-Redefine-Classes: false
Can-Retransform-Classes: false
Boot-Class-Path:
```

`Can-Retransform-Classes: false` — ключевое решение, подробно разобрано в [performance.md](./performance.md#почему-нет-retransform).

## `BootOptions`

Парсится из:

1. Аргументов агента (`-javaagent:vida-loader.jar=mods=./mods,log=DEBUG`).
2. Системных свойств (`-Dvida.mods=./mods -Dvida.log.level=DEBUG`).
3. Переменных окружения (`VIDA_MODS`, `VIDA_LOG_LEVEL`).

Приоритет: агент-аргументы → системные свойства → env → дефолты. Все значения иммутабельны после парсинга; никакого in-place изменения конфигурации в рантайме.

Базовые опции:


| Ключ                | Что означает                                 | Дефолт       |
| ------------------- | -------------------------------------------- | ------------ |
| `mods`              | директория с модами                          | `./mods`     |
| `config`            | директория конфигов                          | `./config`   |
| `data`              | директория для данных модов                  | `./run/vida` |
| `log.level`         | уровень логгера `vida.*`                     | `INFO`       |
| `debug.dumpClasses` | сохранять патченные классы в `<data>/dumps/` | `false`      |


## `BootSequence`

```
BootSequence.run(options, inst):
    1. Log.init(options)
    2. report = ModScanner.scan(options.modsDir())
    3. manifests = ManifestParser.parseAll(report.successes())
    4. universe = ManifestAdapter.toUniverse(manifests)
    5. resolution = Resolver.resolve(universe, report.roots(), options.resolver())
    6. morphIndex = MorphIndex.build(resolution, options)
    7. inst.addTransformer(new VidaClassTransformer(morphIndex), /*canRetransform*/ false)
    8. BootRegistry.publish(resolution, morphIndex)  // для base-модуля
```

Шаги 2–5 строго последовательны: без полной резолюции нельзя знать, какие морфы применять. Внутри шагов много параллелизма:

- Скан — по одному воркеру на jar.
- Парсинг манифестов — `parallelStream()` по успешным кандидатам.
- Резолвер работает в одном потоке (бэктрекинг плохо распараллеливается), но лимитирован по времени (`ResolverOptions.timeoutMs`).

## `MorphIndex`

Это мапа `target-FQCN → List<MorphSource>`, рассчитанная один раз за загрузку. Благодаря ей `VidaClassTransformer.transform(...)` не делает никаких вычислений на горячем пути — только `Map.get(className)`; если там `null`, трансформер моментально возвращает `null` (значит, JVM использует оригинальные байты).

Это «ленивое» применение трансформаций: класс патчится только когда JVM его *реально* загружает. Моды, классы которых никогда не трогает игрок, не дают никаких накладных расходов в рантайме.

## Что происходит дальше

После `return` из `premain` JVM отдаёт управление своему загрузчику классов и вызывает `main`-метод оригинального `mainClass` (для клиента — `net.minecraft.client.main.Main`). Minecraft начинает стартовать как обычно; Vida вступает в дело только когда какой-то класс с записью в `MorphIndex` доходит до `ClassLoader.defineClass(...)`.

## Программный boot: `VidaBoot`

Для тестов и инструментов у Vida есть путь без JVM-агента:

```java
BootOptions options = BootOptions.builder()
    .modsDir(Path.of("build/test-mods"))
    .build();

try (VidaBoot.Handle handle = VidaBoot.boot(options)) {
    TransformingClassLoader loader = handle.loader();
    Class<?> cls = loader.loadClass("net.minecraft.server.MinecraftServer");
    // cls уже с применёнными морфами
}
```

`VidaBoot` создаёт `TransformingClassLoader` вместо регистрации `ClassFileTransformer` в `Instrumentation` — это даёт полную изоляцию для интеграционных тестов.

## Ошибки при загрузке


| Где                                                   | Что происходит | Что делает загрузчик                                                       |
| ----------------------------------------------------- | -------------- | -------------------------------------------------------------------------- |
| `ModScanner` не смог прочитать JAR                    | fail-one       | лог `WARN`, jar игнорируется                                               |
| `ManifestParser` нашёл невалидный `vida.mod.json`     | fail-one       | лог `ERROR`, jar игнорируется, мод исчезает из `Universe`                  |
| `Resolver` не смог найти решение                      | fail-all       | лог `ERROR` с диагностикой конфликта, Vida **не передаёт управление игре** |
| `ClassFileTransformer.transform(...)` кинул exception | fail-one       | лог `ERROR`, JVM получает `null` → оригинальные байты                      |


«`fail-all` в резолвере — это намеренный выбор: запуск игры с частично разрешёнными зависимостями приведёт к рантайм-крашу в случайном месте, что хуже любой диагностики при старте.