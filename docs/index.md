# Документация Vida

Добро пожаловать в документацию загрузчика модов **Vida**. Отсюда удобно переходить к любому файлу в `[docs/](./)`: ниже — **полный каталог** с кратким назначением каждой страницы.

Корень репозитория: `[README.md](../README.md)` (обзор продукта), `[CHANGELOG.md](../CHANGELOG.md)`, `[CONTRIBUTING.md](../CONTRIBUTING.md)`, `[SECURITY.md](../SECURITY.md)`.

---

## Быстрые входы


| Цель                                    | Куда                                                                       |
| --------------------------------------- | -------------------------------------------------------------------------- |
| Первый запуск игры с Vida               | [getting-started/installation.md](./getting-started/installation.md)       |
| Настройка JDK / Gradle / IDE            | [getting-started/dev-environment.md](./getting-started/dev-environment.md) |
| Минимальный мод за один проход          | [getting-started/first-mod.md](./getting-started/first-mod.md)             |
| Карта Gradle + JVM + ссылки             | [guides/modder-toolkit.md](./guides/modder-toolkit.md)                     |
| Все модули монорепо                     | [modules/index.md](./modules/index.md)                                     |
| Справочники (схема манифеста, BOM, CLI) | [reference/index.md](./reference/index.md)                                 |
| Все практические гайды                  | [guides/index.md](./guides/index.md)                                       |
| Терминология Vida (Latidos, Fuente, …)  | [glossary.md](./glossary.md)                                               |


---

## Getting started


| Файл                                                       | Содержание                                                                            |
| ---------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| [installation.md](./getting-started/installation.md)       | Установка для игрока: лаунчеры, артефакты, Sigstore, проверка контрольных сумм.       |
| [dev-environment.md](./getting-started/dev-environment.md) | JDK 21+, Gradle 9.x, подключение плагина `dev.vida.mod`, типичный `build.gradle.kts`. |
| [first-mod.md](./getting-started/first-mod.md)             | Первый `VidaMod`, `ModContext`, Latido, манифест.                                     |


---

## Архитектура


| Файл                                              | Содержание                                                                  |
| ------------------------------------------------- | --------------------------------------------------------------------------- |
| [overview.md](./architecture/overview.md)         | Плоская карта модулей и зависимостей; таблица «одно предложение на модуль». |
| [bootstrap.md](./architecture/bootstrap.md)       | VidaPremain, `BootOptions`, цепочка `BootSequence`, Fuente в bootstrap.     |
| [classloading.md](./architecture/classloading.md) | Три слоя ClassLoader: агент, игра, моды; изоляция и делегирование.          |
| [lifecycle.md](./architecture/lifecycle.md)       | Жизненный цикл мода от discovery до остановки.                              |
| [performance.md](./architecture/performance.md)   | Без retransform, нулевая работа лоадера в игровом тике, профилирование.     |


---

## Модули (`docs/modules/`)

Обзор и граф зависимостей: [modules/index.md](./modules/index.md).


| Файл                                                   | Модуль Gradle   | Кратко                                                   |
| ------------------------------------------------------ | --------------- | -------------------------------------------------------- |
| [core.md](./modules/core.md)                           | `:core`         | `Identifier`, `Version`, `Result`, `ApiStatus`.          |
| [manifest.md](./modules/manifest.md)                   | `:manifest`     | Парсер `vida.mod.json`.                                  |
| [config.md](./modules/config.md)                       | `:config`       | Ajustes, TOML, профили.                                  |
| [cartografia.md](./modules/cartografia.md)             | `:cartografia`  | Маппинги, `.ctg`, ремаппер.                              |
| [discovery.md](./modules/discovery.md)                 | `:discovery`    | Скан `mods/`, nested JAR, кэш.                           |
| [resolver.md](./modules/resolver.md)                   | `:resolver`     | SemVer, SAT, политики доступа.                           |
| [vifada.md](./modules/vifada.md)                       | `:vifada`       | Vifada 2, аннотации морфов.                              |
| [loader.md](./modules/loader.md)                       | `:loader`       | Агент, трансформер, платформенный мост, Fuente snapshot. |
| [fuente.md](./modules/fuente.md)                       | `:fuente`       | Data-driven datapack JSON, loot, worldgen-ссылки.        |
| [escultores.md](./modules/escultores.md)               | `:escultores`   | API Escultor, BrandingEscultor.                          |
| [base.md](./modules/base.md)                           | `:base`         | `VidaMod`, Latidos, Catalogo, Ajustes.                   |
| [base-ejecutor.md](./modules/base-ejecutor.md)         | `:base`         | `Ejecutor`, latidos profundos, биндер.                   |
| [bloque.md](./modules/bloque.md)                       | `:bloque`       | Блоки, свойства, регистрация.                            |
| [objeto.md](./modules/objeto.md)                       | `:objeto`       | Предметы, компоненты.                                    |
| [entidad.md](./modules/entidad.md)                     | `:entidad`      | Сущности, hitbox, компоненты.                            |
| [mundo.md](./modules/mundo.md)                         | `:mundo`        | Мир, координаты, биомы, LatidosMundo.                    |
| [render.md](./modules/render.md)                       | `:render`       | Модели, атлас, пайплайн рендера.                         |
| [red.md](./modules/red.md)                             | `:red`          | Tejido, пакеты, codecs.                                  |
| [susurro.md](./modules/susurro.md)                     | `:susurro`      | Пул задач, back-pressure.                                |
| [puertas.md](./modules/puertas.md)                     | `:puertas`      | Access wideners `.ptr`.                                  |
| [vigia.md](./modules/vigia.md)                         | `:vigia`        | Профайлер JFR, отчёты.                                   |
| [gradle-plugin.md](./modules/gradle-plugin.md)         | `gradle-plugin` | DSL `vida { }`, задачи, remap, run.                      |
| [installer.md](./modules/installer.md)                 | `:installer`    | GUI/CLI установщик, лаунчеры.                            |
| [platform-profiles.md](./modules/platform-profiles.md) | контракт        | Версии MC + Cartografía + морфы.                         |
| [bom.md](./modules/bom.md)                             | `:bom`          | Java Platform BOM `dev.vida:vida-bom`.                   |


---

## Guides (`docs/guides/`)

Навигатор по сложности и аудитории: [guides/index.md](./guides/index.md).


| Файл                                                  | Тема                                        |
| ----------------------------------------------------- | ------------------------------------------- |
| [modder-toolkit.md](./guides/modder-toolkit.md)       | Gradle DSL, задачи, JVM, BOM, карта ссылок. |
| [modding-paths.md](./guides/modding-paths.md)         | Три дорожки: API / Vifada / Escultores.     |
| [resolver-policies.md](./guides/resolver-policies.md) | `accessDeniedIds`, исключения резолвера.    |
| [latidos.md](./guides/latidos.md)                     | События, приоритеты, `Ejecutor`.            |
| [catalogo.md](./guides/catalogo.md)                   | Реестры контента.                           |
| [first-entity.md](./guides/first-entity.md)           | Первая сущность, mundo API.                 |
| [ajustes.md](./guides/ajustes.md)                     | Конфиги и профили.                          |
| [vifada.md](./guides/vifada.md)                       | Патчинг байткода на примерах.               |
| [puertas.md](./guides/puertas.md)                     | Wideners.                                   |
| [escultores.md](./guides/escultores.md)               | Низкоуровневые трансформеры.                |
| [hot-reload.md](./guides/hot-reload.md)               | Dev hot-reload, ограничения.                |
| [multi-launcher.md](./guides/multi-launcher.md)       | Инсталлятор и лаунчеры.                     |


---

## Reference (`docs/reference/`)

Сводка раздела: [reference/index.md](./reference/index.md).


| Файл                                                 | Назначение                                               |
| ---------------------------------------------------- | -------------------------------------------------------- |
| [manifest-schema.md](./reference/manifest-schema.md) | Полная схема `vida.mod.json`, включая `vida:dataDriven`. |
| [platform-bom.md](./reference/platform-bom.md)       | Версии артефактов `dev.vida:*`.                          |
| [cli-installer.md](./reference/cli-installer.md)     | Флаги CLI установщика.                                   |
| [api-stability.md](./reference/api-stability.md)     | `@Stable` / `@Preview`, SemVer API.                      |


---

## Миграции и платформы


| Файл                                                               | Назначение                                                       |
| ------------------------------------------------------------------ | ---------------------------------------------------------------- |
| [migration/1.0.0.md](./migration/1.0.0.md)                         | Переход на стабильную линию 1.0.                                 |
| [migration/2.0.0.md](./migration/2.0.0.md)                         | Мажор 2.0 «Масштаб»: Fuente loot, Vifada 2, mundo, телеметрия, … |
| [migration/platform-profiles.md](./migration/platform-profiles.md) | Профили платформы и совместимость.                               |


---

## Безопасность


| Файл                                                   | Назначение                    |
| ------------------------------------------------------ | ----------------------------- |
| [security/telemetry-v1.md](./security/telemetry-v1.md) | Телеметрия opt-in, без PII.   |
| [security/audit-1.0.md](./security/audit-1.0.md)       | Отчёт готовности / аудит 1.0. |


---

## Дорожные карты и история


| Файл                                       | Назначение                                  |
| ------------------------------------------ | ------------------------------------------- |
| [roadmap.md](./roadmap.md)                 | Текущая дорожная карта и выполненные этапы. |
| [session-roadmap.md](./session-roadmap.md) | История сессий разработки до 2.0.           |


---

## Прочее


| Файл                                       | Назначение                                                   |
| ------------------------------------------ | ------------------------------------------------------------ |
| [faq.md](./faq.md)                         | Частые вопросы (MC 1.21.1, Java, агент, лаунчеры, лицензия). |
| [troubleshooting.md](./troubleshooting.md) | Ошибки инсталлятора, резолвера, Vifada, отладка.             |
| [glossary.md](./glossary.md)               | Алфавитный словарь терминов Vida.                            |


---

## Шаблоны

- [templates/starter-mod/README.md](../templates/starter-mod/README.md) — минимальный Gradle-проект с BOM и `VidaMod`.

---

## Про стабильность

Каждый публичный класс помечен `[dev.vida.core.ApiStatus](./reference/api-stability.md)`:

- `**@Stable`** — контракт SemVer для линии после объявления стабильности модуля.
- `**@Preview("name")**` — может меняться; критерии выхода в stable — в [api-stability.md](./reference/api-stability.md).
- `**@Internal**` — не для внешних модов.

В статусе **preview** остаются в первую очередь `**loader`** и `**installer**`; остальные перечисленные выше API-модули в основном `**@Stable**` с 1.0 / 2.0.

В CI: `verifyPlatformProfiles`, полная `build` и `**vidaDocTest**` (компиляция примеров с `package dev.vida.*` из fenced-блоков в `docs/`). Сводный Javadoc — `./gradlew javadocAll` → `build/javadoc-all/`.

Если нашли несоответствие коду — [issue](https://github.com/vidamc/Vida/issues/new/choose) или PR.