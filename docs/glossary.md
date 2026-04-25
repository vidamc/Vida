# Глоссарий

Vida использует собственный словарь — ни одно название не заимствовано у существующих систем моддинга. Часть терминов — испанские и латинские слова, созвучные названию проекта. Часть — технические.

Страница отсортирована по алфавиту. Для каждого термина приводится краткое определение, откуда оно берётся в коде и где читать дальше. Чтобы найти **страницу модуля или гайд по имени файла**, используйте [полный каталог `docs/`](index.md) или [обзор модулей](modules/index.md).

Согласованность терминов с кодом и правилами IDE — в [`.cursor/rules/vida-project.mdc`](../.cursor/rules/vida-project.mdc) (краткая таблица для контрибьюторов и инструментов).

---

### Ajustes

_исп. «настройки»_. Модуль конфигурации Vida. Иммутабельный снимок дерева узлов `ConfigNode` с dotted-path доступом и типизированными геттерами (strict / optional / with-default / Result-style). Формат — TOML (через `tomlj`). Встроенная поддержка профилей `[profile.<name>]` и внешних overlay.

- Пакет: `dev.vida.config`
- Документация: [modules/config.md](./modules/config.md), [guides/ajustes.md](./guides/ajustes.md)

### ApiStatus

Набор аннотаций-маркеров стабильности публичного API: `@Stable`, `@Preview("name")`, `@Internal`. Каждый публичный класс Vida помечен одним из них.

- Пакет: `dev.vida.core`
- Документация: [reference/api-stability.md](./reference/api-stability.md)

### BrandingEscultor

_«эскультор брендинга»_. Встроенный `Escultor`, переписывающий строку F3-оверлея — в итоге в F3 остаётся аккуратное `Minecraft … (vida)` без суффиксов загрузчика.

- Класс: `dev.vida.escultores.BrandingEscultor` (модуль `:escultores`)

### Cartografía

_исп. «картография»_. Подсистема маппингов имён. Читает Proguard-маппинги Mojang, хранит их в собственном компактном формате `.ctg`, применяет через `CartografiaRemapper` (поверх ASM). Иммутабельное `MappingTree` с несколькими namespace (`obf`, `intermediary`, `mojang`).

- Пакет: `dev.vida.cartografia`
- Документация: [modules/cartografia.md](./modules/cartografia.md)

### Catalogo

_исп. «каталог»_. Типизированный реестр контента: блоки, предметы, звуки, рецепты. Не допускает повторной регистрации одного `CatalogoClave` и явно выделяет фазу «open» (регистрация) от «frozen» (рантайм).

- Пакет: `dev.vida.base.catalogo`
- Документация: [modules/base.md](./modules/base.md), [guides/catalogo.md](./guides/catalogo.md)

### Discovery

Модуль обнаружения модов. Сканирует `mods/`, поддерживает вложенные JAR. Результат — `DiscoveryReport` (успехи + ошибки + статистика) и кэш `mods.idx` для пропуска повторного разбора неизменных файлов.

- Пакет: `dev.vida.discovery`
- Документация: [modules/discovery.md](./modules/discovery.md)

### Escultores

_исп. «скульпторы»_. Низкоуровневые трансформеры байткода (`Escultor`: `mightMatch` + `tryPatch`). В отличие от `Vifada`, не требуют статических аннотаций на цели — подходят для точечных правок. Публичный контракт **`@Stable`**; декларация классов в `vida.mod.json` (`escultores`).

- Пакет: `dev.vida.escultores`
- Документация: [modules/escultores.md](./modules/escultores.md), [guides/escultores.md](./guides/escultores.md)

### Fuente

_исп. «источник»_. Модуль **data-driven** контента: парсинг JSON из datapack-путей в JAR по ключу `custom["vida:dataDriven"]` в `vida.mod.json`. Поддерживаются блоки, предметы, shaped-рецепты (`vida:shaped`), таблицы лута (`loot_tables/**`) с проверкой внутренних ссылок, а также **`worldgen/**`** — эвристический сбор ссылок на loot и блоки мода с валидацией против того же снимка Fuente.

- Пакет: `dev.vida.fuente`
- Документация: [modules/fuente.md](./modules/fuente.md), [reference/manifest-schema.md](./reference/manifest-schema.md)

### Identifier

Namespaced-идентификатор вида `ns:path`. Базовый примитив для `Catalogo`, имён событий и пакетов. Строго ASCII, без пробелов; валидация в `ModManifest.ID_PATTERN`.

- Пакет: `dev.vida.core`

### InstallMode

Режим установки в мульти-лаунчер инсталляторе: `CREATE_NEW_PROFILE` (создать новый instance/профиль) или `PATCH_EXISTING_INSTANCE` (добавить `-javaagent` в уже существующий).

- Пакет: `dev.vida.installer.launchers`

### Juego / JuegoLoader

_исп. «игра»_. Промежуточный `ClassLoader`, в котором живёт Minecraft и `vida-base`. Родитель всех мод-лоадеров.

- Документация: [architecture/classloading.md](./architecture/classloading.md)

### Latidos

_исп. «биение сердца»_. Высокопроизводительная система событий. Шина (`DefaultLatidoBus`) вызывает `Oyente` напрямую — **без рефлексии на пути `emitir`**. Авто-биндер `LatidoRegistrador` после разрешения метода по возможности использует **`MethodHandle`**. Приоритеты, отмена, fan-out — см. [guides/latidos.md](../guides/latidos.md).

- Пакет: `dev.vida.base.latidos`
- Документация: [modules/base.md](./modules/base.md), [guides/latidos.md](./guides/latidos.md)

### Latidos Profundos

_«глубокие биения»_. Расширение `Latidos`, в котором подписка явно декларирует `Ejecutor` — стратегию исполнения обработчика: `SINCRONO`, `hiloPrincipal(...)`, `susurro(...)` или `serializado(...)`. Стабилизировано в 0.3.0.

- Документация: [guides/latidos.md](./guides/latidos.md#latidos-profundos--ejecutor), [modules/base.md](./modules/base.md)

### Morph / @VifadaMorph

_исп. «форма»_. Класс, отмеченный `@VifadaMorph(target = "...")`, описывает изменения, которые Vifada вносит в целевой класс. Аналог концептуального «mixin» в других системах, но с иной моделью слияния.

- Пакет: `dev.vida.vifada`
- Документация: [modules/vifada.md](./modules/vifada.md), [guides/vifada.md](./guides/vifada.md)

### ModContext

«Паспорт мода» — объект, который загрузчик передаёт в `VidaMod.iniciar(ctx)`. Через него мод получает шину событий, реестры, настройки, логгер и директорию для данных. Не даёт прямого доступа к другим модам — только через публичные API.

- Пакет: `dev.vida.base`

### Puertas

_исп. «двери»_. Система расширения доступа. Перечень директив в `.ptr`-файле описывает, какие классы/методы/поля должны стать `accesible` (public), `extensible` (non-final) или `mutable` (non-final для поля). Парсер (`PuertaParser`) работает в сборке (через `vidaValidatePuertas`/`vidaPackagePuertas` Gradle-плагина) и в рантайме (через `AplicadorPuertas`, интегрированный с `Cartografía`). Поддерживает namespace `crudo`, `intermedio` (рекомендуемый), `exterior`.

- Пакет: `dev.vida.puertas`
- Документация: [modules/puertas.md](./modules/puertas.md), [guides/puertas.md](./guides/puertas.md)

### Resolver

Модуль разрешения зависимостей. SAT-бэктрекинг с unit-пропагацией диапазонов. Поддерживает REQUIRED / OPTIONAL / INCOMPATIBLE, pin'ы, лимиты, alias'ы через `provides`.

- Пакет: `dev.vida.resolver`
- Документация: [modules/resolver.md](./modules/resolver.md)

### Susurro

_исп. «шёпот»_. Управляемый thread-pool Vida: приоритеты `URGENTE → BAJA`, back-pressure по `Etiqueta`, именованные daemon-потоки. `Tarea<T>` расширяет `CompletableFuture`, поддерживает отмену, плазмы (`conPlazo`) и маршалинг результата в главный поток через `HiloPrincipal`. Стабилизировано в 0.3.0.

- Пакет: `dev.vida.susurro`
- Документация: [modules/susurro.md](./modules/susurro.md)

### Tarea

_исп. «задача»_. `CompletableFuture<T>` с приоритетом, `Etiqueta`, статусами (`PENDIENTE → EJECUTANDO → COMPLETADA/FALLADA/CANCELADA`), колбэками `enExito` / `enFallo` / `enFin` и поддержкой deadline. Основной возвращаемый тип `Susurro`.

- Пакет: `dev.vida.susurro`
- Документация: [modules/susurro.md](./modules/susurro.md)

### HiloPrincipal

_исп. «главный поток»_. Маркер-интерфейс для main-thread rail'а клиента/сервера MC. Позволяет `Ejecutor.hiloPrincipal(...)` и `Tarea.enHiloPrincipal(...)` переносить исполнение в главный поток без привязки к конкретной реализации MC.

- Пакет: `dev.vida.susurro`
- Документация: [modules/susurro.md](./modules/susurro.md)

### Etiqueta

_исп. «этикетка»_. Строковый тег, используемый `Susurro` для back-pressure (лимит одновременных задач на тег) и отладки. Также передаётся в `Ejecutor.susurro(...)` для группировки подписок в шине.

- Пакет: `dev.vida.susurro`

### Bloque

_исп. «блок»_. API блоков в модуле `vida-bloque`: иммутабельный `Bloque`, фасадные `PropiedadesBloque`, коллизии (`FormaColision`), звуки (`SonidoBloque`), требования к инструментам (`NivelHerramienta` / `TipoHerramienta`), per-position state (`BloqueEntidad`), реестр `RegistroBloques` и типизированные теги `EtiquetaBloque`.

- Пакет: `dev.vida.bloque`
- Документация: [modules/bloque.md](./modules/bloque.md)

### Objeto

_исп. «предмет»_. API предметов в модуле `vida-objeto`: `Objeto`, `PropiedadesObjeto`, материалы и инструменты (`Material`, `Herramienta`), BlockItem-эквивалент (`ObjetoDeBloque`), собственная система data-components (`Componente`, `ClaveComponente`, `MapaComponentes`) и `RegistroObjetos` / `EtiquetaObjeto`.

- Пакет: `dev.vida.objeto`
- Документация: [modules/objeto.md](./modules/objeto.md)

### Tejido

_исп. «ткань»_. Запланированный API для сетевого взаимодействия: кастомные пакеты клиент↔сервер, packet-splitter для больших полезных нагрузок, потокобезопасность.

### TransformingClassLoader

Класс-лоадер, применяющий `VidaClassTransformer` к загружаемым байтам. Используется, когда `VidaPremain` работает программно (тесты, инструменты) без JVM-агента.

- Пакет: `dev.vida.loader`

### Vifada

Байткод-трансформер Vida. Декларативные аннотации `@VifadaMorph`, `@VifadaInject`, `@VifadaOverwrite`, `@VifadaShadow`, `@VifadaAt`. Построен на ASM (ObjectWeb), спрятан за `dev.vida.vifada.Transformer#transform(...)`.

- Пакет: `dev.vida.vifada`
- Документация: [modules/vifada.md](./modules/vifada.md), [guides/vifada.md](./guides/vifada.md)

### VidaBoot

Программная инициализация Vida без участия JVM-агента. Точка входа для тестов и инструментов.

- Пакет: `dev.vida.loader`

### VidaMod

Интерфейс главной точки входа мода. Контракт жизненного цикла: `iniciar(ctx)` → `ACTIVO` → `detener(ctx)`.

- Пакет: `dev.vida.base`
- Документация: [modules/base.md](./modules/base.md)

### VidaPremain

Java-агент Vida. Читает `BootOptions`, делегирует в `BootSequence`, регистрирует `VidaClassTransformer` в `Instrumentation`. Манифест: `Can-Redefine-Classes: false`, `Can-Retransform-Classes: false` — сознательно, ради производительности.

- Пакет: `dev.vida.loader`
- Документация: [architecture/bootstrap.md](./architecture/bootstrap.md)

### Vigia

_исп. «страж»_. Встроенный JFR-sampling-профайлер (модуль `vigia`, **0.4.0+**): `VigiaSesion`, HTML-отчёты, команда `/vida profile`. См. [modules/vigia.md](./modules/vigia.md).

### ZipReader

Абстракция над zip/jar для модуля `discovery`. Две реализации: `FileZipReader` (файл на диске) и `BytesZipReader` (in-memory для вложенных JAR).

- Пакет: `dev.vida.discovery`
