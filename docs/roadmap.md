# Roadmap

Короткий, живой документ. Детали — в GitHub Projects / Milestones.

**План вынесенного вперёд развития публичного API для мододелов (оси, фазы A–E, критерии DoD):** [roadmap/ultimate-api-plan.md](roadmap/ultimate-api-plan.md).

## Done (0.1.x — 1.0.0)

- **1.0.0 (Session 9).** Стабилизация `@ApiStatus.Stable` для `base`, `bloque`, `objeto`, `susurro`, `puertas`, Gradle plugin; `vidaDocTest` для проверки примеров в `docs/` (`package dev.vida.*`); JDK CI matrix 21–25; CycloneDX SBOM в релизе (Syft); `docs/migration/1.0.0.md`, `docs/security/audit-1.0.md`.
- **Core bootstrap (0.1.x).** Skeleton репозитория, CI/CD, release-please, Sigstore-подписи; `core`, `manifest`, `config`, `cartografia`, `discovery`, `resolver`, `vifada`, `loader`, `base` (Latidos, Catalogo, Ajustes, `VidaMod`, `ModContext`), `gradle-plugin` (`vidaGenerateManifest`, `vidaValidateManifest`, `vidaRemapJar`, `vidaRun`), `installer` (GUI + CLI, Mojang / Prism / MultiMC / ATLauncher), `BrandingEscultor`. Unit- + интеграционные + контрактные + installer-IT покрытие.
- **API и рантайм-модули (0.3.0).**
  - `**vida-bloque`** — публичный API для блоков: `Bloque`, `PropiedadesBloque` с fluent-builder, `MaterialBloque`, `FormaColision` (union AABB), `SonidoBloque`, `NivelHerramienta`/`TipoHerramienta`, `BloqueEntidad`, `RegistroBloques` с типизированными `EtiquetaBloque`.
  - `**vida-objeto`** — `Objeto`, `PropiedadesObjeto`, `Material`, `Herramienta`, `Raridad`, `TipoObjeto`; `Componente` + `ClaveComponente` + `MapaComponentes` (data-components 1.21.1: `Durabilidad`, `Comida`, `Irrompible`, `AtributosModificados`, …); `ObjetoDeBloque`; `RegistroObjetos` + `EtiquetaObjeto`.
  - `**vida-susurro`** — managed thread-pool: `Susurro.Politica`, `Prioridad`, `Etiqueta` (back-pressure), `Tarea<T>` (с `orTimeout`/`conPlazo` как cancellation), `HiloPrincipal` (main-thread marshalling через `pulso()`), `Susurro.Estadisticas`.
  - `**vida-puertas`** — access-wideners: формат `.ptr` (namespaces `crudo` / `intermedio` / `exterior`), `PuertaParser` с детальной диагностикой ошибок, `AplicadorPuertas` на ASM (accesible / extensible / mutable по class/method/field), `PuertaArchivo.combinar`, индексация по `claseInternal`.
  - **Latidos profundos в `base`.** `Ejecutor` (SAM) с фабриками `SINCRONO`, `hiloPrincipal(hp)`, `susurro(s, …)`, `serializado(name)`; новая перегрузка `LatidoBus.suscribir(..., Ejecutor, Oyente)` — старые методы сохранены, дефолт `SINCRONO`, обратная совместимость API 0.1.x не нарушена. Аннотация `@EjecutorLatido` — для биндера `**LatidoRegistrador`** (сканирование при регистрации; рантайм по возможности через `**MethodHandle`**).
  - **Gradle-plugin.** `vidaValidatePuertas` (PuertaParser с `@InputDirectory` resources root), `vidaPackagePuertas` (копирует валидные `.ptr` в `build/generated/vida/puertas/`), авто-подключение к `processResources` и зависимость `Jar` → `vidaValidatePuertas`.
  - **Installer.** Флаг `--validate-puertas <file|dir>`: рекурсивный обход, exit-code 0/1/2.
  - Тесты: `bloque`, `objeto`, `susurro`, `puertas`, `base/Ejecutor`, functional-tests gradle-plugin через TestKit, installer CLI — все green.
- **DX и наблюдаемость (0.4.0).**
  - `**vigia`** — встроенный JFR-профайлер: `VigiaSesion`, `Resumen`, `VigiaReporte`, `VigiaComando`.
  - **Биндер `@EjecutorLatido` / `@OyenteDeTick`.** `LatidoRegistrador` + `LatidoRegistradorError`, авто-регистрация методов мода с `Susurro` и `HiloPrincipal`.
  - **Installer.** Поддержка Modrinth App и CurseForge App.
  - **Документация.** Полные страницы под `vigia`, `base-ejecutor`, расширение `installer`.
- **Мир и сущности (0.5.0).**
  - `**vida-entidad`** — `Entidad`, `TipoEntidad`, `PropiedadesEntidad`, `RegistroEntidades`; entity data-components: `ComponenteEntidad`, `ClaveComponenteEntidad`, `MapaComponentesEntidad`.
  - `**vida-mundo`** — `Mundo`, `Coordenada`, `Dimension`, `Bioma`, `LatidosMundo` (`MundoCargado`, `ChunkCargado`, `Tick`, `NocheAmanece`); в **2.0** добавлены `ChunkCoordenada`, `RegionCoordenada`, `LimitesVerticales`, `ChunkDescargado`, default-методы `Mundo` для высоты — см. [`modules/mundo.md`](modules/mundo.md).
  - `**base`** — `@OyenteDeTick(tps=...)` как shortcut над `LatidoPulso`; `LatidoRegistrador` валидирует конфликт с `@EjecutorLatido` и диапазон `tps`.
  - **Документация.** `docs/modules/entidad.md`, `docs/modules/mundo.md`, `docs/guides/first-entity.md`, обновления индексов и reference-страниц.
- **Рендер, сеть, data-driven (0.6.0).**
  - `**vida-render`** — `ModeloBloque`, `ModeloEntidad`, `TextureAtlas`, `RenderPipeline`, shader hooks, safe fallback (`cube + missing-texture`).
  - `**vida-red`** — `PaqueteCliente` / `PaqueteServidor`, `TejidoCanal`, auto record-codec (`CodificadorRegistros`), versioned codecs и back-pressure.
  - **Loader + Fuente.** Чтение `custom["vida:dataDriven"]` и datapack JSON для блоков, предметов, shaped-рецептов; далее расширено в **2.0** (loot tables, стабильный контракт — см. §2.0 ниже).
  - **Vifada.** Изначально заявлены расширения `@VifadaMulti` / `@VifadaLocal`; полная реализация **Vifada 2** и `@VifadaRedirect` — в линии **2.0 «Масштаб»**.
  - **Документация.** Страницы `docs/modules/render.md`, `docs/modules/red.md`, обновления `vifada.md`, индексов, roadmap и schema reference.
- **Первые моды (0.7.0, Session 5).**
  - Опциональные сценарии `mods/saciedad` и `mods/senda` (только при наличии папок в `settings.gradle.kts`) — `vida.render`, Ajustes, Latidos, Vifada; подробности — [session-roadmap](session-roadmap.md) (Session 5).
- **Платформа MC-моста (0.8.0, Session 6).**
  - `SyntheticProviders` — провайдеры `vida` / `minecraft` / `java` до `Resolver.resolve()`; `BootOptions.vidaVersion` / `minecraftVersion`; `META-INF/vida/loader-version.properties`.
  - `dev.vida.platform`: `PlatformBridge`, `VanillaBridge`, `MinecraftTickMorph`, `GuiRenderMorph`; `BootSequence` регистрирует морфы и мост.
  - Документация в `docs/modules/loader.md`.
- **Клиентский рендер Sodium-класса (0.9.0, Session 7).**
  - Зафиксированы цели: VBO / indirect draw, chunk meshing через `Susurro`, culling, QoL; отдельный мод в этом репозитории не подключается (см. [session-roadmap.md](session-roadmap.md)).
  - Инсталлятор без изменения CLI: тот же fat-jar ставит агент; релизы 0.5.0—0.9.0 оформлены в [modules/installer.md](modules/installer.md#соответствие-релизам-платформы-050--090).
- **Пометка 0.9.5 (Session 8), done** — [session-roadmap.md](session-roadmap.md), [CHANGELOG 0.9.5](CHANGELOG.md#095--2026-04-22).

## Next — подготовка к 1.0.0 (Session 9+)

- **Session 9** — стабилизация 1.0.0: `@Preview` → `@Stable` где зрело, `vidaDocTest`, security audit, матрица JDK, подписи релиза (см. [session-roadmap.md](session-roadmap.md)).
- См. также [session-roadmap.md](session-roadmap.md) для деталей.

## 1.0

1.0 — это обещание стабильности. Критерии выхода:

- Все API-пакеты аннотированы `@ApiStatus.Stable` или `@Internal` — никакого `Preview`.
- Документация полная, примеры компилируются в CI (`vidaDocTest`).
- Не меньше 3 публично анонсированных модов на Vida в продакшн-качестве.
- Прохождение аудита безопасности внешним ревьюером.
- Gradle-плагин кросс-тестируется на Gradle 9.x и 10.x.

---

## После 1.0 — мажорные релизы

SemVer соблюдается строго: каждый мажор может ломать `@Stable` API, но только после ≥12 месяцев `@Deprecated(forRemoval=true)` и миграционной заметки в `CHANGELOG.md`. У каждого мажора — одна тема; минорные релизы внутри мажора наращивают её, не уводя в сторону.

Ориентировочный темп: 1.0 → 2.0 примерно 12–18 месяцев, далее — 18–24 месяца на мажор. LTS-окно: N-1 получает только security- и critical-bug фиксы в течение 24 месяцев после выхода N.

### 2.0 — «Масштаб»

**Детальный план работ и критерии GA:** [roadmap/2.0-plan.md](roadmap/2.0-plan.md).

**Состояние документации и репозитория:** реализованы Vifada 2 (`@VifadaMulti` / `@VifadaLocal` / `@VifadaRedirect`), расширенный data-driven контент (включая `loot_tables/`), dev hot-reload (`docs/guides/hot-reload.md`), телеметрия opt-in (`docs/security/telemetry-v1.md`), офлайн-кэш и докачка HTTP в инсталляторе (`docs/modules/installer.md`), регрессия сканирования 300+ модов, стабилизация публичных пакетов `entidad` / `mundo` / `render` / `fuente` / `vifada` / ключевых типов `vigia` — см. [CHANGELOG.md](../CHANGELOG.md), [migration/2.0.0.md](migration/2.0.0.md), [reference/api-stability.md](reference/api-stability.md). В статусе `**@Preview`** остаются `**loader`** и `**installer**`.

Тема: модпаки на 300+ модов и автор-experience уровня индустриальных SDK.

- **Vifada 2** — аннотации `@VifadaMulti` (один метод патчит N таргетов), `@VifadaLocal` (безопасный доступ к LVT), `@VifadaRedirect` (замена конкретных call-sites), диагностика конфликтов между морфами с предложением приоритета.
- **Declarative mods** — полноценный data-driven мод: блок/предмет/рецепт/таблица без единой строки Java, только `vida.mod.json` + датапак. Для ≈60% простых модов код перестаёт быть обязательным.
- **Hot-reload в dev** — `./gradlew vidaRun --hot` переподгружает изменённые классы мода и пересобирает активные `Catalogo`-реестры без рестарта клиента.
- **API-модули стабилизируются полностью**: `vida-bloque`, `vida-objeto`, `vida-entidad`, `vida-mundo`, `vida-render`, `vida-red` — все уходят из `@Preview` в `@Stable`.
- **Susurro / Latidos Profundos / Vigia** — если не успели в 0.3.x/0.4.x, стабилизируются здесь; `vigia` становится обязательным dev-инструментом (`/vida profile`, flame-graph в HTML).
- **Installer v2** — Modrinth App и CurseForge вышли стабильно; добавляется offline-режим и resumable-download для больших модпаков.
- **Telemetry v1 (opt-in)** — агрегированные метрики cold-start, частоты фризов, топ-морфов по времени. Только явное согласие, локальная агрегация, никакой PII.
- **LTS 1.x** начинает 24-месячное окно поддержки.

### 3.0 — «Сервер»

Тема: серверный стек первого класса — headless, sync, proxy-friendly.

- **Headless-профиль `vida-loader`** — сборка без Swing/AWT/OpenGL-зависимостей для контейнеров и root-серверов. Docker-образы `vidamc/vida-server:1.21.1-3.x` с K8s-friendly signals.
- **Server-side-only моды** — официальная классификация `side: server` в `vida.mod.json`, отдельный жизненный цикл, запрет на клиентские API на compile-time.
- **Sync-реестры через Tejido** — автоматическая синхронизация `Catalogo` клиент↔сервер с версионированием и negotiation'ом missing-content: сервер с модом X работает с клиентом без X в read-only режиме, если мод помечен `compatibility: optional`.
- **Server scripting** — лёгкий Kotlin Script DSL для админов: правила спауна, лимиты, GUI-команды. Запускается в sandbox-класслоадере без доступа к reflection/io.
- **Proxy-aware API** — стандартизированные hook'и для backend'ов за Velocity/BungeeCord-подобными прокси: server-switch события, кросс-сервер синхронизация `Ajustes`.
- **Анти-чит surface** — публичный контракт «серверная истина» для движения, инвентаря, взаимодействий; моды декларируют, какие инварианты они меняют, сервер отказывает несогласованным клиентам.
- **Zero-downtime restart** — `/vida rehydrate` перезагружает конфигурационные overlay'ы без кика игроков.

### 4.0 — «Платформа»

Тема: Vida перестаёт быть «только загрузчиком» и становится платформой разработки модов.

- **Vida Studio (CLI)** — самостоятельный инструмент: REPL по контексту загруженного клиента, class-inspector (живая навигация по Vifada-стэку трансформаций), профайлер, diff маппингов между MC-версиями. Без IDE-привязки.
- **AOT / native-image бинарники** — `vida-installer` и `vida-server-headless` собираются через GraalVM в нативные исполняемые файлы: запуск < 200 мс, отсутствие требования установленного JDK у пользователя.
- **Multi-version мод-JAR** — один артефакт автора, скомпилированный Gradle-плагином для N поддерживаемых MC-версий одновременно: внутри JAR секции `mc-1.21.1/`, `mc-1.22/`, … выбираются загрузчиком по рантайму.
- **Auto-mapping pipeline** — если Mojang-маппинги для версии ещё не опубликованы, Vida умеет автогенерировать «черновые» маппинги анализом двух соседних jar'ов. Экономит 0–14 дней ожидания при первом порте.
- **Plug-in система для Vifada** — сторонние авторы могут добавить свои трансформационные примитивы (новые аннотации) без форка. Изоляция через отдельный ClassLoader и явную регистрацию.
- **Федеративный catalog-репозиторий** — опциональный протокол поиска модов по нескольким источникам (Modrinth, CurseForge, собственные индексы) единым API инсталлятора. Без привязки к одному провайдеру.
- **Gradle 11.x** как целевая, JDK 25 LTS как минимальная база.

### 5.0 — «Независимость»

Тема: долгосрочная устойчивость — меньше внешних зависимостей, больше формальных гарантий.

- **Formal verification ядра** — модель резолвера и инвариантов `ClassLoader`-изоляции проверены в TLA+ или Alloy; результаты публикуются как `docs/verification/`. Цель — доказать невозможность таких классов багов, как циклы в discovery и unsound-delegation между слоями лоадеров.
- **Mapping-независимость** — собственный формат `.ctg` остаётся единственным source-of-truth. Mojang-маппинги и любые внешние источники — только импортёры. Smoke-тесты гарантируют, что сборка проходит без сети.
- **Platform-independence core** — выделение `vida-nucleo` (manifest + resolver + classloading) как переиспользуемого ядра для non-Minecraft JVM-приложений, нуждающихся в моддинге (внутренние enterprise-сценарии, но не маркетинг вектор). Публикуется как отдельный `dev.vida:nucleo`.
- **Zero-downtime server upgrade** — смена ветки мода или Vida-версии без рестарта JVM: двухфазный commit, snapshot-restore `Catalogo`, миграция `Ajustes`. Ограничено модами, декларирующими `live-upgradable: true`.
- **Федеративный мод-поиск v2** — протокол с подписями, TUF-подобной цепочкой доверия и reproducible-build атрибутами; пользователь видит, собран ли мод из публичных сорцов, и каким CI.
- **Ликвидация `@Preview`** — весь API без исключения либо `@Stable`, либо `@Internal`. Никаких новых `@Preview`-пакетов вводить нельзя без RFC.
- **LTS-модель уточнена** — мажор N получает критические фиксы 36 месяцев, безопасность — 60 месяцев. Совместимость 5.x с 4.x на уровне манифеста гарантирована.

### За горизонтом 5.0

Явно не планируется до соответствующего RFC:

- Pluggable game-engine-адаптеры (Vida под не-Minecraft) — рассматривается только если `vida-nucleo` из 5.0 найдёт органический внешний интерес.
- Замена Gradle-плагина собственной build-системой — нет причин, пока Gradle покрывает сценарии.
- Интеграция с Bedrock Edition (см. «Не в планах»).

## Поддержка версий Minecraft

- Порт на новый мажорный релиз Minecraft начинается после публикации Mojang-маппингов.
- Цель — первая работающая сборка Vida для новой версии в течение **14 дней** после релиза (не снапшота) Minecraft.
- Обратная совместимость мод↔загрузчик: если мод не обращается к удалённым/переименованным vanilla-символам, он работает без перекомпиляции. Подробнее — [reference/api-stability.md](./reference/api-stability.md#поддержка-версий-minecraft).

## Не в планах

- Слой совместимости с другими системами моддинга. Это противоречит философии проекта и снизило бы планку по производительности.
- Поддержка Minecraft Bedrock. Vida — Java Edition only.
- Закрытые плагины / feature-flags в сборке.