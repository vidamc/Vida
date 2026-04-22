# Документация Vida

Добро пожаловать в документацию загрузчика модов **Vida**. Это живой источник, из которого следует начинать знакомство с проектом: сюда стекаются все руководства, справочники и архитектурные заметки.

Если вы ищете общий обзор — [`README.md`](../README.md) в корне. Если вы уже знаете, что делаете, — навигация ниже.

## Навигация

### Getting Started
Для тех, кто подошёл к Vida впервые.

- [Установка для игрока](./getting-started/installation.md) — выбор лаунчера, artifacts, Sigstore, `SHA256SUMS`.
- [Dev-окружение](./getting-started/dev-environment.md) — JDK 21, Gradle 9.x, подключение плагина.
- [Первый мод за 10 минут](./getting-started/first-mod.md) — `VidaMod`, `ModContext`, первый `Latido`.

### Архитектура
Как Vida устроена внутри — ровно настолько, чтобы перестать бояться правок.

- [Обзор](./architecture/overview.md) — модули и их зависимости.
- [Bootstrap / VidaPremain](./architecture/bootstrap.md) — как агент перехватывает JVM.
- [ClassLoader'ы](./architecture/classloading.md) — три слоя изоляции.
- [Жизненный цикл мода](./architecture/lifecycle.md) — от дискавери до `detener()`.
- [Философия производительности](./architecture/performance.md) — почему тик главного потока — святое.

### Модули
Поддерживаются Javadoc-подобные страницы на каждый подпроект.

- [Обзор модулей](./modules/index.md)
- [core](./modules/core.md) · [manifest](./modules/manifest.md) · [config](./modules/config.md)
- [cartografia](./modules/cartografia.md) · [discovery](./modules/discovery.md) · [resolver](./modules/resolver.md)
- [vifada](./modules/vifada.md) · [loader](./modules/loader.md)
- [base](./modules/base.md) · [gradle-plugin](./modules/gradle-plugin.md) · [installer](./modules/installer.md)
- **0.3.0 (preview):** [`bloque`](./modules/bloque.md) · [`objeto`](./modules/objeto.md) · [`susurro`](./modules/susurro.md) · [`puertas`](./modules/puertas.md) · `base.latidos.Ejecutor` · `vidaValidatePuertas` / `vidaPackagePuertas`. Полное описание — в [`CHANGELOG.md`](../CHANGELOG.md#030--2026-04-22).
- **0.4.0 (preview):** [`vigia`](./modules/vigia.md) · reflection `LatidoRegistrador` · [installer](./modules/installer.md): **Modrinth App** / **CurseForge App** · `--validate-puertas`.
- **0.5.0 (preview):** [`entidad`](./modules/entidad.md) · [`mundo`](./modules/mundo.md) · `base.latidos.@OyenteDeTick`.
- **0.6.0 (preview):** [`render`](./modules/render.md) · [`red`](./modules/red.md) · data-driven prototype (`vida.mod.json` + datapack, без Java) · `@VifadaMulti`/`@VifadaLocal` (`vifada-next`).
- **0.7.0+:** примеры модов в `mods/` — `Saciedad`, `Senda`.
- **0.8.0+:** платформенный мост — [loader](./modules/loader.md) (`SyntheticProviders`, `VanillaBridge`, платформенные морфы).
- **0.9.0+:** мод **Valenta** — [architecture](./mods/valenta/architecture.md) · [compat](./mods/valenta/compat-matrix.md).

### Guides
Глубокие материалы по конкретным API.

- [Latidos — события](./guides/latidos.md) *(обновлён под `Ejecutor` в 0.3.0)*
- [Первая сущность](./guides/first-entity.md) *(новый в 0.5.0)*
- [Catalogo — реестры контента](./guides/catalogo.md)
- [Ajustes — конфиги](./guides/ajustes.md)
- [Vifada — модификация байткода](./guides/vifada.md)
- [Puertas — access wideners](./guides/puertas.md) *(новый в 0.3.0)*
- [Escultores — низкоуровневые трансформеры](./guides/escultores.md)
- [Мульти-лаунчер инсталлятор](./guides/multi-launcher.md)

### Reference
Точные, компактные справочники — для IDE-подсказок и CI.

- [`vida.mod.json` schema](./reference/manifest-schema.md)
- [CLI установщика](./reference/cli-installer.md)
- [API stability / versioning](./reference/api-stability.md)

### Прочее

- [FAQ](./faq.md)
- [Troubleshooting](./troubleshooting.md)
- [Глоссарий](./glossary.md)
- [Roadmap](./roadmap.md)
- [Session roadmap](./session-roadmap.md) — план работ 0.4.0 → 1.0.0 и модов Saciedad / Valenta / Vida Shaders + Estorosso.
- [`CHANGELOG.md`](../CHANGELOG.md) — история релизов.

## Про стабильность

Каждый публичный класс в Vida помечен одной из аннотаций [`dev.vida.core.ApiStatus`](./reference/api-stability.md):

- `@Stable` — API зафиксирован; breaking changes только в мажорной версии.
- `@Preview("<name>")` — API обкатывается; breaking changes допустимы до 1.0.0 и явно документируются в `CHANGELOG.md`.
- `@Internal` — не для использования вне загрузчика; может меняться в любом патче.

Эта страница и весь `docs/` поддерживаются вместе с кодом. Если нашли несоответствие — [issue](https://github.com/vidamc/Vida/issues/new/choose) или PR с исправлением.
