# Session Roadmap: 0.3.0 → 1.0.0 + Mod Projects

> Документ фиксирует, что именно делается в каждой из оставшихся AI-ассистированных сессий работы над проектом Vida.
> Каждый пункт — production-quality, без заглушек, без `TODO`/`FIXME`.
> Связано с общим планом: см. `[docs/roadmap.md](./roadmap.md)` и `[CHANGELOG.md](../CHANGELOG.md)`.

## Соглашения

Каждая сессия включает:

- **Код** — новые публичные API и внутренние реализации.
- **Тесты** — unit + интеграционные, AssertJ + JUnit 5, зелёные на Windows/Linux.
- **Документация** — страницы в `docs/`, кросс-ссылки, примеры.
- **Verification** — `./gradlew build check javadocAll` + `rg -n 'TODO|FIXME|XXX' <новые пути>` = 0.

«Session N» — условная единица ≈ 1 рабочая AI-сессия с достаточным контекстом и временем на green-билд.
Реальное вмещение зависит от сложности; если что-то не влезает, следующая сессия начинает с «переходящего» пункта.

---

## Session 1 — 0.3.0 [DONE]

См. `[CHANGELOG.md](../CHANGELOG.md#030--2026-04-22)`. Итоги:

- `vida-bloque`, `vida-objeto`, `vida-susurro`, `vida-puertas`.
- Latidos profundos (`Ejecutor` + `@EjecutorLatido`) в `base`.
- Gradle-plugin: `vidaValidatePuertas`, `vidaPackagePuertas`.
- Installer: `--validate-puertas`.
- Документация 0.3.0: раздел Done в `roadmap.md`, этот файл, `CHANGELOG.md`.

---

## Session 2 — 0.4.0 «DX и наблюдаемость» [DONE]

См. `[CHANGELOG.md](../CHANGELOG.md#040--2026-04-22)`. Итоги:

- `vida-vigia`: `VigiaSesion`, `Resumen`, `VigiaReporte` (HTML), `VigiaComando`.
- Reflection-биндер: `LatidoRegistrador`, `LatidoRegistradorError` (sealed), расширение `@EjecutorLatido`.
- Modrinth App handler: `ModrinthHandler`, `ModrinthDbReader` (SQLite JDBC).
- CurseForge handler: `CurseForgeHandler`, `CurseForgeJsonPatcher`, `CurseForgeInstanceScanner`.
- Документация: `vigia.md`, `base-ejecutor.md`, расширение `installer.md`.
- CHANGELOG 0.4.0.

Оригинальный план

- `**vida-vigia` (новый модуль).** Лёгкий samplig-профайлер поверх JFR.
  - `VigiaSesion` — start/stop/snapshot; запись в `.jfr` + in-memory `Resumen`.
  - Команда `/vida profile start|stop|dump`.
  - HTML-отчёт (`vigia-report-<ts>.html`) с flame-chart, top-20 методов, breakdown по `Latido`/`Catalogo`/`Susurro`.
  - Интеграция с `Susurro.Estadisticas` и `DefaultLatidoBus` (метрики на канал).
  - Тесты: контракты snapshot-формата, smoke-тесты HTML-renderer'а.
- **Reflection-биндер для `@EjecutorLatido`.**
  - `LatidoRegistrador.registrarEnObjeto(bus, instance)` — сканирует методы, помеченные `@EjecutorLatido`, создаёт правильный `Ejecutor` (SINCRONO / SUSURRO / HILO_PRINCIPAL), подписывает с корректной `Prioridad`/`Fase`.
  - Валидация сигнатуры: ровно один параметр, присваиваемый типу события из `@Latido`-поля владеющего класса.
  - Ошибки — типизированные `LatidoRegistradorError`.
  - Интеграционный тест поднимает in-memory `Susurro` + `HiloPrincipal`, проверяет, что методы вызываются в правильных потоках.
- **Modrinth App handler.** Чтение `app.db` (SQLite JDBC) + модификация `profiles.json`; `InstanceRef` отдаётся installer'у.
- **CurseForge handler.** `minecraftinstance.json` с `javaArgsOverride`; обработка `modpack.manifest`.
- **Обновление `docs/modules/`**: страницы `vigia.md`, `base-ejecutor.md`, расширение `installer.md`.
- **CHANGELOG 0.4.0**: два новых модуля, два новых launcher'а, reflection-биндер, docs.

Критерии выхода: ≥95% покрытие новых модулей Jacoco, нулевые линты, все installer-ITs зелёные.

---

## Session 3 — 0.5.0 «Мир и сущности» [DONE]

См. `[CHANGELOG.md](../CHANGELOG.md#050--2026-04-22)`. Итоги:

- `vida-entidad`: `Entidad`, `TipoEntidad`, `PropiedadesEntidad`, `RegistroEntidades`; entity data-components: `ComponenteEntidad`, `ClaveComponenteEntidad`, `MapaComponentesEntidad`.
- `vida-mundo`: `Mundo`, `Coordenada`, `Dimension`, `Bioma`, `LatidosMundo` (`MundoCargado`, `ChunkCargado`, `Tick`, `NocheAmanece`).
- `base`: `@OyenteDeTick(tps=...)` как shortcut над `LatidoPulso`; `LatidoRegistrador` валидирует конфликт с `@EjecutorLatido` и диапазон `tps`.
- Документация: `docs/modules/entidad.md`, `docs/modules/mundo.md`, `docs/guides/first-entity.md`, обновления индексов и reference-страниц.

Оригинальный план

Цель: дать модам API для сущностей и мировых событий так же безопасно и типобезопасно, как уже есть для блоков.

- `**vida-entidad`.** `Entidad`, `PropiedadesEntidad` (масса, хитбокс, AI-группы), `TipoEntidad`, `RegistroEntidades`. Data-components для entities (1.21.1 уже переводит на data-components и для entities).
- `**vida-mundo`.** `Mundo` интерфейс, `Coordenada` record, `Dimension`, `Bioma`, `LatidosMundo` (`MundoCargado`, `ChunkCargado`, `Tick`, `NocheAmanece`).
- **Tick-аннотации** — `@OyenteDeTick(tps=20)` как shortcut над `@EjecutorLatido`.
- **Docs**: `docs/modules/entidad.md`, `docs/modules/mundo.md`, туториал `docs/guides/first-entity.md`.

---

## Session 4 — 0.6.0 «Рендер, сеть, дата» [DONE]

См. `[CHANGELOG.md](../CHANGELOG.md#060--2026-04-22)`. Итоги:

- `vida-render`: `ModeloBloque`, `ModeloEntidad`, `TextureAtlas`, `RenderPipeline`, `ShaderHook`, `ContextoShader`; safe fallback (`cube + missing-texture` / `entity/simple + missing-texture`).
- `vida-red`: `PaqueteCliente`, `PaqueteServidor`, `TramaPaquete`, `TejidoCanal`, `TejidoError`, `CodificadorRegistros`; авто-сериализация record'ов, versioned codecs и back-pressure.
- Data-driven прототип в `loader`: `FuentePrototipoParser` + модели `FuenteBloque`/`FuenteObjeto`/`FuenteRecetaShaped`; чтение `custom["vida:dataDriven"]` из `vida.mod.json` и datapack JSON.
- Интеграция bootstrap: parsed data-driven snapshot доступен в `VidaEnvironment.fuenteDataDriven()`.
- Vifada preview-расширения: `@VifadaMulti`, `@VifadaLocal` под `@ApiStatus.Preview("vifada-next")`.
- Документация: новые страницы `docs/modules/render.md`, `docs/modules/red.md`, обновления индексов, `vifada.md`, `manifest-schema.md`, `roadmap.md`, этот файл.

Оригинальный план

- `**vida-render`.** Pipeline-абстракция для custom-блоков и entities: `ModeloBloque`, `ModeloEntidad`, TextureAtlas, Shader-hooks; безопасно по-умолчанию (если модель не указана — cube + missing-texture).
- `**vida-red`.** `PaqueteCliente` / `PaqueteServidor` — авто-сериализация record'ов, версионирование кодеков, back-pressure.
- **Data-driven моды (прототип).** Чтение блока/предмета/рецепта из `vida.mod.json` + датапака без Java. MVP-объём: простые блоки, простые предметы, shaped-recipes.
- **Vifada расширения (превью)** — `@VifadaMulti`, `@VifadaLocal` доступны под `@ApiStatus.Preview("vifada-next")`.

---

## Session 5 — 0.7.0 «Первые моды (лёгкие два)» [DONE]

См. `[CHANGELOG.md](../CHANGELOG.md#070--2026-04-22)`. Итоги:

- `vida-render`: `PintorHud` (GL-абстракция) + `LatidoRenderHud` (HUD-событие).
- `mods/saciedad`: `SaciedadMod`, `SaciedadConfig`, `SaciedadHudRenderizador`, `SaciedadCache`, `FoodDataSaciedadMorph`, `saciedad.ptr`, тесты, README, CHANGELOG.
- `mods/senda`: `SendaMod`, `SendaCatalogo`, `SendaConfig`, `SendaLatidos`, `PuntoRuta`, тесты, README, CHANGELOG.
- Оба мода в `settings.gradle.kts` как `:mods:saciedad` / `:mods:senda`.

Оригинальный план

Параллельно с основным кодом Vida — первые публикуемые моды, каждый в отдельном `mods/<name>` с собственным `build.gradle.kts`, независимым release-процессом.

### Мод 1 — `Saciedad` (Saturation-bar)

Самый простой. Показывает скрытую шкалу насыщения поверх шкалы голода.

- Один entrypoint, один `@OyenteDeTick(tps=20)` обновляет кэш значения насыщения через `Vifada.@Shadow` на `FoodData#saturation` (access-widener `.ptr` + инжектор).
- HUD-рендер: два подписчика на `LatidoRenderHud`, порядок `Fase.DESPUES`, прозрачный overlay, цвет/шкалу настраивает `Ajustes`.
- Конфиг: `saciedad.toml` — `color`, `mostrarSiempre`, `posicion` (`arriba`/`abajo`/`encima`).
- Тесты: чистая unit-логика парсинга `Ajustes` + headless-рендер-тест (фейковый `GL`-mock).
- Документация: `mods/saciedad/README.md`, скриншоты, CHANGELOG.
- Производительность: < 0.1% CPU, 0 аллокаций на кадр после прогрева.

### Мод 2 — `Senda` (удобство навигации, proof-of-concept)

Не входит в изначальный запрос пользователя, но маленький и демонстрирует `Catalogo` + `Ajustes` + `Latidos`.

---

## Session 6 — 0.8.0 «Платформа» (инфраструктура MC-моста) [DONE]

См. `[CHANGELOG.md](../CHANGELOG.md#080--2026-04-22)`. Итоги:

- `dev.vida.loader.internal.SyntheticProviders` — инъекция провайдеров `vida`/`minecraft`/`java` в `Universe` перед `Resolver.resolve()`; канонизация версий (`"21"` → `21.0.0`).
- `BootOptions.vidaVersion(...)` и `minecraftVersion(...)` + форвард из `VidaPremain` (`-Dvida.version`, `-Dvida.minecraftVersion`, `vidaVersion=…`, `minecraftVersion=…`).
- Ресурс `META-INF/vida/loader-version.properties` (стемпится `processResources` через `expand`) — источник версии `vida` по умолчанию.
- Новый пакет `dev.vida.platform`: `PlatformBridge`, `VanillaBridge`, `MinecraftTickMorph`, `GuiRenderMorph`. `BootSequence` сам регистрирует морфы в `MorphIndex` через `PlatformMorphs` и устанавливает `VanillaBridge` после публикации `LatidoGlobal`.
- `loader/mc-stubs/` — compile-only sourceSet со стабом `net.minecraft.client.gui.GuiGraphics`; исключается из `agentJar` через `exclude "net/minecraft/**"`.
- Тесты: `SyntheticProvidersTest`, `BootSequenceIntegrationTest` (четыре сценария, включая N тиков → N `LatidoPulso`).
- Документация: разделы «Синтетические провайдеры платформы» и «Платформенные морфы» в `docs/modules/loader.md`.

Оригинальный план

Прежде чем делать тяжёлые рендер-моды, закрываем три критических пробела в платформе:

### 0. Синтетические провайдеры зависимостей

`Resolver` сейчас не знает о «платформенных» провайдерах (`vida`, `minecraft`, `java`), из-за чего любой мод с зависимостью `"vida": ">=0.7"` в `vida.mod.json` получает `ResolverError.Missing` при бутстрапе.

**Задача:** добавить в `BootSequence.resolveDependencies()` инъекцию синтетических провайдеров перед вызовом `Resolver.resolve()`:

- `vida` — версия текущего загрузчика из `BootOptions` / `version.txt`;
- `minecraft` — версия игры (читается из `version.json` в game-dir или через `brand()`);
- `java` — `System.getProperty("java.specification.version")`.

После этого моды смогут декларировать `"dependencies": { "required": { "vida": ">=0.7" } }` без ошибок резолвера.

### 1. Entrypoint invocation

**Задача (уже реализовано в исправлении к Session 5):** `BootSequence` вызывает `VidaMod.iniciar(ModContext)` для каждого резолвнутого мода. Инфраструктура (`DefaultModContext`, `LatidoBus`, `CatalogoManejador`) создаётся в загрузчике и инжектится через `ModContext`.

### 2. MC-мост: платформенные события

**Задача:** Vifada-морфы в **ядре** загрузчика (не в пользовательских модах) инжектятся в ключевые MC-методы и публикуют системные события на глобальную `LatidoBus`:

- `LatidoPulso` — каждый game-tick (`Minecraft.tick()` / `ClientTickEvents`);
- `LatidoRenderHud` — каждый render-frame HUD (`Gui.render(GuiGraphics, float)`).

После этого любой мод, подписавшийся на `LatidoRenderHud`, начинает получать события без собственных MC-морфов.

### Инфраструктура

- `core/src/main/java/dev/vida/platform/` — новый пакет с `PlatformBridge` (интерфейс) и `VanillaBridge` (реализация через Vifada).
- Морфы-системы: `MinecraftTickMorph`, `GuiRenderMorph` — включены в agent-fat-jar.
- Тесты: `BootSequenceIntegrationTest` — поднимает фейковый `Instrumentation`, проверяет, что `LatidoPulso` приходит ровно N раз за N тиков.

---

## Session 7 — 0.9.0 «Valenta» (Sodium-class мод) [DONE]

См. `[CHANGELOG.md](../CHANGELOG.md#090--2026-04-22)`. Итоги:

- `mods/valenta/` — полноценный аналог Sodium + Sodium Extra в одном моде.
- Render core: `CompactVertexFormat` (16 bytes), `VboMallaBatcher` с `glMultiDrawElementsIndirect`, `BiomeBlendSsbo`, `BlockLightSsbo`, `GpuBuffer`, `GlFunctions`.
- Chunk meshing: `ChunkTaskGraph` через `Susurro` (`Analisis → Build → Upload`), `Etiqueta.de("valenta/chunk")`, greedy face culling.
- Culling: `ValentaFrustum` (Gribb-Hartmann), `OcclusionQuery` (GL `SAMPLES_PASSED`), `PvsTree` (portal flood-fill), `CullingEngine` (three-tier gate).
- Sky: `SkyRenderer` с `glInvalidateFramebuffer`.
- QoL: `ParticleFilter`, `CloudRenderer`, `AnimatedTextureManager`, `RenderDistanceManager`, `GpuTimingPane`.
- 5 Vifada morphs (`escultor/`): `LevelRendererMorph`, `GameRendererMorph`, `ChunkRendererMorph`, `ParticleMorph`, `CloudMorph`.
- Debug: `/valenta debug occlusion|gpu|stats`, `OcclusionOverlay`.
- Тесты: 11 unit-тестов + jqwik property-based `MallaChunkPropertyTest`.
- JMH бенчмарки: `VertexFormatBenchmark`, `CullingBenchmark`, `MeshingBenchmark`.
- Документация: `README.md`, `CHANGELOG.md`, `docs/mods/valenta/architecture.md`, `docs/mods/valenta/compat-matrix.md`.

Оригинальный план

Полноценный аналог Sodium + Sodium Extra в **одном** моде `valenta`.

### Функциональность

- **Render-ядро.** VBO-батчер + `glMultiDrawIndirect`; compact vertex-format (16 байт вместо 28); отдельные SSBO для biome-blending и block-light.
- **Chunk meshing.** Task-graph через `Susurro`: `Analisis → Build → Upload`. Задачи помечены `Etiqueta.de("valenta/chunk")` для back-pressure.
- **Culling.** Occlusion queries + frustum; PVS-дерево; `/valenta debug occlusion` для визуализации.
- **Видимость неба.** `glInvalidateFramebuffer` вместо clear, когда небо перекрыто.
- **Qualities-of-life (Sodium Extra).**
  - Скрытие/замена частиц, туч, анимированных текстур.
  - Render distance < 8 без фриза воркеров.
  - F3 custom-пейн `/valenta debug` с таймингами GPU-пассов.
- **Совместимость.** Vifada-морфы в `net/minecraft/client/renderer/...` группируются в `valenta.escultor`; конфликты с другими render-модами сообщает `VidaClassTransformer`.

### Инфраструктура

- `mods/valenta/` — `build.gradle.kts` через `dev.vida.mod`.
- `.ptr`-файл на ~30 нужных vanilla-символов.
- Тесты: render-логика — unit (с offscreen-GL через LWJGL headless); целочисленные инварианты meshing'а — property-based (jqwik).
- Бенчмарк-сьют: `@Benchmark` JMH, сравнение vanilla vs Valenta на 10 воркспейсах.
- Документация: `mods/valenta/README.md` + `docs/mods/valenta/architecture.md`.

---

## Session 8 — 0.9.5 «Vida Shaders» + «Estorosso»

Аналог Iris Shaders. Вызов не в том, чтобы повторить байт-в-байт, а сделать корректную работающую реализацию `.shaders.zip` и написать простой shader-pack `Estorosso` (итал. «огненная тень»), добавляющий реалистичные тени.

### `Vida Shaders` (мод)

- Разбор `.shaders.zip` по конвенции OptiFine/Iris (`shaders/*.vsh|*.fsh|*.glsl`).
- `ShaderProgramaManejador` — компиляция, линковка, горячая перезагрузка при сохранении файла.
- Hook'и в vanilla render-pipeline через Vifada:
  - gbuffers (terrain/entities/water/sky/translucent).
  - composite passes.
  - final pass.
- Uniforms стандарта Iris: `gbufferModelView`, `sunPosition`, `viewHeight`, `rainStrength`, и т. д. Полный совместимый список — в `docs/mods/vida-shaders/uniforms.md`.
- `vshconfig.txt` — GUI редактор.
- Тесты: parser-contracts, compile-smoke против тестового шейдер-пака (`Estorosso` используется в CI).

### `Estorosso` (shader-pack)

- Простые, но корректные тени: shadow-map 2k с PCF 3×3; cascades — 2 уровня.
- Мягкая корона вокруг солнца и луны.
- Rain-wet-ness: увеличение specular на нормалях вверх при rainStrength > 0.
- Без атмосферного рассеяния, без volumetric-fog — чтобы оставаться «простым».
- В `mods/estorosso/shaders/`; пакуется в `.shaders.zip`.
- Документация: `mods/estorosso/README.md` + скриншоты.

---

## Session 9 — 1.0.0 «Стабилизация»

- Прогон всех `@Preview` → `@Stable` по зрелым модулям (`base`, `bloque`, `objeto`, `susurro`, `puertas`, `gradle-plugin`). Модули, не готовые к `@Stable`, остаются `@Preview`, но с чётким критерием выхода.
- `**vidaDocTest`** — кастомная Gradle-таска компилирует все  ``java примеры из `docs/` как self-contained `Test`-классы.
- **Security audit** — внешний ревьюер. Отчёт публикуется в `docs/security/audit-1.0.md`.
- **Full JDK 21 matrix** — CI гоняет на 21, 22, 23, 25; разница логируется.
- **Signed release** — Sigstore keyless + SBOM (`CycloneDX`).
- **1.0.0 notes** — миграционные заметки для тех, кто начал на 0.x; обещание SemVer.

---

## Что важно помнить между сессиями

1. **Кэш Gradle** — `./gradlew --stop` между прогонами, чтобы избежать «почему-то кэшируется старый манифест».
2. **Не ломать API 0.3.0.** Каждая сессия начинается с запуска `./gradlew test` всего репозитория; если что-то упало — чиним до начала новой работы.
3. **Спанглиш-именование** — придерживаемся стиля Vida: `Bloque`, `Latido`, `Susurro`, `Puerta`, `Cartografía`. Новые публичные типы — ревью на соответствие глоссарию `[docs/glossary.md](./glossary.md)`.
4. `**@ApiStatus`** — каждый новый public-type получает `@Stable` / `@Preview("<модуль>")` / `@Internal`. Без аннотации — ошибка CI.
5. **Docs-first контракт** — прежде чем писать код новой фичи, описываем её в `docs/` (как она видится пользователю). Код подтягивается под описание.

## Риски и альтернативы

- **Render-моды (Valenta/Shaders) тяжелее, чем выглядят.** Если session 6–7 не помещаются в отведённое время, разделяем Valenta на «рендер-ядро + qualities-of-life как отдельный mod Valenta-Extra». Это не ухудшает UX, пользователь всё равно устанавливает `valenta` как зависимость `valenta-extra`.
- **Sodium/Iris используют SPIRV-Cross и патчи OpenGL-драйверов.** Мы не копируем их — пишем по спецификации OpenGL 4.6 core; если драйвер глючит, документируем в `docs/mods/valenta/compat-matrix.md`.
- **Если Mojang в 1.22 ломает render-pipeline.** Роадмап гибкий — 0.6.x/0.7.x сдвигаются, предыдущие модули остаются стабильными на 1.21.1 LTS.

## Что НЕ входит в план

- Порт мода-комбайна (Create-подобные механизмы). Не наш слой — это задача мод-авторов, не платформы.
- Shaders-pack уровня BSL/Complementary. `Estorosso` — намеренно простой, чтобы быть референсом для других авторов.
- Перенос Vida на Minecraft Bedrock. См. `[docs/roadmap.md#не-в-планах](./roadmap.md#не-в-планах)`.