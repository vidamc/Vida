# Модули

Каждая страница в этом разделе — обзор одного Gradle-подпроекта Vida: роль, ключевые классы, стабильность API, типовые сценарии. Это не замена Javadoc — это то, что Javadoc не умеет: контекст, границы ответственности и кросс-ссылки.

**Полный список файлов** `docs/modules/*.md` с однострочными описаниями — в разделе «Модули» на [главной странице документации](../index.md).

## Ядро

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`core`](./core.md) | `@Stable` | Базовые типы: `Identifier`, `Version`, `Result`, `Log`, `ApiStatus`. |
| [`manifest`](./manifest.md) | `@Stable` | `vida.mod.json` и его парсер без сторонних JSON-библиотек. |
| [`config`](./config.md) | `@Stable` | Ajustes — иммутабельный снимок конфигурации с профилями. |
| [`cartografia`](./cartografia.md) | `@Stable` | Маппинги Mojang↔intermediary, `.ctg` формат, ASM-ремаппер. |
| [`discovery`](./discovery.md) | `@Stable` | Сканер `mods/`, nested-JAR, кэш `mods.idx`. |
| [`resolver`](./resolver.md) | `@Stable` | Бэктрекинг по SemVer; pin/exclude; политика `accessDeniedIds`. |

## Рантайм

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`vifada`](./vifada.md) | `@Stable` (2.0+) | Байткод-трансформер; Vifada 2: `@VifadaMulti`, `@VifadaLocal`, `@VifadaRedirect`. |
| [`loader`](./loader.md) | `@Preview("loader")` | Java-агент `VidaPremain`, `TransformingClassLoader`, `BootSequence`. |
| [`fuente`](./fuente.md) | `@Stable` (2.0+) | Data-driven парсер JSON из datapack (`dev.vida.fuente`), включая loot tables. |
| [`escultores`](./escultores.md) | `@Stable` | Интерфейс `Escultor`, `BrandingEscultor`; декларация в `vida.mod.json`. |

## Публичный API

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`base`](./base.md) | `@Stable` (1.0+) | `VidaMod`, `ModContext`, Latidos (+ `Ejecutor`), Catalogo, Ajustes. |
| [`bloque`](./bloque.md) | `@Stable` (1.0+) | Блоки: `Bloque`, `PropiedadesBloque`, `FormaColision`, `RegistroBloques`. |
| [`objeto`](./objeto.md) | `@Stable` (1.0+) | Предметы: `Objeto`, data-components, `Material`, `Herramienta`, `ObjetoDeBloque`. |
| [`entidad`](./entidad.md) | `@Stable` (2.0+) | Сущности: `Entidad`, `TipoEntidad`, `PropiedadesEntidad`, entity data-components. |
| [`mundo`](./mundo.md) | `@Stable` (2.0+) | Мир: `Mundo`, `Coordenada`, `ChunkCoordenada`, `RegionCoordenada`, `LimitesVerticales`, `Dimension`, `Bioma`, `LatidosMundo`. |
| [`render`](./render.md) | `@Stable` (2.0+) | Клиентский render API: `ModeloBloque`, `ModeloEntidad`, `TextureAtlas`, `RenderPipeline`. |
| [`red`](./red.md) | `@Stable` | Tejido: пакетные маркеры, auto record-codec, versioning и back-pressure. |
| [`susurro`](./susurro.md) | `@Stable` (1.0+) | Управляемый thread-pool с приоритетами, back-pressure и `HiloPrincipal`. |
| [`puertas`](./puertas.md) | `@Stable` (1.0+) | Access wideners `.ptr`: парсер + ASM-аппликатор. |

## Инструменты

| Модуль | Стабильность | Одно предложение |
|--------|--------------|------------------|
| [`platform-profiles`](./platform-profiles.md) | контракт превью | Версионируемые дропы MC + Cartografía + платформенные морфы; дерево `platform-profiles/generations/`. |
| [`bom`](./bom.md) | `@Stable` | Java Platform BOM `dev.vida:vida-bom` для согласованных версий API-модулей. |
| [`gradle-plugin`](./gradle-plugin.md) | `@Stable` (1.0+) | Плагин `dev.vida.mod` (+ `vidaValidatePuertas`, `vidaPackagePuertas`, `vidaRun`, hot reload в dev). |
| [`installer`](./installer.md) | `@Preview("installer")` | GUI + CLI: Mojang, Prism, MultiMC, ATLauncher, Modrinth / CurseForge; offline-кэш loader; `--validate-puertas`. |
| [`vigia`](./vigia.md) | `@Stable` (2.0+, основной API) | JFR-профайлер, HTML-отчёты, `/vida profile` *(инструмент платформы)*. |

## Граф зависимостей

```
core ← manifest ← discovery ← resolver
core ← config
core ← cartografia ← loader
core ← vifada ← loader
core ← bloque ← fuente
core ← objeto ← fuente
manifest + discovery ← fuente
core ← puertas
core ← susurro ← base
manifest + resolver + vifada → loader → base
loader ← escultores
loader ← fuente
loader ← mundo
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
