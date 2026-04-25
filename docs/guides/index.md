# Guides

Практические материалы: **как** использовать API и инструменты. Обзоры модулей («что есть в Gradle-проекте») — в [modules/](../modules/index.md); полный список всех файлов `docs/` — на [главной странице документации](../index.md).

## Навигатор по уровням сложности

- [**Полный набор моддера**](./modder-toolkit.md) — Gradle, задачи `vida*`, JVM-свойства, автозависимость на `vida`, оглавление ссылок на остальную документацию.
- [**Три дорожки моддинга (API / Vifada / Escultores)**](./modding-paths.md) — карта выбора подхода и фазы освоения.
- [**Политики доступа резолвера**](./resolver-policies.md) — `accessDeniedIds`, отличие от исключений пути, ошибка `AccessPolicyDenied`.

## Полный список руководств

| Файл | Для кого | О чём |
|------|----------|--------|
| [modder-toolkit.md](./modder-toolkit.md) | Все моддёлы | Единая точка входа: DSL, задачи, BOM. |
| [modding-paths.md](./modding-paths.md) | Выбор стека | API vs байткод vs Escultores. |
| [resolver-policies.md](./resolver-policies.md) | Паки / CI | Запрет модов при резолве. |
| [latidos.md](./latidos.md) | Автор gameplay | События, приоритеты, исполнители. |
| [catalogo.md](./catalogo.md) | Контент | Реестры блоков и предметов. |
| [first-entity.md](./first-entity.md) | World / entities | `vida-entidad`, `vida-mundo`. |
| [ajustes.md](./ajustes.md) | Конфигурация | TOML и типизированные ключи. |
| [vifada.md](./vifada.md) | Байткод | `@VifadaMorph`, Inject, Shadow. |
| [puertas.md](./puertas.md) | Доступ к vanilla | `.ptr` wideners. |
| [escultores.md](./escultores.md) | Низкий уровень ASM | Когда Vifada мало. |
| [hot-reload.md](./hot-reload.md) | Dev | `./gradlew` + watcher, ограничения. |
| [multi-launcher.md](./multi-launcher.md) | Инсталлятор | Prism, MultiMC, ATLauncher, Mojang. |

Связанные модули без отдельного «guide»: data-driven datapack описан в [modules/fuente.md](../modules/fuente.md); установка для игрока — [getting-started/installation.md](../getting-started/installation.md).
