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

Дополнительно для профилей платформы см. `-Dvida.platformProfile`, `-Dvida.minecraftVersion`, `-Dvida.clientJar` в документации [modules/platform-profiles.md](../modules/platform-profiles.md).


## `BootSequence`

Цепочка в коде (упрощённо, см. `dev.vida.loader.internal.BootSequence`):

```
BootSequence.run(options, inst):
    0. (опционально) загрузить профиль платформы; проверить minimumJavaVersion и clientJar.sha256
    1. Discovery: ModScanner по modsDir и extraSources
    2. Resolver.resolve с синтетическими провайдерами vida / minecraft / java
    3. Data-driven prototype-контент (Fuente) по резолвнутым манифестам
    4. Сбор MorphIndex: платформенные морфы + морфы модов (фильтр custom.vida.platformProfileIds)
    5. Загрузка client_mappings → obf-карта + дерево Cartografía; fingerprint кеша дополняется id профиля
    6. VidaClassTransformer, JuegoLoader, ModLoader'ы модов, декларативные Escultor'ы
    7. instrumentation.addTransformer(...) при наличии Agent
    8. LatidoBus, PlatformBridge через PlatformBridgeSupport из дескриптора (или VanillaBridge)
    9. Entrypoint'ы модов
    10. Сборка VidaEnvironment и возврат BootReport
```

Шаги discovery → resolve → fuente → морфы → маппинги → трансформер строго упорядочены: индекс морфов и Cartografía нужны до загрузки игровых классов через `JuegoLoader`. Внутри отдельных шагов возможен ограниченный параллелизм (скан jar, и т.д.):

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