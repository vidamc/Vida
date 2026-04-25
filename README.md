<div align="center">

# Vida

**Оригинальный загрузчик модов для Minecraft: Java Edition 1.21.1**

_Быстрый старт. Мощный API. Нулевое влияние на игровой тик._

[![CI](https://github.com/vidamc/Vida/actions/workflows/ci.yml/badge.svg)](https://github.com/vidamc/Vida/actions/workflows/ci.yml)
[![CodeQL](https://github.com/vidamc/Vida/actions/workflows/codeql.yml/badge.svg)](https://github.com/vidamc/Vida/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/vidamc/Vida/badge)](https://securityscorecards.dev/viewer/?uri=github.com/vidamc/Vida)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/java-21%2B-f89820.svg)](https://openjdk.org/projects/jdk/21/)
[![Minecraft 1.21.1](https://img.shields.io/badge/minecraft-1.21.1-62b47a.svg)](#)

[Документация](./docs/index.md) ·
[Установка](./docs/getting-started/installation.md) ·
[Первый мод](./docs/getting-started/first-mod.md) ·
[API](./docs/modules/base.md) ·
[Roadmap](./docs/roadmap.md) ·
[Contributing](./CONTRIBUTING.md) ·

</div>

---

## Зачем Vida

Существующие загрузчики модов делают три вещи примерно одинаково плохо: они долго стартуют, они предлагают API поверх игрового цикла вместо того чтобы уводить работу с него, и они ставят мододелу много вопросов, ответов на которые нет в документации.

Vida построен вокруг трёх инженерных решений, принятых _до_ написания первой строчки:

- **Cold-start ≤ 3.5 с** без модов и **≤ 8 с** с сотней модов. Параллельная дискавери, кэш разобранных манифестов, лёгкий `emitir` шины Latidos (прямые вызовы `Oyente`, без `Method.invoke`).
- **Нулевая работа в hot-path после инициализации.** Загрузчик не вмешивается в game-tick: ни `retransform`, ни тяжёлый event-bus, ни лишняя рефлексия на тик/HUD (платформенный мост — через закэшированные `MethodHandle`; метрики Escultor по наносекундам — только по явному флагу JVM).
- **API, который сам уводит работу с главного потока.** `Susurro` (пул задач), `Latidos Profundos` (events с executor-hint), `Catalogo` (регистрации батчами) — всё спроектировано так, чтобы мододел _по умолчанию_ писал код, который не роняет TPS.

Детальное архитектурное обоснование — в [`docs/architecture/performance.md`](./docs/architecture/performance.md).

## Основные возможности

- **VidaPremain** — тонкий Java-агент без `retransform`, без дублирующей загрузки classpath. [→ архитектура запуска](./docs/architecture/bootstrap.md)
- **Cartografía** — собственный компактный формат `.ctg` + прямой импорт Proguard-маппингов Mojang. [→ docs/modules/cartografia.md](./docs/modules/cartografia.md)
- **Vifada** — байткод-трансформер с аннотациями `@VifadaMorph / @VifadaInject / @VifadaShadow`. [→ docs/guides/vifada.md](./docs/guides/vifada.md)
- **Latidos / Catalogo / Ajustes** — события, реестры, конфиги. Публичный API в `vida-base`. [→ docs/modules/base.md](./docs/modules/base.md)
- **Резолвер зависимостей** — SemVer 2.0.0, NPM-совместимые диапазоны, SAT-бэктрекинг. [→ docs/modules/resolver.md](./docs/modules/resolver.md)
- **Мульти-лаунчер инсталлятор** — Mojang Launcher, Prism, MultiMC, ATLauncher. GUI и CLI. [→ docs/guides/multi-launcher.md](./docs/guides/multi-launcher.md)
- **Gradle-плагин `dev.vida.mod`** — генерация `vida.mod.json`, ремаппинг, `vidaRun`, опционально hot-reload в dev. [→ docs/modules/gradle-plugin.md](./docs/modules/gradle-plugin.md)
- **Data-driven моды** — datapack JSON по `vida:dataDriven` (блоки, предметы, рецепты, loot tables). [→ docs/modules/fuente.md](./docs/modules/fuente.md)
- **Телеметрия v1 (opt-in)** — локальные агрегаты без PII; сеть только при явном будущем включении endpoint. [→ docs/security/telemetry-v1.md](./docs/security/telemetry-v1.md)

## Статус проекта

Платформа следует **SemVer**: линия **1.0.x** закрепила базовые модули (`base`, `bloque`, `objeto`, …); линия **2.0 «Масштаб»** расширила стабильный контур на **entidad**, **mundo**, **render**, **fuente**, **vifada** (включая Vifada 2), ключевые типы **vigia**, а также добавила dev hot-reload, opt-in телеметрию и улучшения инсталлятора. Политика аннотаций — в [`docs/reference/api-stability.md`](./docs/reference/api-stability.md); миграции — [`docs/migration/1.0.0.md`](./docs/migration/1.0.0.md), [`docs/migration/2.0.0.md`](./docs/migration/2.0.0.md).

| # | Компонент | Состояние |
|---|-----------|-----------|
| 1 | `core`, `manifest`, `config`, `cartografia`, `discovery`, `resolver` | ✅ stable |
| 2 | `base`, `bloque`, `objeto`, `susurro`, `puertas`, `red` (Tejido), `escultores`, Gradle plugin `dev.vida.mod` | ✅ stable (1.0+) |
| 3 | `entidad`, `mundo`, `render`, `fuente`, `vifada` | ✅ stable (2.0+) |
| 4 | `vigia` — `/vida profile`, JFR, HTML-отчёт | ✅ stable (основной API, 2.0+) |
| 5 | `loader` — premain, трансформер, Fuente-снимок, dev hot-reload hooks | ⚠️ `@Preview("loader")` |
| 6 | `installer` — GUI + CLI, Mojang / Prism / MultiMC / ATLauncher / Modrinth / CurseForge | ⚠️ `@Preview("installer")` |

## Быстрый старт

### Для игрока

Скачайте установщик с [Releases](https://github.com/vidamc/Vida/releases/latest) и запустите. Артефакты подписаны Sigstore (keyless) и имеют `SHA256SUMS`.

<details>
<summary>Headless CLI — одной командой</summary>

```bash
# стандартный лаунчер Minecraft
java -jar vida-installer.jar --headless --minecraft 1.21.1 -y

# Prism Launcher — создать новый instance
java -jar vida-installer.jar --headless --launcher prism \
  --dir "$APPDATA/PrismLauncher" --minecraft 1.21.1 -y

# ATLauncher — патчить существующий instance
java -jar vida-installer.jar --headless --launcher atlauncher \
  --dir "$USERPROFILE/ATLauncher" \
  --instance "$USERPROFILE/ATLauncher/instances/MyPack" -y

# посмотреть instance'ы выбранного лаунчера
java -jar vida-installer.jar --headless --launcher prism --list-instances
```

Полный список флагов — в [`docs/reference/cli-installer.md`](./docs/reference/cli-installer.md).

</details>

### Для мододела

```kotlin
plugins {
    id("dev.vida.mod") version "0.1.0"
}

vida {
    mod {
        id.set("miaventura")
        displayName.set("Mi Aventura")
        entrypoint.set("com.ejemplo.MiAventura")
    }
    minecraft {
        version.set("1.21.1")
    }
}
```

Дальше — [Создание первого мода](./docs/getting-started/first-mod.md) за 10 минут.

### Для контрибьютора

```bash
git clone https://github.com/vidamc/Vida.git
cd Vida
./gradlew verifyPlatformProfiles build vidaDocTest
```

Полный цикл, как в CI, при необходимости с чистой пересборкой: `./gradlew clean build verifyPlatformProfiles vidaDocTest javadocAll` (сводный Javadoc — `build/javadoc-all/`).

Требования: **JDK 21+**, **Gradle 9.x** (через встроенный `gradlew`), **Git 2.40+**. Подробнее — [`docs/getting-started/dev-environment.md`](./docs/getting-started/dev-environment.md).

## Документация

Источник истины — каталог [`docs/`](./docs/index.md): на [**главной странице документации**](docs/index.md) есть **полный каталог** всех страниц (getting started, архитектура, модули, guides, reference, миграции, безопасность). Краткие справочники собраны в [`docs/reference/index.md`](docs/reference/index.md); практические гайды — в [`docs/guides/index.md`](docs/guides/index.md).

Aggregated Javadoc: `./gradlew javadocAll` → `build/javadoc-all/`. Примеры из markdown проверяются в CI: `./gradlew vidaDocTest`.

- [Установка для игрока](./docs/getting-started/installation.md)
- [Dev-окружение](./docs/getting-started/dev-environment.md)
- [Первый мод](./docs/getting-started/first-mod.md)
- [Архитектура: overview](./docs/architecture/overview.md), [bootstrap](./docs/architecture/bootstrap.md), [classloading](./docs/architecture/classloading.md), [lifecycle](./docs/architecture/lifecycle.md), [performance](./docs/architecture/performance.md)
- [Все модули](./docs/modules/index.md) — Javadoc-подобные страницы на каждый подпроект
- [Guides](./docs/guides/index.md) — Latidos, Catalogo, Ajustes, Vifada, Escultores, мульти-лаунчер
- [Reference](./docs/reference/index.md) — `vida.mod.json` schema, CLI installer, стабильность API
- [FAQ](./docs/faq.md) · [Troubleshooting](./docs/troubleshooting.md) · [Глоссарий](./docs/glossary.md)

## Словарь проекта

Все подсистемы и API имеют собственные имена, не заимствованные у других загрузчиков:

| Подсистема | Имя Vida |
|---|---|
| Маппинги                       | **Cartografía** |
| Модификация байткода           | **Vifada** (`@VifadaMorph`, `@VifadaInject`, `@VifadaShadow`, …) |
| Расширение доступа             | **Puertas** |
| Низкоуровневые трансформеры    | **Escultores** |
| События                        | **Latidos** |
| Реестры контента               | **Catalogo** |
| Сеть                           | **Tejido** |
| Ресурсы и данные               | **Fuente** |
| Конфиги модов                  | **Ajustes** |
| Асинхронные задачи             | **Susurro** |
| Потоко-осознанные события      | **Latidos Profundos** |
| Профайлер                      | **Vigia** |
| Ранний bootstrap / Java Agent  | **VidaPremain** |

Полный [глоссарий](./docs/glossary.md) — с контекстом и ссылками на пакеты.

## Структура монорепо

```
vida/
  build-logic/           convention-плагины Gradle
  core/                  базовые типы (Identifier, SemVer, Result, Log, ApiStatus)
  manifest/              vida.mod.json + парсер
  config/                Ajustes: TOML + профили
  cartografia/           маппинги (Cartografía)
  discovery/             обнаружение и чтение модов
  resolver/              резолвер зависимостей
  vifada/                модификация байткода
  fuente/                data-driven JSON из datapack (dev.vida.fuente)
  escultores/            Escultor API + BrandingEscultor
  loader/                VidaPremain + TransformingClassLoader + BootSequence
  base/                  публичный API моддинга (Latidos, Catalogo, Ajustes)
  gradle-plugin/         плагин dev.vida.mod
  installer/             GUI + CLI установщик (Mojang/Prism/MultiMC/ATLauncher)
  docs/                  документация
  mods/                  опциональные composite-подпроекты (только если каталоги заданы в `settings.gradle.kts`)
  .github/               CI/CD, issue/PR шаблоны
```

## Вклад

Контрибьюции приветствуются. Прежде чем открывать PR, прочитайте [`CONTRIBUTING.md`](./CONTRIBUTING.md) и [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md). Для нетривиальных изменений сначала заведите [issue](https://github.com/vidamc/Vida/issues/new/choose) — обсудим направление.

- **Баг-репорты и feature-requests** — через [GitHub Issues](https://github.com/vidamc/Vida/issues/new/choose).
- **Безопасность** — приватно, через [Security Advisory](https://github.com/vidamc/Vida/security/advisories/new). Политика — [`SECURITY.md`](./SECURITY.md).
- **Обсуждения** — [GitHub Discussions](https://github.com/vidamc/Vida/discussions).

## Лицензия

Apache License 2.0 — см. [`LICENSE`](./LICENSE).

Проект разработан с нуля и не содержит кода из сторонних систем моддинга. Все имена, форматы и архитектурные решения — оригинальные.
