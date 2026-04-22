# Модули

Каждая страница в этом разделе — обзор одного Gradle-подпроекта Vida: роль, ключевые классы, стабильность API, типовые сценарии. Это не замена Javadoc — это то, что Javadoc не умеет: контекст, границы ответственности и кросс-ссылки.

## Ядро

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`core`](./core.md) | `@Stable` | Базовые типы: `Identifier`, `Version`, `Result`, `Log`, `ApiStatus`. |
| [`manifest`](./manifest.md) | `@Stable` | `vida.mod.json` и его парсер без сторонних JSON-библиотек. |
| [`config`](./config.md) | `@Stable` | Ajustes — иммутабельный снимок конфигурации с профилями. |
| [`cartografia`](./cartografia.md) | `@Stable` | Маппинги Mojang↔intermediary, `.ctg` формат, ASM-ремаппер. |
| [`discovery`](./discovery.md) | `@Stable` | Сканер `mods/`, nested-JAR, кэш `mods.idx`. |
| [`resolver`](./resolver.md) | `@Stable` | SAT-бэктрекинг по SemVer-диапазонам. |

## Рантайм

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`vifada`](./vifada.md) | `@Preview("vifada")` | Байткод-трансформер: `@VifadaMorph`, `@VifadaInject`, `@VifadaShadow`. |
| [`loader`](./loader.md) | `@Preview("loader")` | Java-агент `VidaPremain`, `TransformingClassLoader`, `BrandingEscultor`. |

## Публичный API

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`base`](./base.md) | `@Preview("base")` | `VidaMod`, `ModContext`, Latidos (+ `Ejecutor`), Catalogo, Ajustes. |
| [`bloque`](./bloque.md) | `@Preview("bloque")` | Блоки: `Bloque`, `PropiedadesBloque`, `FormaColision`, `RegistroBloques`. |
| [`objeto`](./objeto.md) | `@Preview("objeto")` | Предметы: `Objeto`, data-components, `Material`, `Herramienta`, `ObjetoDeBloque`. |
| [`entidad`](./entidad.md) | `@Preview("entidad")` | Сущности: `Entidad`, `TipoEntidad`, `PropiedadesEntidad`, entity data-components. |
| [`mundo`](./mundo.md) | `@Preview("mundo")` | Мир: `Mundo`, `Coordenada`, `Dimension`, `Bioma`, `LatidosMundo`. |
| [`render`](./render.md) | `@Preview("render")` | Клиентский render API: `ModeloBloque`, `ModeloEntidad`, `TextureAtlas`, `RenderPipeline`. |
| [`red`](./red.md) | `@Preview("red")` | Сеть: пакетные маркеры, auto record-codec, versioning и back-pressure. |
| [`susurro`](./susurro.md) | `@Preview("susurro")` | Управляемый thread-pool с приоритетами, back-pressure и `HiloPrincipal`. |
| [`puertas`](./puertas.md) | `@Preview("puertas")` | Access wideners `.ptr`: парсер + ASM-аппликатор. |

## Инструменты

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`gradle-plugin`](./gradle-plugin.md) | `@Preview` | Плагин `dev.vida.mod` для сборки мода (+ `vidaValidatePuertas`, `vidaPackagePuertas`). |
| [`installer`](./installer.md) | `@Preview` | GUI + CLI: Mojang, Prism, MultiMC, ATLauncher, **Modrinth App**, **CurseForge App**; `--validate-puertas`. |
| [`vigia`](./vigia.md) | `@Preview` | JFR-профайлер, HTML-отчёты, `/vida profile` *(платформа, не грузится через installer как мод)*. |

## Граф зависимостей

```
core ← manifest ← discovery ← resolver
core ← config
core ← cartografia ← loader
core ← vifada ← loader
core ← puertas
core ← susurro ← base
manifest + resolver + vifada → loader → base
base ← bloque
base + bloque ← objeto
base ← entidad
base ← mundo
core + bloque + entidad ← render
core ← red
```

Контракт: ни один модуль из левого столбца не зависит от модулей справа. `core` — всегда нижний корень.

Полный граф компиляции и рантайма — в [architecture/overview.md](../architecture/overview.md#карта-модулей).

## Аннотации стабильности

Каждый публичный пакет аннотирован одним из:

- `@ApiStatus.Stable` — breaking changes только в мажоре.
- `@ApiStatus.Preview("name")` — может измениться до 1.0.0; изменения — в `CHANGELOG.md`.
- `@ApiStatus.Internal` — вне контракта, меняется свободно.

Подробный разбор и правила использования — [reference/api-stability.md](../reference/api-stability.md).
