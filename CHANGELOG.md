# Changelog

Все заметные изменения в проекте Vida документируются здесь.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версионирование соответствует [SemVer 2.0.0](https://semver.org/lang/ru/).

Типы изменений:

- **Added** — новая функциональность.
- **Changed** — изменения в существующей функциональности.
- **Deprecated** — функциональность, помеченная как устаревшая.
- **Removed** — удалённая функциональность.
- **Fixed** — исправления ошибок.
- **Security** — изменения, связанные с безопасностью.

---

## [0.9.1](https://github.com/vidamc/Vida/compare/v0.9.0...v0.9.1) (2026-04-22)


### Features

* ремаппинг обфусцированных имён для Vifada-морфов ([9eaf41b](https://github.com/vidamc/Vida/commit/9eaf41ba4d063b141d70cccc83bbe0dadeef4f52))


### Continuous Integration

* авторелиз — release-please включает auto-merge на Release PR ([4599748](https://github.com/vidamc/Vida/commit/45997489af194bdfdbec05da3c511220d18c9eb0))

## [0.9.0] — 2026-04-22

Valenta — Sodium-class rendering optimization mod. Полноценный аналог Sodium + Sodium Extra в одном моде.

### Added

#### `:mods:valenta` — render engine

- **Render core** (`dev.vida.mods.valenta.core`):
  - `CompactVertexFormat` — compact 16-byte vertex format (vs vanilla 28 bytes): position (short3), normal (byte2), color (RGBA8), texcoord (short2). ~43% VRAM bandwidth reduction.
  - `GpuBuffer` — typed OpenGL buffer object wrapper (VBO, SSBO, etc.) с auto-grow и amortised re-allocation.
  - `IndirectDrawBuffer` — CPU-side command buffer для `glMultiDrawElementsIndirect`.
  - `VboMallaBatcher` — mega-VBO batcher: groups all visible sections into one draw call.
  - `BiomeBlendSsbo` / `BlockLightSsbo` — SSBO-backed per-section biome colors and block light values.
  - `GlFunctions` — injectable GL abstraction с `Noop` implementation для headless testing.

- **Chunk meshing** (`dev.vida.mods.valenta.chunk`):
  - `MallaChunk` — immutable mesh record с compact vertex/index data, builder pattern.
  - `AnalisisEtapa` — render-thread scan: dirty sections sorted by camera distance, priority-capped.
  - `BuildEtapa` — worker-thread greedy mesher: face culling against opaque neighbours, biome blend + light data generation.
  - `UploadEtapa` — render-thread transfer: mesh → VBO, biome → SSBO, light → SSBO с per-frame upload cap.
  - `ChunkTaskGraph` — three-stage orchestrator (`Analisis → Build → Upload`) через `Susurro` с `Etiqueta.de("valenta/chunk")` back-pressure.

- **Culling** (`dev.vida.mods.valenta.culling`):
  - `ValentaFrustum` — Gribb-Hartmann frustum plane extraction + AABB test (~5 ns/section).
  - `OcclusionQuery` — GL `SAMPLES_PASSED` с one-frame-delayed readback и conditional render support.
  - `PvsTree` — Potentially Visible Set via portal flood-fill; compressed sorted-key entries.
  - `CullingEngine` — three-tier gate (PVS → Frustum → Occlusion) с per-frame statistics.

- **Sky** (`dev.vida.mods.valenta.sky`):
  - `SkyRenderer` — `glInvalidateFramebuffer` when camera fully enclosed by opaque geometry.

- **Quality of life** (`dev.vida.mods.valenta.quality`):
  - `ParticleFilter` — none / reduce (every Nth) / hide all particles.
  - `CloudRenderer` — vanilla / fast (flat planes) / disabled.
  - `AnimatedTextureManager` — toggle animated texture ticking.
  - `RenderDistanceManager` — smooth ±1 section/4 frames transitions, minimum safe distance.
  - `GpuTimingPane` — F3 debug pane with per-pass nanosecond timings.

- **Vifada morphs** (`dev.vida.mods.valenta.escultor`):
  - `LevelRendererMorph` (priority 900) — hooks `setupRender` и `renderLevel`.
  - `GameRendererMorph` (priority 900) — hooks `renderLevel`, applies render distance override.
  - `ChunkRendererMorph` (priority 900) — redirects `SectionRenderDispatcher.runTask`.
  - `ParticleMorph` (priority 800) — `ParticleEngine.add` filter.
  - `CloudMorph` (priority 800) — `LevelRenderer.renderClouds` skip/replace.
  - `ValentaHooks` — static hook registry с volatile null-check pattern, zero-alloc hot path.
  - Все морфы используют `requireTarget = false` для graceful degradation.

- **Debug** (`dev.vida.mods.valenta.debug`):
  - `ValentaDebugComando` — `/valenta debug occlusion|gpu|stats`.
  - `OcclusionOverlay` — HUD overlay визуализации culling statistics.

- **Configuration**: `valenta.toml` с секциями `[render]`, `[chunks]`, `[culling]`, `[sky]`, `[quality]`, `[debug]`.
- **Puertas**: `valenta.ptr` — ~30 vanilla access-wideners для renderer injection.
- **MC stubs**: compile-only заглушки `LevelRenderer`, `GameRenderer`, `SectionRenderDispatcher`, `Frustum`, `ParticleEngine`, `GlStateManager` и др.

### Tests

- Unit tests: `CompactVertexFormatTest`, `GpuBufferTest`, `IndirectDrawBufferTest`, `VboMallaBatcherTest`, `AnalisisEtapaTest`, `ValentaFrustumTest`, `OcclusionQueryTest`, `PvsTreeTest`, `ParticleFilterTest`, `RenderDistanceManagerTest`, `ValentaConfigTest`.
- Property-based (jqwik): `MallaChunkPropertyTest` — vertex buffer sizing, index divisibility, section key uniqueness, builder invariants.

### Benchmarks

- JMH suite: `VertexFormatBenchmark` (encode/decode throughput), `CullingBenchmark` (frustum 10K sections), `MeshingBenchmark` (sparse/dense/solid sections).

### Documentation

- `mods/valenta/README.md` — features, config, build instructions, benchmark commands.
- `mods/valenta/CHANGELOG.md` — initial release notes.
- `docs/mods/valenta/architecture.md` — internal design: pipeline flow, vertex format, multi-draw indirect, culling tiers, threading model.
- `docs/mods/valenta/compat-matrix.md` — tested configurations, known conflicts, GPU driver notes.

---

## [0.8.0] — 2026-04-22

Платформа: закрыты три критических пробела в MC-мосту — синтетические провайдеры зависимостей, платформенные Vifada-морфы и автоматическая установка `VanillaBridge`. Теперь любой мод может объявить `required.vida` / `required.minecraft` / `required.java` и получить `LatidoPulso` / `LatidoRenderHud` без собственных морфов.

### Added

#### `:loader` — синтетические провайдеры зависимостей

- `SyntheticProviders` (`dev.vida.loader.internal`) — собирает `Provider`-ы для `vida`, `minecraft`, `java` и инжектит их в `Universe` до вызова `Resolver.resolve()`.
- Канонизация строки версии: `"21"` → `21.0.0`, `"1.21"` → `1.21.0`, сохранение `prerelease`/`build`-суффиксов; невалидный вход — `Optional.empty()`.
- Источники версий: `BootOptions.vidaVersion()` ↓ встроенный ресурс `META-INF/vida/loader-version.properties` (стемпится `processResources` через `expand` с `project.version`) ↓ fallback `0.0.0`; `BootOptions.minecraftVersion()`; `System.getProperty("java.specification.version")`.
- `BootOptions.Builder#vidaVersion(String)` и `#minecraftVersion(String)` — публичный API для запускалок и тестов; пустая строка возвращает поле к авто-детекту.
- `VidaPremain` принимает эти версии через `-Dvida.version=…` / `-Dvida.minecraftVersion=…` и агент-аргументы `vidaVersion=…,minecraftVersion=…`.
- Реальный мод с тем же `id` (`vida` / `minecraft` / `java`) шэдоуит синтетику — конфликта нет.

#### `:loader` — платформенные морфы и `VanillaBridge`

- Пакет `dev.vida.platform`:
  - `PlatformBridge` — интерфейс моста Vida↔vanilla с методами `onClientTick()` и `onHudRender(Object guiGraphics, float partialTick)`; утилита `pintorOver(Object)` строит `PintorHud` через reflection.
  - `VanillaBridge` — дефолтная реализация: монотонный счётчик client-тиков (`AtomicLong`), публикация `LatidoPulso` / `LatidoRenderHud` на `LatidoGlobal`-шину; размеры экрана читаются через `Minecraft.getInstance().getWindow()` reflection'ом.
  - `MinecraftTickMorph` — `@VifadaMorph(target="net.minecraft.client.Minecraft")`, `@VifadaInject method="tick()V"`, `requireTarget=false`.
  - `GuiRenderMorph` — `@VifadaMorph(target="net.minecraft.client.gui.Gui")`, `@VifadaInject method="render(Lnet/minecraft/client/gui/GuiGraphics;F)V"`, `requireTarget=false`.
- `PlatformMorphs` (`dev.vida.loader.internal`) — читает байты платформенных морфов из classpath и регистрирует их в `MorphIndex.Builder` независимо от наличия модов.
- `BootSequence` автоматически:
  - добавляет платформенные морфы в `MorphIndex` (см. `collectMorphs`);
  - устанавливает `VanillaBridge` после публикации `LatidoGlobal`, если мост ещё не установлен (тестовая подмена не перетирается).

#### `:loader` — compile-only стабы Minecraft

- Новый sourceSet `mcStubs` в `loader/build.gradle.kts`; папка `loader/mc-stubs/` содержит `net.minecraft.client.gui.GuiGraphics` — минимальную API-поверхность, которую использует `GuiRenderMorph`. Стабы явно исключаются из публикуемого jar и из `agentJar` (`exclude "net/minecraft/**"`).
- `loader/src/main/resources/META-INF/vida/loader-version.properties` — ресурс, стемпящийся Gradle'ом с `project.version`, чтобы `SyntheticProviders` знал версию платформы в рантайме без явной передачи через `BootOptions`.

### Tests

- `SyntheticProvidersTest` — канонизация версий, коллизии с реальными модами, источники `vida`/`minecraft`/`java`, `build()` с разными `existingIds`.
- `BootSequenceIntegrationTest` — мод `required.vida` успешно резолвится против синтетики, `required.minecraft` падает без `minecraftVersion` и проходит с ней, `VanillaBridge.onClientTick()` × N → ровно N `LatidoPulso`-событий, платформенные морфы попадают в индекс даже без модов.
- `VidaBootTest` — обновлены ожидания `totalMorphs()` с учётом двух платформенных морфов; добавлен сброс `VanillaBridge` между тестами.

### Documentation

- `docs/modules/loader.md` — новые разделы «Синтетические провайдеры платформы» и «Платформенные морфы»; обновлена таблица `BootOptions` (`vidaVersion`, `minecraftVersion`); обновлён список шагов `BootSequence` (теперь 10 шагов, включая установку моста и вызов entrypoint'ов).
- `docs/session-roadmap.md` — Session 6 помечена как выполненная.

### Notes

- Сигнатура `Gui.render` совпадает с Minecraft 1.20.5+ (Mojang-mapped). Для других версий морф молча пропускается благодаря `requireTarget=false`; пользовательский мод при желании может добавить свой морф с нужной сигнатурой и переопределить поведение — `VanillaBridge.install(...)` допускает подмену.
- `LatidoRenderHud` теперь доставляется любому подписчику без участия `Saciedad`/`Senda`/прочих модов. Поэтому `mods/saciedad/.../InGameHudDispatchMorph.java` остаётся в репозитории как референс, но функционально дублирован платформенным морфом и может быть удалён в одном из следующих релизов.

---

## [0.7.0] — 2026-04-22

Первые публикуемые моды: `Saciedad` (шкала насыщения) и `Senda` (навигационные точки).
Расширен `vida-render` новым HUD-событием.

### Added

#### `vida-render` — HUD render API

- `PintorHud` — абстракция рисования прямоугольников на HUD; позволяет тестировать HUD-рендереры без OpenGL.
- `LatidoRenderHud` — событие одного кадра HUD (`anchoPantalla`, `altoPantalla`, `deltaTick`, `PintorHud`); испускается с `Fase.DESPUES` для наложения поверх ванильного HUD.

#### Мод `Saciedad` (`mods/saciedad`)

- `SaciedadMod` — entrypoint; подписывается на `LatidoPulso` (`@OyenteDeTick(tps=20)`) и `LatidoRenderHud`.
- `SaciedadConfig` — типизированная конфигурация: `color` (hex ARGB), `mostrarSiempre`, `posicion` (`arriba`/`abajo`/`encima`).
- `SaciedadHudRenderizador` — два подписчика на `LatidoRenderHud` с `Fase.DESPUES`: фон + цветная шкала; 0 аллокаций на кадр после прогрева.
- `SaciedadCache` — безаллокационный кэш значения насыщения (`volatile float`).
- `FoodDataSaciedadMorph` — `@VifadaMorph(target = "net.minecraft.world.food.FoodData")`, читает `FoodData#saturation` через `@VifadaShadow`, обновляет `SaciedadCache` при каждом `FoodData#tick`.
- `saciedad.ptr` — Puertas access-widener: `mutable field net/minecraft/world/food/FoodData saturation F`.
- `saciedad.toml` — дефолтный файл конфигурации.
- `vida.mod.json` — манифест мода; `vifada` перечисляет `FoodDataSaciedadMorph`.
- Тесты: `SaciedadConfigTest` (парсинг hex-цвета, Posicion, Ajustes), `SaciedadHudRenderizadorTest` (headless GL-mock, пропорции шкалы, позиции).
- Производительность: < 0.1% CPU, 0 аллокаций на кадр.

#### Мод `Senda` (`mods/senda`)

- `PuntoRuta` — record путевой точки (`nombre`, `dimension`, `x`, `y`, `z`); методы `distanciaHorizontal`, `distancia3d`.
- `SendaCatalogo` — реестр точек поверх `CatalogoManejador`; активные точки в `ConcurrentHashMap` (CRUD), официальный `CatalogoMutable` (append-only, полный журнал).
- `SendaLatidos` — события `PuntoAgregado`, `PuntoEliminado`, `DimensionLimpiada`.
- `SendaConfig` — конфигурация: `maxPuntosPorDimension` (1–1000, по умолчанию 50), `dimensionInicial`.
- `SendaMod` — entrypoint; связывает Catalogo + Ajustes + Latidos.
- `senda.toml` — дефолтный файл конфигурации.
- Тесты: `SendaConfigTest` (парсинг, валидация диапазонов, invalid dimension), `SendaCatalogoTest` (CRUD, события, лимиты, `tamanioRegistro` vs `cantidad`).

### Changed

- `render/build.gradle.kts` — добавлена `api`-зависимость на `:base` (нужна `LatidoRenderHud` → `Latido`).
- `settings.gradle.kts` — добавлены `:mods:saciedad` и `:mods:senda`.

### Notes

- Никаких `TODO`/`FIXME` в коде новых модулей; проверено `rg -n 'TODO|FIXME|XXX' mods/`.
- Все публичные типы модов аннотированы `@ApiStatus.Preview("saciedad")` / `@ApiStatus.Preview("senda")`.

---

## [0.6.0] — 2026-04-22

Рендеринг, сеть и data-driven prototype: два новых API-модуля, preview-расширения Vifada и чтение контента из `vida.mod.json` + datapack без Java-entrypoint.

### Added

#### `vida-render` — render pipeline API

- `ModeloBloque` и `ModeloEntidad` для декларативного описания моделей блоков/сущностей.
- `TextureAtlas` с явным `missing-texture` и resolver-контрактом.
- `RenderPipeline` с регистрацией моделей и shader hooks.
- `ShaderHook` + `ContextoShader` для этапов `ANTES_MUNDO` / `DESPUES_MUNDO` / `HUD`.
- Безопасный fallback по умолчанию: блок без модели рендерится как `cube + missing-texture`, сущность — `entity/simple + missing-texture`.

#### `vida-red` — Tejido network API

- Маркеры направления: `PaqueteCliente`, `PaqueteServidor`, `DireccionPaquete`.
- `TramaPaquete` wire-level frame (`tipo`, `direccion`, `versionCodec`, `payload`).
- `CodecPaquete<T>` — контракт сериализации пакета.
- `CodificadorRegistros` — авто-кодек для Java `record` (primitives + `String` + `Identifier` + `enum`).
- `TejidoCanal` — версиярованные codecs, queue-based отправка, fallback decode на ближайшую меньшую версию (`floorEntry`).
- `TejidoError` — типизированные ошибки (`TipoNoRegistrado`, `VersionNoSoportada`, `BackPressure`, `PayloadInvalido`).

#### Loader — data-driven mods prototype

- Новый пакет `dev.vida.loader.fuente`:
  - `FuentePrototipoParser`
  - `FuenteContenidoMod`
  - `FuenteBloque`, `FuenteObjeto`, `FuenteRecetaShaped`
  - `FuenteError`
- Prototype-чтение `custom["vida:dataDriven"]` из `vida.mod.json`.
- Разбор datapack JSON (`bloques/*.json`, `objetos/*.json`, `recipes/*.json`, только `type = "vida:shaped"`).
- Интеграция в bootstrap: parsed snapshot доступен как `VidaEnvironment.fuenteDataDriven()`.
- `LoaderError.DataDrivenFailure` для ошибок parser/IO в prototype-контуре.

#### Vifada — preview extensions

- Новые аннотации API:
  - `@VifadaMulti`
  - `@VifadaLocal`
- Обе помечены `@ApiStatus.Preview("vifada-next")` как публичный контракт следующего этапа развития Vifada.

#### Документация

- Новые страницы: `docs/modules/render.md`, `docs/modules/red.md`.
- Обновлены: `docs/modules/index.md`, `docs/modules/vifada.md`, `docs/index.md`, `docs/architecture/overview.md`, `docs/reference/api-stability.md`, `docs/reference/manifest-schema.md`, `docs/roadmap.md`, `docs/session-roadmap.md`.

### Changed

- `settings.gradle.kts` — добавлены `include(":render")` и `include(":red")`.
- `loader/internal/BootSequence` теперь собирает data-driven snapshot для каждого разрешённого мода.
- `VidaEnvironment` расширен accessor'ом `fuenteDataDriven()`.

### Notes

- Data-driven формат в 0.6.x — прототип (`preview`), без гарантий стабильности до 1.0.0.
- Runtime-обработка `@VifadaMulti` / `@VifadaLocal` будет добавляться итеративно; в 0.6.0 это compile-time API.

---

## [0.5.0] — 2026-04-22

Мир и сущности: два новых публичных API-модуля, world-латидосы и shortcut для
тиковых подписчиков.

### Added

#### `vida-entidad` — публичный API сущностей

- `Entidad` — декларативный descriptor entity-type (`Identifier`, `TipoEntidad`, `PropiedadesEntidad`).
- `TipoEntidad` — типовые категории (`CRIATURA`, `MONSTRUO`, `AMBIENTAL`, `ACUATICA`, `UTILIDAD`, `PROYECTIL`, `JEFE`, `MISCELANEA`) с предикатами `esHostil()` / `esViva()`.
- `PropiedadesEntidad` — масса, `Hitbox`, `GrupoIa`, entity data-components; fluent-builder с валидацией.
- `ComponenteEntidad`, `ClaveComponenteEntidad`, `MapaComponentesEntidad` — типизированная система entity data-components (`Salud`, `VelocidadMovimiento`, `NombreVisible`, `Brillo`, `InmuneFuego`, `TablaBotin`).
- `RegistroEntidades` — типизированная регистрация сущностей поверх `CatalogoManejador`.

#### `vida-mundo` — публичный API мира

- `Mundo` — минимальный runtime-контракт мира: `id`, `dimension`, `biomaEn`, `estaCargado`, `tiempoDelDia`.
- `Coordenada` — immutable world-position с helper'ами `desplazar`, `chunkX`, `chunkZ`, `distanciaCuadrada`.
- `Dimension` — value-type измерения с built-in `OVERWORLD`, `NETHER`, `END`.
- `Bioma` — value-type биома (`temperatura`, `humedad`, `precipitacion`) с helper'ами `esFrio()` и `tienePrecipitacion()`.
- `LatidosMundo` — world-события `MundoCargado`, `ChunkCargado`, `Tick`, `NocheAmanece`.

#### `base` — `@OyenteDeTick`

- `@OyenteDeTick(tps=...)` — shortcut для `LatidoPulso` с частотным throttling'ом по корневым тикам.
- Поддерживает те же executor-параметры, что и `@EjecutorLatido`: `kind`, `etiqueta`, `prioridad`, `prioridadBus`, `fase`.
- `LatidoRegistrador` теперь понимает `@OyenteDeTick` и валидирует конфликты аннотаций (`AnotacionesConflictivas`) и диапазон `tps` (`TpsInvalido`).

#### Документация

- `docs/modules/entidad.md` — обзор entity API.
- `docs/modules/mundo.md` — обзор world API и `LatidosMundo`.
- `docs/guides/first-entity.md` — минимальный walkthrough по первой сущности.
- Обновлены `docs/index.md`, `docs/modules/index.md`, `docs/guides/index.md`, `docs/modules/base-ejecutor.md`, `docs/reference/api-stability.md`, `docs/architecture/overview.md`, `docs/roadmap.md`.

### Changed

- `settings.gradle.kts` — добавлены `include(":entidad")` и `include(":mundo")`.
- Граф публичных preview-модулей расширен модулями `entidad` и `mundo`.

### Notes

- Все новые публичные типы помечены `@ApiStatus.Preview("entidad"|"mundo"|"base")`.
- Для `@OyenteDeTick` subtick-события намеренно не доставляются; если нужен каждый `LatidoPulso`, используйте `@EjecutorLatido`.

---

## [0.4.0] — 2026-04-22

DX и наблюдаемость: профайлер, авто-регистрация подписчиков, поддержка
Modrinth App и CurseForge App в инсталляторе.

### Added

#### `vida-vigia` — sampling-профайлер поверх JFR

- `VigiaSesion` — start/stop/snapshot; запись в `.jfr` + in-memory `Resumen`.
- `Resumen` — агрегированный снимок: duration, samples, top-20 методов (`MetodoMuestra`), метрики Latido (`LatidoMetrica`), статистика Susurro.
- `VigiaReporte` — self-contained HTML-отчёт с flame-chart (CSS bars), top-N таблицей, Susurro-breakdown и XSS-escaping.
- `VigiaComando` — контракт команды `/vida profile start|stop|dump` для loader'а.
- Интеграция с `Susurro.Estadisticas` — при привязке пула через `sesion.conSusurro(s)` в Resumen попадают activos/pendientes/completadas.

#### `base` — Reflection-биндер для `@EjecutorLatido`

- **`LatidoRegistrador.registrarEnObjeto(bus, instance, susurro, hp)`** — сканирует методы, помеченные `@EjecutorLatido`, создаёт правильный `Ejecutor` (SINCRONO / SUSURRO / HILO_PRINCIPAL), подписывает с корректной `Prioridad`/`Fase`. Одна строка вместо ручных `suscribir(...)`.
- Валидация сигнатуры: ровно один параметр, присваиваемый типу события из `static final Latido<E>` поля класса события (конвенция `TIPO`) или класса-владельца.
- `LatidoRegistradorError` — sealed-иерархия ошибок: `FirmaInvalida`, `LatidoNoEncontrado`, `LatidoAmbiguo`, `EjecutorFaltante`, `TipoIncompatible`.
- `@EjecutorLatido` расширен: новые поля `prioridadBus` (`PrioridadBus`) и `fase` (`FaseBus`) для управления приоритетом/фазой подписки без ручного кода.

#### Installer — Modrinth App handler

- `ModrinthHandler` — полная поддержка Modrinth App (режим `PATCH_EXISTING_INSTANCE`).
- `ModrinthDbReader` — чтение профилей из SQLite `app.db` (JDBC); обновление `java_args` с `-javaagent`.
- Зависимость: `org.xerial:sqlite-jdbc` добавлена в installer fat-jar.

#### Installer — CurseForge App handler

- `CurseForgeHandler` — полная поддержка CurseForge App (режим `PATCH_EXISTING_INSTANCE`).
- `CurseForgeJsonPatcher` — патчинг `javaArgsOverride` в `minecraftinstance.json`: добавление, замена существующего `-javaagent`, сохранение прежних аргументов.
- `CurseForgeInstanceScanner` — сканирование `Instances/` для `--list-instances`.

#### Документация

- `docs/modules/vigia.md` — полная документация модуля Vigia.
- `docs/modules/base-ejecutor.md` — Ejecutor, @EjecutorLatido, LatidoRegistrador.
- Обновлён `docs/modules/installer.md` — добавлены секции Modrinth и CurseForge.
- Обновлён `CHANGELOG.md` — этот файл.

### Changed

- `LauncherKind.MODRINTH` и `LauncherKind.CURSEFORGE` — `implemented` = `true`.
- `LauncherRegistry` — зарегистрированы `ModrinthHandler` и `CurseForgeHandler`.
- `installer/build.gradle.kts` — добавлена зависимость `sqlite-jdbc`.
- `gradle/libs.versions.toml` — добавлена версия `sqlite-jdbc = "3.47.1.0"`.
- `settings.gradle.kts` — `include(":vigia")`.

### Notes

- Никаких placeholder'ов или `TODO`/`FIXME` в коде новых модулей.
- Все новые публичные классы аннотированы `@ApiStatus.Preview`.

---

## [0.3.0] — 2026-04-22

Крупный релиз: четыре новых публичных API-модуля, потокобезопасная шина
событий, access-wideners и улучшения тулчейна. Совместимость с `base` 0.1.x
сохранена — ни одного API-метода не удалили и не переименовали.

### Added

#### `vida-bloque` — публичный API для блоков

- `Bloque` + `PropiedadesBloque` (fluent `Constructor` с валидацией и дефолтами по материалу).
- `MaterialBloque` (solido / inflamable / liquido / traversable), `SonidoBloque` с пресетами.
- `FormaColision` как объединение `Caja`-AABB, методы `union`, `contiene`, `exterior`, `aabb`.
- `NivelHerramienta` (wooden → netherite с `rango` и `satisfechoPor`), `TipoHerramienta`.
- `BloqueEntidad` + `ContextoBloqueEntidad` с `serializar` / `deserializar`.
- `RegistroBloques` поверх `CatalogoManejador` — `registrar`, `etiquetar`, `miembros`, `obtener`.
- `EtiquetaBloque` — типизированные block-tag'и.

#### `vida-objeto` — публичный API для предметов

- `Objeto` + `PropiedadesObjeto` (fluent builder, `maxPila=1` enforce для инструментов).
- `Material` (стандартные пресеты: дерево/камень/железо/алмаз/незерит/золото) и `Herramienta`.
- `Raridad` с RGB-цветами, `TipoObjeto`.
- Система **data-components 1.21.1**: sealed `Componente` с записями `DatosModeloPersonalizados`, `Irrompible`, `Durabilidad`, `Comida`, `AtributosModificados`, …; типизированный `ClaveComponente`; иммутабельная `MapaComponentes` с `con`, `obtener`, `fusionar`.
- `ObjetoDeBloque` для сопоставления `Bloque` ↔ `Objeto`.
- `RegistroObjetos`, `EtiquetaObjeto`.

#### `vida-susurro` — managed thread-pool

- `Susurro.Politica(workers, maxCola, maxPorEtiqueta)` + дефолтная политика (`workers = max(2, cpu/2)`, `maxCola = 1024`).
- `Prioridad` (`ALTA`/`NORMAL`/`BAJA`) + приоритетная очередь; FIFO внутри приоритета.
- `Etiqueta` для back-pressure по лейблу — лимит одновременных задач per-tag.
- `Tarea<T>`: обёртка `CompletableFuture` со своим `Estado` (`PENDIENTE` → `INICIADA` → `COMPLETADA`/`FALLADA`/`CANCELADA`); `conPlazo(Duration)` (трактует `TimeoutException` как cancellation); `enHiloPrincipal(HiloPrincipal, Consumer)` для маршалинга результата.
- `HiloPrincipal` — FIFO-очередь `Runnable`-ов для исполнения на игровом тике через `pulso()`.
- `Susurro.Estadisticas(activos, pendientes, completadas, rechazadas, workers)`.
- `AutoCloseable` — аккуратный `shutdown` с 5-секундным `awaitTermination`.

#### `vida-puertas` — access-wideners

- Формат `.ptr` с заголовком `vida-puertas <версия> namespace=<crudo|intermedio|exterior>`; комментарии `#`.
- Директивы `accesible|extensible|mutable class|method|field ...` с полной JVM-descriptor-валидацией.
- `PuertaParser.parsear(Path|String|Reader)` → `ParseResult` с диагностикой (`CabeceraInvalida`, `DirectivaTruncada`, `DirectivaInvalida`, `DescriptorMalformado`, `NamespaceDesconocido`, `VersionNoSoportada`, `MutableNoAplicable`).
- `PuertaArchivo` — иммутабельное представление, индексация `paraClase(...)`, `combinar(...)` для нескольких файлов с проверкой совпадения `Namespace`.
- `AplicadorPuertas.aplicar(byte[], List<PuertaDirectiva>)` — ASM-трансформер, меняет флаги доступа (`ACC_PUBLIC`/`ACC_PROTECTED`/`ACC_PRIVATE`/`ACC_FINAL`) на ClassNode/FieldNode/MethodNode; возвращает `Informe(aplicadas, perdidas)`.
- Семантика: `accesible` повышает видимость, `extensible` снимает `final` у класса/метода, `mutable` снимает `final` у поля (не применим к class/method).

#### `base` — Latidos profundos

- **`Ejecutor`** — SAM-интерфейс стратегии исполнения для обработчиков событий.
  - Статическая константа `SINCRONO` — исполнение inline в потоке `emitir`.
  - Фабрики: `hiloPrincipal(HiloPrincipal)`, `susurro(Susurro, Prioridad, Etiqueta)`, `serializado(String)` (однопоточный FIFO).
- **Новая перегрузка** `LatidoBus.suscribir(Latido, Prioridad, Fase, Ejecutor, Oyente)`. Все предыдущие `suscribir(...)`-вызовы сохранены и трактуются как `Ejecutor.SINCRONO` — **обратная совместимость API 0.1.x не нарушена**.
- `DefaultLatidoBus` использует `Ejecutor.ejecutar(Runnable)` вне critical-path; ошибки подписчиков ловятся и логируются, как и раньше.
- **`@EjecutorLatido(kind, etiqueta, prioridad)`** — аннотация-маркер метода для будущего reflection-биндера (0.4.x).
- Асинхронные подписчики на отменяемые события логируются с предупреждением (их решения об отмене игнорируются — гарантия согласованности потока эмиссии).

#### Тулчейн

- **Gradle-plugin** (`dev.vida.mod`):
  - Задача `vidaValidatePuertas` — парсит и валидирует все `.ptr` из DSL `mod { puertas = [...] }`. При ошибке собирает все проблемы в одно сообщение с указанием файла/строки.
  - Задача `vidaPackagePuertas` — после валидации копирует `.ptr` в `build/generated/vida/puertas/`, сохраняя иерархию. Папка автоматически подключается как resource srcDir для `main` sourceSet.
  - Авто-подключение: если в манифесте есть хоть один `puertas`-путь, `processResources` начинает зависеть от `vidaPackagePuertas`, а `Jar` — от `vidaValidatePuertas`.
- **Installer**: флаг `--validate-puertas <file|dir>` (новое `Action.VALIDATE_PUERTAS`); рекурсивный обход директорий, exit-code `0` при успехе, `1` при user-error, `2` при ошибках парсинга.

#### Документация

- `docs/roadmap.md` — блок «Done (0.3.0)» и пересобранный план 0.4.x — 1.0.
- `docs/session-roadmap.md` — подробный план оставшихся сессий до 1.0 и трёх мод-проектов.
- `CHANGELOG.md` — этот файл.

### Changed

- `base/build.gradle.kts` — новая `api`-зависимость на `:susurro` (нужна для `Ejecutor.susurro(...)` / `Ejecutor.hiloPrincipal(...)`). Старый код модов продолжает компилироваться без изменений — классы `Susurro`/`HiloPrincipal` доступны на classpath, но их использование не требуется, пока `Ejecutor.SINCRONO` остаётся дефолтом.
- `gradle-plugin/build.gradle.kts` и `installer/build.gradle.kts` получили `implementation(project(":puertas"))`.

### Notes

- Никаких placeholder'ов или `TODO`/`FIXME` в коде новых модулей нет; новый код прошёл локальную проверку `rg -n 'TODO|FIXME|XXX' bloque objeto susurro puertas` → пусто.
- Все новые публичные классы аннотированы `@ApiStatus.Preview("<module>")` — см. [api-stability.md](docs/reference/api-stability.md).

---

## [0.1.0] — initial public snapshot

Все базовые модули (`core`, `manifest`, `config`, `cartografia`, `discovery`, `resolver`, `vifada`, `loader`, `base`, `gradle-plugin`, `installer`) и multi-launcher installer (Mojang / Prism / MultiMC / ATLauncher). Подробности — в [docs/index.md](docs/index.md) и [README.md](README.md).
