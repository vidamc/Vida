# loader

Рантайм Vida: Java-агент, трансформер байткода и иерархия `ClassLoader`'ов. Связывает все остальные модули в одно работающее целое.

- Пакет: `dev.vida.loader`
- Gradle: `dev.vida:vida-loader`
- Стабильность: `@ApiStatus.Preview("loader")`

## Точки входа

### `VidaPremain`

Входные точки `premain` / `agentmain` для запуска под `-javaagent:vida-loader.jar`. Делегирует в `VidaBoot`.

```java
public final class VidaPremain {
    public static void premain(String args, Instrumentation inst) {
        VidaBoot.boot(BootOptions.parse(args, System.getProperties()), inst);
    }
}
```

Класс тонкий сознательно — мы не держим в нём логики, которую хочется тестировать. Всё интересное — в `VidaBoot` и `BootSequence`.

Манифест агента (`META-INF/MANIFEST.MF`):

```
Premain-Class: dev.vida.loader.VidaPremain
Agent-Class:   dev.vida.loader.VidaPremain
Can-Redefine-Classes: false
Can-Retransform-Classes: false
```

См. [architecture/bootstrap.md](../architecture/bootstrap.md#вход-vidapremain).

### `VidaBoot`

Программная инициализация Vida без участия JVM-агента. Для тестов, инструментов, интеграционных сценариев.

```java
try (VidaBoot.Handle handle = VidaBoot.boot(options)) {
    TransformingClassLoader loader = handle.loader();
    Class<?> cls = loader.loadClass("net.minecraft.server.MinecraftServer");
}
```

Создаёт свой `TransformingClassLoader` вместо регистрации transformer'а в `Instrumentation`. Полезно, когда JVM-агент недоступен (встроенный пользовательский рантайм, unit-тесты).

## BootSequence

Полный сценарий старта задокументирован в [architecture/bootstrap.md](../architecture/bootstrap.md#bootsequence). Короткая версия:

1. Парсим `BootOptions`; опционально загружаем [профиль платформы](./platform-profiles.md) (`platformProfile`, JVM-свойства).
2. Сканируем директорию модов (`discovery`).
3. Читаем манифесты (`manifest`).
4. Резолвим зависимости (`resolver`), предварительно публикуя [синтетические провайдеры платформы](#синтетические-провайдеры-платформы) (версия `minecraft` может подставиться из профиля, если не задана в опциях).
5. Читаем data-driven prototype-контент (`custom["vida:dataDriven"]` + datapack JSON) по резолвнутым модам.
6. Собираем `MorphIndex: target → List<MorphSource>` — [платформенные морфы](#платформенные-морфы) по профилю и морфы модов (с учётом `custom.vida.platformProfileIds`).
7. Загружаем Cartografía (client mappings) по стратегии профиля или из каталога игры.
8. Регистрируем `VidaClassTransformer` в `Instrumentation` (или через `TransformingClassLoader`).
9. Устанавливаем глобальный `LatidoBus` (`LatidoGlobal`) и **PlatformBridge** (`PlatformBridgeSupport` по FQCN из профиля или `VanillaBridge`).
10. Вызываем entrypoint'ы резолвнутых модов: `preLaunch` → `main` → `client` → `server`.
11. Собираем `VidaEnvironment` и возвращаем отчёт; дальше управление получает обычный `main` Minecraft.

## Синтетические провайдеры платформы

Перед запуском `Resolver.resolve()` загрузчик добавляет в `Universe` три «виртуальных» `Provider`-а, которые не являются модами, но позволяют модам декларировать зависимости от платформы без `ResolverError.Missing`:

| id          | Источник версии                                                             | Поведение                                          |
|-------------|-----------------------------------------------------------------------------|----------------------------------------------------|
| `vida`      | `BootOptions#vidaVersion()` → `META-INF/vida/loader-version.properties`    | Публикуется всегда; fallback `0.0.0` если ресурса нет. |
| `minecraft` | `BootOptions#minecraftVersion()` → иначе `gameVersion` из активного профиля → иначе `version.json` на classpath | Публикуется, если версия удаётся разрешить.        |
| `java`      | `System.getProperty("java.specification.version")`                          | Публикуется всегда; нормализация `"21"` → `21.0.0`. |

Если реальный мод объявил `id` из этого списка, синтетика для такого `id` пропускается — реальный мод выигрывает, без конфликта.

Запускалки передают версии одним из способов:

- системные свойства: `-Dvida.version=0.7.0 -Dvida.minecraftVersion=1.21.1`;
- агент-аргументы: `-javaagent:vida-loader-agent.jar=vidaVersion=0.7.0,minecraftVersion=1.21.1`;
- программно: `BootOptions.builder().vidaVersion("0.7.0").minecraftVersion("1.21.1")`.

Мод теперь может просто написать:

```json
{
  "dependencies": {
    "required": {
      "vida": ">=0.7",
      "minecraft": "^1.21.0",
      "java": ">=21"
    }
  }
}
```

Реализация: [`SyntheticProviders`](../../loader/src/main/java/dev/vida/loader/internal/SyntheticProviders.java).

## Платформенные морфы

Vida публикует базовые платформенные события без участия каких-либо модов — они идут прямо из загрузчика. Соответствующие Vifada-морфы живут в пакете `dev.vida.platform` и регистрируются в `MorphIndex` автоматически:

| Морф                       | Цель                                            | Событие                                                          |
|----------------------------|-------------------------------------------------|------------------------------------------------------------------|
| `MinecraftTickMorph`       | `net.minecraft.client.Minecraft#tick()V`        | `LatidoPulso` — каждый клиентский тик.                           |
| `GuiRenderMorph`           | `net.minecraft.client.gui.Gui#render(GuiGraphics,F)V` | `LatidoRenderHud` — каждый HUD-кадр.                       |

Фактический диспатч делегирован в `PlatformBridge`. Дефолтная реализация **`VanillaBridge`** ставится `BootSequence` после публикации глобальной шины: Minecraft по-прежнему **не** в compile-classpath модуля `:loader`, но в рантайме вызовы идут через **закэшированные `MethodHandle`** (`Minecraft` / `Window` / `Level`, `GuiGraphics.fill`), без **`Method.invoke`** на каждый тик или HUD-кадр. Тесты могут подменять мост через `VanillaBridge.install(myBridge)` (после `resetForTests()`).

`requireTarget = false` у обоих морфов: если целевой класс отсутствует (сервер-сайд, обфусцированный JAR, другая версия MC) — морф просто пропускается, ошибок в логе нет. Любой мод, подписавшийся на `LatidoPulso` через `@OyenteDeTick` или на `LatidoRenderHud` напрямую, начинает получать события без собственных MC-морфов.

Compile-only стабы нужных MC-классов (`net.minecraft.client.gui.GuiGraphics`) лежат в отдельном sourceSet `mcStubs` модуля `:loader` и явно исключаются из `agentJar` (`exclude "net/minecraft/**"`).

## Data-driven контент (Fuente, 2.0)

Парсинг декларативного контента вынесен в модуль [`:fuente`](./fuente.md) (`dev.vida.fuente`):

- `FuentePrototipoParser`
- `FuenteContenidoMod`
- `FuenteBloque`, `FuenteObjeto`, `FuenteRecetaShaped`, `FuenteLootTable`

Если в `vida.mod.json` присутствует:

```json
"custom": {
  "vida:dataDriven": {
    "enabled": true,
    "datapackRoot": "data/<mod-id>/vida"
  }
}
```

то loader читает JSON-файлы:

- `<root>/bloques/*.json`
- `<root>/objetos/*.json`
- `<root>/recipes/*.json` (только `type = "vida:shaped"`)
- `<root>/loot_tables/**/*.json` — минимальный разбор пулов (см. [manifest-schema.md](../reference/manifest-schema.md))

Разобранный snapshot публикуется в `VidaEnvironment.fuenteDataDriven()`.

## VidaClassTransformer

Реализация `ClassFileTransformer`:

```java
public byte[] transform(ClassLoader loader, String name, Class<?> cls,
                        ProtectionDomain domain, byte[] bytes) {
    // быстрый путь: если класса нет в индексе — null (JVM оставит оригинал)
    List<MorphSource> morphs = index.get(name);
    if (morphs == null || morphs.isEmpty()) return null;

    // дорогой путь: Vifada
    return Transformer.transform(bytes, morphs)
        .orElseLogAndReturn(bytes);  // при ошибке — оригинальные байты + ERROR в лог
}
```

Счётчики (`transformed`, `skipped`, `errors`) — `LongAdder` для минимального контеншна на hot-path. См. [architecture/performance.md](../architecture/performance.md).

## TransformingClassLoader

Классическая иерархия, описана в [architecture/classloading.md](../architecture/classloading.md). Три слоя:

- System/Agent — сам `vida-loader` и зависимости.
- JuegoLoader — Minecraft + `vida-base`.
- ModLoader × N — по одному на мод.

`TransformingClassLoader` — это `JuegoLoader` в программном варианте (путь через `VidaBoot`). В штатном запуске Minecraft мы используем `Instrumentation`-транформер, а иерархия создаётся на базе стандартных JDK-лоадеров плюс наши custom `ModLoader`.

## BrandingEscultor

Встроенный `Escultor`, переписывающий строку F3-дебаг-оверлея Minecraft. Пример низкоуровневой модификации байткода без `@VifadaMorph`. Класс **`dev.vida.escultores.BrandingEscultor`** (модуль `:escultores`, `@Stable`); загрузчик регистрирует singleton до Escultor'ов из поля манифеста [`escultores`](./escultores.md).

Логика:

1. `mightMatch(byte[] classFile)` — быстрый byte-scan: ищем в сыром `.class` ASCII-подстроку `"Minecraft %s (%s/%s%s)"`. Если не нашли — ничего не делаем.
2. `tryPatch(byte[] classFile)` — ASM-парсинг, поиск `LdcInsnNode` с нужной строкой, замена на `"Minecraft %1$s (vida)"`. Сериализация через `ClassWriter(0)` с `accept(cn, 0)` (сохранение stack-frames).

Этот же паттерн — «дешёвый pre-check, дорогая обработка» — рекомендуется всем авторам `Escultor` (см. [guides/escultores.md](../guides/escultores.md)).

## BootOptions

Иммутабельный контейнер параметров:

| Поле | Источник | Дефолт |
|------|----------|--------|
| `modsDir` | args / `vida.mods` / `VIDA_MODS` | `./mods` |
| `configDir` | args / `vida.config` / `VIDA_CONFIG` | `./config` |
| `dataDir` | args / `vida.data` / `VIDA_DATA` | `./run/vida` |
| `vidaVersion` | args `vidaVersion=…` / `vida.version` | bundled `loader-version.properties` |
| `minecraftVersion` | args `minecraftVersion=…` / `vida.minecraftVersion` | — (синтетика `minecraft` не публикуется) |
| `logLevel` | args / `vida.log.level` / `VIDA_LOG_LEVEL` | `INFO` |
| `debug.dumpClasses` | args / `vida.debug.dumpClasses` | `false` |
| `debug.verifyClasses` | args / `vida.debug.verifyClasses` | `false` |
| `resolver.timeoutMs` | args / `vida.resolver.timeoutMs` | `5000` |
| `discovery.maxThreads` | args / `vida.discovery.maxThreads` | `Runtime.availableProcessors()` |

Приоритет: агент-аргументы → системные свойства → env → дефолты.

## BootRegistry

После завершения `BootSequence` публикуется `BootRegistry` — канал, через который `vida-base` получает:

- `Resolution` — финальный список модов;
- `MorphIndex` — для диагностики;
- `BootOptions` — чтобы `ModContext` знал, где `dataDir`.

`BootRegistry` синглтон, `@Internal`. Моды не должны обращаться к нему напрямую — всё нужное приходит через `ModContext`.

## Почему агент «ничего не делает» в рантайме

Подробный ответ — [architecture/performance.md](../architecture/performance.md#правило-нуля). Короткий: после `BootSequence` возврата агент не имеет работы. `transform` вызывается только при загрузке новых классов; если класс не в индексе — выходим за одну проверку. `Can-Retransform-Classes: false` гарантирует, что JVM не позовёт нас повторно.

## Что читать дальше

- [architecture/bootstrap.md](../architecture/bootstrap.md) — детальный разбор boot-фазы.
- [architecture/classloading.md](../architecture/classloading.md) — слои лоадеров.
- [architecture/performance.md](../architecture/performance.md) — решения по производительности.
- [guides/escultores.md](../guides/escultores.md) — низкоуровневая модификация на примере `BrandingEscultor`.
