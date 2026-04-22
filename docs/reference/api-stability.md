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

### `@ApiStatus.Internal`

Не используйте вне загрузчика. Правила:

- API может измениться в любом патче.
- Если нашли, что без него не обходится — откройте issue, мы либо обнародуем, либо предложим альтернативу.

## Как мы это применяем

- Пакеты `dev.vida.*.internal` — полностью `@Internal`; аннотация на `package-info.java`.
- `dev.vida.core.*` — `@Stable`; точечные исключения помечены на классе.
- `dev.vida.vifada.*`, `dev.vida.loader.*`, `dev.vida.base.*` — `@Preview("<name>")` до 1.0.0.
- `dev.vida.bloque.*`, `dev.vida.objeto.*`, `dev.vida.susurro.*`, `dev.vida.puertas.*` — `@Preview("<name>")` с 0.3.0, стабилизируются по мере обкатки (см. [roadmap](../roadmap.md), [session-roadmap](../session-roadmap.md)).
- `dev.vida.entidad.*`, `dev.vida.mundo.*` — `@Preview("entidad")` / `@Preview("mundo")` с 0.5.0; стабилизация пойдёт после появления первых продакшн-модов и world bridge'ей.
- `dev.vida.render.*`, `dev.vida.red.*` — `@Preview("render")` / `@Preview("red")` с 0.6.0.
- `@VifadaMulti` / `@VifadaLocal` — отдельный preview-тег `@Preview("vifada-next")` для будущей ветки расширений Vifada.
- Записи `record` и `sealed`-иерархии — первоклассные члены стабильного API; изменение компонентов `record` или набора пермитов `sealed` — breaking.

### Preview-модули 0.3.0 — явный reminder

| Модуль | Preview-тег | Что стабилизируем к 1.0 |
|--------|-------------|--------------------------|
| `bloque` | `@Preview("bloque")` | `Bloque`, `PropiedadesBloque`, `FormaColision`, `RegistroBloques` — после двух минорных релизов с несколькими продакшн-модами. |
| `objeto` | `@Preview("objeto")` | `Objeto`, `PropiedadesObjeto`, `MapaComponentes` и набор стандартных `Componente` — вместе с `bloque`. |
| `susurro` | `@Preview("susurro")` | `Susurro`, `Tarea`, `Prioridad`, `Etiqueta`, `HiloPrincipal` — после 0.5.0 (когда `HiloPrincipal` получит полноценную реализацию в клиенте/сервере MC). |
| `puertas` | `@Preview("puertas")` | Формат `.ptr` v1, `Accion`, `Objetivo`, `Namespace`, `PuertaError` — формат файла стабилизируется раньше API (сам формат — первый кандидат на `@Stable`). |
| `base.latidos` (`Ejecutor`) | `@Preview("base")` | `Ejecutor` + фабрики (`hiloPrincipal`, `susurro`, `serializado`) стабилизируются вместе с `susurro`. |

### Preview-модули 0.5.0

| Модуль | Preview-тег | Что стабилизируем к 1.0 |
|--------|-------------|--------------------------|
| `entidad` | `@Preview("entidad")` | `Entidad`, `TipoEntidad`, `PropiedadesEntidad`, entity data-components и `RegistroEntidades` — после реальных модов, которые докажут shape API. |
| `mundo` | `@Preview("mundo")` | `Mundo`, `Coordenada`, `Dimension`, `Bioma`, `LatidosMundo` и `@OyenteDeTick` — после того как runtime bridge и world events пройдут хотя бы один игровой цикл без breaking-дыр. |

### Preview-модули 0.6.0

| Модуль/фича | Preview-тег | Что стабилизируем к 1.0 |
|-------------|-------------|--------------------------|
| `render` | `@Preview("render")` | `ModeloBloque`, `ModeloEntidad`, `TextureAtlas`, `RenderPipeline`, shader-hooks. |
| `red` | `@Preview("red")` | `PaqueteCliente/Servidor`, `TejidoCanal`, `CodecPaquete`, auto-serialización record и versioning-политика. |
| Vifada extensions | `@Preview("vifada-next")` | `@VifadaMulti`, `@VifadaLocal` и их runtime-реализация в transformer'е. |

Правило: любой breaking change в preview-модуле обязан быть отмечен в `CHANGELOG.md` с указанием тега (`[preview/susurro]`, `[preview/puertas]` и т.д.) и, где возможно, сопровождаться migration note.

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
