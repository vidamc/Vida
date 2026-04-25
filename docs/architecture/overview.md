# Архитектура: обзор

Vida — монорепо из десятка тесно связанных, но независимо тестируемых модулей. Эта страница даёт плоскую карту: что есть, кто от кого зависит, как это запускается.

Если вы читаете Vida впервые — начните отсюда и дальше переходите к [bootstrap](./bootstrap.md) и [classloading](./classloading.md).

## Карта модулей

```
                 ┌──────────────┐
                 │     core     │  базовые типы (Identifier, SemVer, Result, Log, ApiStatus)
                 └──────┬───────┘
          ┌─────────────┼──────────────┬──────────────┐
          ▼             ▼              ▼              ▼
    ┌──────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
    │ manifest │ │   config   │ │ cartografia│ │   vifada   │
    └────┬─────┘ └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
         │             │              │              │
         └──────┬──────┴──────────────┼──────────────┘
                ▼                     │
          ┌───────────┐               │
          │ discovery │               │
          └─────┬─────┘               │
                ▼                     │
          ┌──────────┐                │
          │ resolver │                │
          └─────┬────┘                │
                ▼                     │
          ┌─────────────────────────────────┐
          │              loader             │  VidaPremain + TransformingClassLoader
          └─────┬─────────────────────┬─────┘
                ▼                     ▼
          ┌─────────┐           ┌───────────┐
          │  base   │           │ escultores│  Escultor API + BrandingEscultor
          └─────────┘           └───────────┘
                ▲
                │ compileOnly
       ┌────────┴─────────┐
       │  gradle-plugin   │  dev.vida.mod
       └──────────────────┘

                   ┌───────────┐
                   │ installer │  самостоятельный GUI + CLI,
                   │           │  использует core, manifest
                   └───────────┘
```

Стрелки — Gradle-зависимости времени компиляции. Рантайм-граф тот же, плюс `base` загружается в `JuegoLoader`, а моды — в отдельных `ModLoader`'ах ([classloading.md](./classloading.md)).

## Роль каждого модуля

| Модуль | Одно предложение | Подробно |
|--------|------------------|----------|
| `core` | Базовые типы: `Identifier`, `Version`, `Result`, `Log`, `ApiStatus`. Без сторонних зависимостей. | [modules/core.md](../modules/core.md) |
| `manifest` | Парсер `vida.mod.json`, собственный JSON без внешних библиотек. | [modules/manifest.md](../modules/manifest.md) |
| `config` | Ajustes: TOML-снимок, профили, dotted-path. | [modules/config.md](../modules/config.md) |
| `cartografia` | Маппинги Mojang↔intermediary, `.ctg` формат, ASM-ремаппер. | [modules/cartografia.md](../modules/cartografia.md) |
| `discovery` | Сканер `mods/`, чтение nested-JAR, кэш `mods.idx`. | [modules/discovery.md](../modules/discovery.md) |
| `resolver` | SAT-бэктрекинг по SemVer-диапазонам. | [modules/resolver.md](../modules/resolver.md) |
| `vifada` | `@VifadaMorph` и друзья, transformer байткода. | [modules/vifada.md](../modules/vifada.md) |
| `loader` | Java-агент, `TransformingClassLoader`, `BootSequence`. | [modules/loader.md](../modules/loader.md) |
| `fuente` | Data-driven парсинг JSON из datapack (`dev.vida.fuente`). | [modules/fuente.md](../modules/fuente.md) |
| `escultores` | Интерфейс `Escultor`, встроенный `BrandingEscultor`. | [modules/escultores.md](../modules/escultores.md) |
| `base` | Публичный API: `VidaMod`, `ModContext`, Latidos, Catalogo, Ajustes. | [modules/base.md](../modules/base.md) |
| `entidad` | Декларативный entity API: `Entidad`, hitbox, масса, entity data-components. | [modules/entidad.md](../modules/entidad.md) |
| `mundo` | World API: `Mundo`, координаты (`Coordenada`, `ChunkCoordenada`, `RegionCoordenada`), `LimitesVerticales`, `Dimension`, `Bioma`, world-латидосы. | [modules/mundo.md](../modules/mundo.md) |
| `render` | Render API: модели блоков/сущностей, texture-atlas, shader-hooks. | [modules/render.md](../modules/render.md) |
| `red` | Tejido API: пакеты, record-codec, versioning, back-pressure. | [modules/red.md](../modules/red.md) |
| `gradle-plugin` | `dev.vida.mod` — DSL + таски для мода. | [modules/gradle-plugin.md](../modules/gradle-plugin.md) |
| `installer` | GUI/CLI установщик, мульти-лаунчер. | [modules/installer.md](../modules/installer.md) |

## Последовательность запуска (высокий уровень)

Детальный разбор — в [bootstrap.md](./bootstrap.md). Короткий скелет:

1. JVM видит `-javaagent:vida-loader.jar` и вызывает `VidaPremain.premain(args, inst)`.
2. `BootSequence` парсит `BootOptions` (из args и системных свойств).
3. `discovery` сканирует `mods/`, читает вложенные JAR.
4. Каждый JAR превращается в `ModCandidate` через `manifest`.
5. `resolver` строит `Universe`, ищет валидную резолюцию.
6. Loader читает data-driven prototype-контент (`custom["vida:dataDriven"]` + datapack JSON), если включён.
7. Из резолюции собирается `MorphIndex`: мапа `target-class → List<MorphSource>`.
8. `VidaClassTransformer` регистрируется в `Instrumentation` с `canRetransform=false`.
9. Агент возвращается, JVM продолжает загружать main-класс игры.
10. При первом обращении к целевому классу `VidaClassTransformer` применяет Vifada и отдаёт новые байты.
11. Как только загружен класс entrypoint мода — Vida создаёт экземпляр `VidaMod`, вызывает `iniciar(ModContext)`.

После шага 10 загрузчик не делает ничего в hot-path. Причины — в [performance.md](./performance.md).

## Жизненный цикл мода

```
[DESCUBIERTO]  ─ candidate найден в mods/
      │
      ▼
[RESUELTO]     ─ прошёл dependency resolution
      │
      ▼
[CARGADO]      ─ классы мода загружены в свой ModLoader
      │
      ▼
[INICIADO]     ─ iniciar(ctx) вернулся успешно
      │
      ▼
[ACTIVO]       ─ игра запущена, мод получает события
      │
      ▼
[DETENIDO]     ─ detener(ctx) вернулся; ресурсы освобождены
```

Подробнее — [lifecycle.md](./lifecycle.md).

## Иерархия ClassLoader'ов

Три слоя изоляции:

```
System / Agent  ← vida-loader + его транзитивные зависимости (ASM, SLF4J, tomlj)
      │
      ▼
JuegoLoader     ← Minecraft + vida-base (общий контракт между модами)
      │
      ▼
ModLoader × N   ← по одному на мод, parent = JuegoLoader
```

Моды не видят друг друга напрямую: взаимодействие идёт только через API (`Latidos`, `Catalogo`), загруженный в `JuegoLoader`. Это даёт гарантию, что два мода не могут случайно подменить транзитивную зависимость третьему. Подробнее — [classloading.md](./classloading.md).

## Где живёт API моддера

В модуле `base` — и только там. Мод в своём `build.gradle.kts` ставит `compileOnly("dev.vida:vida-base:<ver>")` и этим исчерпывается его знание о Vida. Никаких имплементационных деталей — ни `TransformingClassLoader`, ни `BootSequence`, ни даже пакета `dev.vida.loader.internal` — мод не видит и видеть не должен.

В рантайме `vida-base` живёт в `JuegoLoader`, так что все моды пользуются одним и тем же экземпляром `LatidoBus`, одним `CatalogoManejador` и одним `ModContext`-фабриком. Это и есть граница «публичного» API — всё, что попадает в `base`, обязано соответствовать [api-stability.md](../reference/api-stability.md).

## Что читать дальше

- [bootstrap.md](./bootstrap.md) — агент `VidaPremain` шаг за шагом.
- [classloading.md](./classloading.md) — три лоадера и почему их три.
- [lifecycle.md](./lifecycle.md) — состояния мода и гарантии между ними.
- [performance.md](./performance.md) — решения, которые влияют на hot-path.
