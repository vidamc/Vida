# API stability

Политика версионирования публичного API Vida и правила использования аннотаций `@ApiStatus`.

## Три уровня стабильности

Каждый публичный класс Vida аннотирован одним из:

### `@ApiStatus.Stable`

Зафиксированный API. Правила:

- Breaking changes — только в мажорной версии (`1.x → 2.0`).
- Новые методы в интерфейсах → только как `default`-методы (не требующие реализации).
- Удаление или переименование публичных классов/методов — запрещено в минорных версиях.
- Удаление deprecation'ов — не раньше чем через **две минорные версии** после его ввода (если был помечен `@Deprecated(forRemoval = true)`), и не раньше **одной мажорной** (если не `forRemoval`).

Формально `Stable` означает: мы обещаем бинарную совместимость между `1.x` и `1.y`, и source-совместимость.

### `@ApiStatus.Preview("<name>")`

API, который мы обкатываем. Правила:

- Breaking changes допустимы между любыми релизами до `1.0.0`.
- Каждый breaking change обязан быть задокументирован в `CHANGELOG.md` с указанием `<name>`-тега.
- Мы стараемся предоставить migration guide; но обещать не можем.

`<name>` — тег feature-set'а: `base`, `vifada`, `loader`, `susurro`, etc. Это позволяет нам «стабилизировать» части API независимо. Например, `@Preview("base")` может стать `@Stable` до того, как `@Preview("vifada")`.

### Профили платформы {#platform-profiles-stability}

Файлы `platform-profiles/**/profile.json`, типы `PlatformProfileDescriptor`, выбор профиля в агенте и поля `custom.vida.platformProfileIds` в манифесте — **не** часть стабильного контракта мода Modrinth/CurseForge-уровня. Мы можем расширять схему профиля и правила валидации в минорных версиях Vida.

Стабильными остаются: формат `vida.mod.json` (обязательные группы полей), SemVer зависимостей и способ **указать** версию игры / профиль через JVM и Gradle (см. [modules/platform-profiles.md](../modules/platform-profiles.md)).

### `@ApiStatus.Internal`

Не используйте вне загрузчика. Правила:

- API может измениться в любом патче.
- Если нашли, что без него не обходится — откройте issue, мы либо обнародуем, либо предложим альтернативу.

## Как мы это применяем

- Пакеты `dev.vida.*.internal` — полностью `@Internal`; аннотация на `package-info.java`.
- `dev.vida.core.*` — `@Stable`; точечные исключения помечены на классе.
- `dev.vida.loader.*` — **смешанный контракт**: публичная поверхность бутстрапа (`VidaBoot`, `BootOptions`, `BootReport`, `VidaRuntime`, `VidaEnvironment`, `LoaderError`) помечена **`@Stable`**; агент, трансформер, иерархия лоадеров и профили платформы остаются **`@Preview("loader")`** до полной заморозки байткод-политики.
- `dev.vida.base.*`, `dev.vida.bloque.*`, `dev.vida.objeto.*`, `dev.vida.susurro.*`, `dev.vida.puertas.*`, публичный API `dev.vida.gradle.*` — **`@Stable` с 1.0.0** (контракт SemVer между `1.x`, см. `CHANGELOG`).
- `dev.vida.entidad.*`, `dev.vida.mundo.*`, `dev.vida.render.*`, `dev.vida.fuente.*`, `dev.vida.vifada.*` — **`@Stable` с 2.0 «Масштаб»** (публичные пакеты без `Preview`; см. [migration/2.0.0.md](../migration/2.0.0.md)).
- Основные типы **`dev.vida.vigia.*`** (`VigiaSesion`, `Resumen`, `VigiaReporte`, …) — **`@Stable`**.
- `dev.vida.red.*` — **`@Stable`** (Tejido).
- `dev.vida.escultores.*` — **`@Stable`** (`Escultor`, `BrandingEscultor`; см. [modules/escultores.md](../modules/escultores.md)).
- Аннотации **Vifada 2** — `@VifadaMulti`, `@VifadaLocal`, `@VifadaRedirect` входят в стабильный контракт `dev.vida.vifada` (без отдельного тега `vifada-next`).
- Записи `record` и `sealed`-иерархии — первоклассные члены стабильного API; изменение компонентов `record` или набора пермитов `sealed` — breaking.

### Стабилизация 1.0.0

| Модуль | Статус с 1.0.0 | Комментарий |
|--------|-----------------|-------------|
| `base`, `bloque`, `objeto`, `susurro`, `puertas`, **`red`** (Tejido), **`escultores`** | `@Stable` | Обещание SemVer для публичных типов; см. [migration/1.0.0.md](../migration/1.0.0.md). |
| Gradle plugin `dev.vida.gradle`, `dev.vida.gradle.tasks` | `@Stable` | Внутренности `dev.vida.gradle.internal` — `@Internal`. |

### Стабилизация 2.0 «Масштаб»

| Модуль / область | Статус | Комментарий |
|------------------|--------|-------------|
| `entidad`, `mundo`, `render`, `fuente`, `vifada` | `@Stable` | См. [roadmap/2.0-plan.md](../roadmap/2.0-plan.md), [migration/2.0.0.md](../migration/2.0.0.md). |
| `vigia` (основной публичный API) | `@Stable` | Форматы отчётов согласованы с тестами; при изменении HTML/JFR — запись в `CHANGELOG.md`. |
| `loader` | См. выше: **Stable** boot API + **Preview** internals | GA по загрузчику = снятие превью с оставшихся публичных типов (`VidaPremain`, `MorphIndex`, …) после заморозки agent/bytecode. |
| `installer` | **`InstallerCore` — `@Stable`**; CLI/GUI и отдельные handlers могут оставаться в превью до стабилизации лаунчер-каталога. |

Примеры кода в `docs/`, оформленные как полные compilation unit с `package dev.vida....`, дополнительно проверяются задачей `./gradlew vidaDocTest` (компиляция извлечённых файлов).

### Оставшиеся Preview-области

| Область | Preview-тег | Критерии перехода в `@Stable` |
|---------|---------------|-------------------------------|
| `loader` (internals) | `@Preview("loader")` | `VidaPremain`, `VidaClassTransformer`, `JuegoLoader`, `ModLoader`, `MorphIndex`, типы `profile.*` — до заморозки agent + bytecode на целевой MC; строгие IT; `vidaDocTest` зелёный. |
| `installer` (кроме ядра) | `@Preview("installer")` | Handlers лаунчеров и толстый CLI — без ломки в двух минорах; сценарии Mojang/Prism на целевых ОС ([modules/installer.md](../modules/installer.md)). |

Исторические таблицы preview для 0.5.x / 0.6.x заменены выпуском **2.0**: `entidad`, `mundo`, `render`, `fuente`, `vifada` (включая Vifada 2), ключевые типы `vigia` переведены на `@Stable`.

Правило: любой breaking change в оставшихся `@Preview`-типах обязан быть отмечен в `CHANGELOG.md` с указанием тега и, где возможно, migration note.

## Semantic Versioning

Vida следует SemVer 2.0.0:

- `X.Y.Z`:
  - `X` — breaking changes в `@Stable` API.
  - `Y` — новые фичи, backward-compatible.
  - `Z` — исправления, backward-compatible.
- pre-release: `0.1.0-alpha.1`, `0.1.0-rc.2`.
- build metadata: `0.1.0+build.42` (игнорируется при сравнении версий).

До `1.0.0` любые изменения, включая breaking — допустимы в минорных (соответствует SemVer §4).

## Conventional Commits

Источник правды о типе релиза — сообщения коммитов. `release-please` автоматически рассчитывает bump:

| Коммит | Bump |
|--------|------|
| `feat:` | MINOR |
| `fix:` | PATCH |
| `perf:` | PATCH |
| `feat!:` или `BREAKING CHANGE:` в теле | MAJOR |
| `docs:`, `chore:`, `ci:`, `refactor:`, `test:`, `build:` | нет bump'а |

Подробности — [`CONTRIBUTING.md`](../../CONTRIBUTING.md#conventional-commits).

## Поддержка версий Minecraft

- **Активная поддержка** — текущая версия Minecraft (сейчас 1.21.1) + предыдущая major (планируется).
- **Порт на новую версию Minecraft** — начинается после публикации Mojang-маппингов. Цель — рабочая сборка в течение 14 дней после релиза (не снапшота).
- **Обратная совместимость мод↔загрузчик**: если мод не обращается к удалённым/переименованным vanilla-символам, он работает без перекомпиляции между версиями Minecraft. Vifada продолжает применять морфы, если target-методы существуют.
- **Breaking Minecraft updates** (радикальная смена vanilla API в игре): мод-автор должен выпустить новую версию. Vida не обеспечивает «magic» совместимости — это противоречило бы принципу производительности.

Стратегия обновлений:

1. Mojang-маппинги новой версии попадают в `cartografia`.
2. Игроки, чьи моды совместимы (по `dependencies.required.minecraft`), получают рабочую сборку на день выхода Vida для новой версии.
3. Несовместимые моды показываются в `DiscoveryReport` с пометкой `IncompatibleMinecraft` — игрок видит ровно кому из его модов нужно обновиться.

## Совместимость с другими экосистемами

Vida — самодостаточный загрузчик. Слоя совместимости с модами других систем нет и не будет:

- Это означает компромиссы по производительности (что противоречит цели проекта).
- Это требует постоянного реверс-инжиниринга чужого API (что хрупко и отнимает ресурсы).
- Это размывает контракт Vida — часть модов работает «хорошо», часть «почти».

Авторы модов переносят код точечно. Публичный API (`vida-base`, будущие `vida-*`) покрывает все типовые задачи.

## Deprecation lifecycle

Если класс/метод должен быть удалён:

1. Помечаем `@Deprecated` с Javadoc-указанием альтернативы.
2. В `CHANGELOG.md` — запись с причиной и миграционным путём.
3. Через две минорные версии (или одну мажорную) — удаляем.

Для `@Preview`-API этот цикл может быть сокращён до одной минорной — breaking changes в preview формально разрешены.

## Бинарная vs source-совместимость

- `@Stable` гарантирует **обе**: старый мод без перекомпиляции работает с новым загрузчиком, и старый исходный код компилируется.
- `@Preview` — **ни одна** не гарантирована; на практике мы стараемся поддерживать source-совместимость между минорными релизами.

## Аудит

Javadoc-taglet `vida-since` — когда класс/метод появился (версия Vida). Используется для автоматической генерации миграционных guide'ов:

```java
/**
 * ...
 * @vida-since 0.1.0
 */
```

Аудит применения аннотаций стабильности и `@Deprecated` — часть CI (`:dev-tools:api-diff`, в разработке).

## См. также

- [`CONTRIBUTING.md`](../../CONTRIBUTING.md#conventional-commits) — как коммиты определяют релизы.
- [roadmap](../roadmap.md#10) — критерии выхода на 1.0.
- [`SECURITY.md`](../../SECURITY.md) — политика поддержки безопасностью (пересекается со стабильностью).
