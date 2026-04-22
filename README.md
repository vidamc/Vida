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

- **Cold-start ≤ 3.5 с** без модов и **≤ 8 с** с сотней модов. Параллельная дискавери, кэш разобранных манифестов, `LambdaMetafactory`-генерация диспетчеров.
- **Нулевая работа в hot-path после инициализации.** Загрузчик не вмешивается в game-tick: ни `retransform`, ни allocation-heavy event-буса, ни scan-on-hit рефлексии.
- **API, который сам уводит работу с главного потока.** `Susurro` (пул задач), `Latidos Profundos` (events с executor-hint), `Catalogo` (регистрации батчами) — всё спроектировано так, чтобы мододел _по умолчанию_ писал код, который не роняет TPS.

Детальное архитектурное обоснование — в [`vida.md`](./vida.md) и в [`docs/architecture/performance.md`](./docs/architecture/performance.md).

## Основные возможности

- **VidaPremain** — тонкий Java-агент без `retransform`, без дублирующей загрузки classpath. [→ архитектура запуска](./docs/architecture/bootstrap.md)
- **Cartografía** — собственный компактный формат `.ctg` + прямой импорт Proguard-маппингов Mojang. [→ docs/modules/cartografia.md](./docs/modules/cartografia.md)
- **Vifada** — байткод-трансформер с аннотациями `@VifadaMorph / @VifadaInject / @VifadaShadow`. [→ docs/guides/vifada.md](./docs/guides/vifada.md)
- **Latidos / Catalogo / Ajustes** — события, реестры, конфиги. Публичный API в `vida-base`. [→ docs/modules/base.md](./docs/modules/base.md)
- **Резолвер зависимостей** — SemVer 2.0.0, NPM-совместимые диапазоны, SAT-бэктрекинг. [→ docs/modules/resolver.md](./docs/modules/resolver.md)
- **Мульти-лаунчер инсталлятор** — Mojang Launcher, Prism, MultiMC, ATLauncher. GUI и CLI. [→ docs/guides/multi-launcher.md](./docs/guides/multi-launcher.md)
- **Gradle-плагин `dev.vida.mod`** — генерация `vida.mod.json`, ремаппинг, dev-run. [→ docs/modules/gradle-plugin.md](./docs/modules/gradle-plugin.md)

## Статус проекта

Vida находится в стадии **preview** — базовая инфраструктура работает, публичный API фиксируется. Semantic Versioning соблюдается с 1.0.0; до тех пор допустимы breaking changes в API, помеченном `@ApiStatus.Preview`.

| # | Компонент | Состояние |
|---|-----------|-----------|
| 1 | `core` — Identifier, SemVer, Result, Log, ApiStatus | ✅ stable |
| 2 | `manifest` — `vida.mod.json` и парсер | ✅ stable |
| 3 | `config` — Ajustes, TOML, профили | ✅ stable |
| 4 | `cartografia` — маппинги, `.ctg`, ASM-ремаппер | ✅ stable |
| 5 | `discovery` — сканер модов, nested-jar, кэш | ✅ stable |
| 6 | `resolver` — резолвер зависимостей | ✅ stable |
| 7 | `vifada` — `@VifadaMorph / @Inject / @Shadow` | ✅ preview |
| 8 | `loader` — VidaPremain, TransformingClassLoader, BrandingEscultor | ✅ preview |
| 9 | `base` — Latidos, Catalogo, Ajustes, VidaMod | ✅ preview |
| 10 | `gradle-plugin` — `dev.vida.mod` | ✅ preview |
| 11 | `installer` — GUI + CLI, Mojang / Prism / MultiMC / ATLauncher | ✅ preview |
| 12 | `bloque` — публичный API для блоков (`Bloque`, `PropiedadesBloque`, `FormaColision`, `RegistroBloques`) | ✅ preview |
| 13 | `objeto` — публичный API для предметов + data-components 1.21.1 | ✅ preview |
| 14 | `susurro` — managed thread-pool, `Tarea`, `HiloPrincipal` | ✅ preview |
| 15 | `puertas` — access-wideners, `.ptr` parser + ASM applier | ✅ preview |
| 16 | Latidos profundos — `Ejecutor`, `@EjecutorLatido` в `base` | ✅ preview |
| 17 | `entidad` + `mundo` — API сущностей/мира | ✅ preview (0.5.0) |
| 18 | `render` + `red` — render/network API | ✅ preview (0.6.0) |
| 19 | Modrinth App / CurseForge в инсталляторе | ✅ preview (0.4.0) |
| 20 | `vigia` — встроенный профайлер | ✅ preview (0.4.0) |

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
./gradlew build
./gradlew test
```

Требования: **JDK 21+**, **Gradle 9.x** (через встроенный `gradlew`), **Git 2.40+**. Подробнее — [`docs/getting-started/dev-environment.md`](./docs/getting-started/dev-environment.md).

## Документация

Полная документация живёт в [`docs/`](./docs/index.md). Aggregated Javadoc собирается `./gradlew javadocAll` → `build/javadoc-all/`.

- [Установка для игрока](./docs/getting-started/installation.md)
- [Dev-окружение](./docs/getting-started/dev-environment.md)
- [Первый мод](./docs/getting-started/first-mod.md)
- [Архитектура: overview](./docs/architecture/overview.md), [bootstrap](./docs/architecture/bootstrap.md), [classloading](./docs/architecture/classloading.md), [lifecycle](./docs/architecture/lifecycle.md), [performance](./docs/architecture/performance.md)
- [Все модули](./docs/modules/index.md) — Javadoc-подобные страницы на каждый подпроект
- [Guides](./docs/guides/index.md) — Latidos, Catalogo, Ajustes, Vifada, Escultores, мульти-лаунчер
- [Reference](./docs/reference/index.md) — `vida.mod.json` schema, CLI installer, стабильность API
- [FAQ](./docs/faq.md) · [Troubleshooting](./docs/troubleshooting.md) · [Глоссарий](./docs/glossary.md)

Техническая спецификация, на которой стоит проект, — [`vida.md`](./vida.md).

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
  loader/                VidaPremain + TransformingClassLoader + BrandingEscultor
  base/                  публичный API моддинга (Latidos, Catalogo, Ajustes)
  gradle-plugin/         плагин dev.vida.mod
  installer/             GUI + CLI установщик (Mojang/Prism/MultiMC/ATLauncher)
  docs/                  документация
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
