# Contributing to Vida

Спасибо за интерес к проекту! Этот документ описывает ожидания от контрибьюций и правила работы с кодовой базой.

Если вы впервые здесь — сначала прочитайте [`docs/index.md`](./docs/index.md) и, в зависимости от задачи:

- [Dev-окружение](./docs/getting-started/dev-environment.md) — как собрать Vida локально.
- [Архитектура](./docs/architecture/overview.md) — как устроен код.
- [API stability](./docs/reference/api-stability.md) — политика `@Stable` / `@Preview` / `@Internal`.

## Поток работы

1. Откройте issue для обсуждения нетривиальных изменений (API, поведение, производительность). Для мелких багфиксов достаточно PR.
2. Форкните репозиторий и создайте ветку `feat/<area>-<slug>` или `fix/<area>-<slug>`.
3. Перед коммитом убедитесь, что проходят команды, как в CI: `./gradlew verifyPlatformProfiles build vidaDocTest` (при смене примеров кода в `docs/` — обязателен `vidaDocTest`). При полной проверке локально: `./gradlew javadocAll` (агрегированный Javadoc в `build/javadoc-all/`).
4. Оформите PR (используется [шаблон](./.github/PULL_REQUEST_TEMPLATE.md)) и пройдите CI.

## Conventional Commits

Сообщения коммитов обязаны соответствовать [Conventional Commits 1.0](https://www.conventionalcommits.org/). Это то, на основе чего [release-please](https://github.com/googleapis/release-please) считает версии и формирует `CHANGELOG.md`.

| Префикс | Bump версии | Назначение |
|---|---|---|
| `feat:` | MINOR | новая функциональность |
| `fix:` | PATCH | багфикс |
| `perf:` | PATCH | ускорение |
| `refactor:` | — | рефакторинг без изменения поведения |
| `docs:` | — | документация |
| `test:` | — | тесты |
| `chore:` | — | инфраструктура, зависимости |
| `ci:` | — | CI/CD |
| `build:` | — | Gradle/сборка |
| `feat!:` или `BREAKING CHANGE:` в теле | MAJOR | breaking API |

Примеры:

```
feat(vifada): добавлена аннотация @VifadaConstant
fix(resolver): корректный выбор версии при совпадении ранжей
perf(latidos): инлайн диспетчера для событий с <=4 обработчиками
docs(installer): примеры CLI для ATLauncher
```

## Область коммита (scope)

Используйте имя модуля без префикса `vida-`: `core`, `manifest`, `config`, `cartografia`, `discovery`, `resolver`, `vifada`, `puertas`, `escultores`, `loader`, `base`, `bloque`, `objeto`, `entidad`, `mundo`, `render`, `red`, `susurro`, `vigia`, `gradle-plugin`, `installer`, `docs`, `ci`.

## Стиль кода

- Java 21, **никаких** зависимостей от preview-фич без явного согласования.
- Отступы 4 пробела (EditorConfig настроен).
- `@ApiStatus.Stable` / `@ApiStatus.Preview("<name>")` / `@ApiStatus.Internal` проставляются на каждом публичном пакете в `package-info.java`. Правила — [reference/api-stability.md](./docs/reference/api-stability.md).
- Не использовать `System.out` / `System.err` в production-коде — только `dev.vida.core.Log`.
- Никакой рефлексии на hot path — `MethodHandle` / сгенерированный байткод.
- Никаких названий / концепций из сторонних систем моддинга в публичных именах (см. [глоссарий](./docs/glossary.md)).

## Тесты

- JUnit 5 (Jupiter) — основной фреймворк.
- Тест-класс `X` называется `XTest`; интеграционные — `XIT`; бенчмарки (JMH, отдельный sourceSet) — `XBench`.
- Все PR, вносящие логику, должны либо иметь тесты, либо содержать обоснование их отсутствия в описании PR.
- Для property-based тестов — jqwik.

## Документация

Полный перечень страниц — в [`docs/index.md`](./docs/index.md) (каталог по разделам). Краткие сводки: [`docs/reference/index.md`](./docs/reference/index.md), [`docs/guides/index.md`](./docs/guides/index.md).

Если вы меняете публичное поведение, **обновите `docs/`**:

- Добавили API — страница в `docs/modules/<module>.md` + при необходимости `docs/guides/`.
- Изменили CLI-флаг — обновите [`docs/reference/cli-installer.md`](./docs/reference/cli-installer.md).
- Добавили поле манифеста — [`docs/reference/manifest-schema.md`](./docs/reference/manifest-schema.md).
- Добавили термин — впишите в [`docs/glossary.md`](./docs/glossary.md).
- Breaking change — запись в `CHANGELOG.md` и упоминание в описании PR.

Примеры Java в markdown: при полном пакете `package dev.vida....` они участвуют в **`./gradlew vidaDocTest`**. После правок доков с кодом запускайте эту задачу локально.

Javadoc на публичных API обязателен; для `@Internal` — желателен, но не блокирует PR.

## Code review

- Требуется минимум 1 одобрение; для `core/`, `loader/`, `vifada/`, `gradle-plugin/` — два.
- CODEOWNERS автоматически назначит ревьюеров.
- CI обязательна зелёная перед merge.

## Безопасность

Сообщайте об уязвимостях приватно — см. [`SECURITY.md`](./SECURITY.md).

## Код поведения

Проект принял [Contributor Covenant 2.1](./CODE_OF_CONDUCT.md). Будьте добрыми друг к другу.
