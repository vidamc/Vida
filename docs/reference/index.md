# Reference

Справочники **без длинного нарратива**: точные правила синтаксиса, схемы, флаги CLI, политика версий. Для обзора всего дерева `docs/` см. [главную страницу документации](../index.md).

## Схемы и контракты

| Страница | Что внутри |
|----------|------------|
| [`manifest-schema.md`](./manifest-schema.md) | Полное описание полей `vida.mod.json`: `entrypoints`, `dependencies`, `vifada`, `puertas`, `escultores`, вложенный **`custom`** и ключ **`vida:dataDriven`** (datapack root, Fuente). |
| [`platform-bom.md`](./platform-bom.md) | BOM `dev.vida:vida-bom`: как зафиксировать одну версию всех модулей `dev.vida:*` в Gradle/Maven. |
| [`api-stability.md`](./api-stability.md) | `@Stable` / `@Preview` / `@Internal`, SemVer для публичного API, поддержка версий Minecraft, совместимость с другими экосистемами (нет). |
| [`tz-compliance-matrix.md`](./tz-compliance-matrix.md) | Построчное соответствие корневого `vida.md` коду и тестам; процесс обновления при релизе. |
| [`domain-gap.md`](./domain-gap.md) | Доменные области игры: что уже в контуре Vida, что планируется, что явно вне минимального API. |

## Инструменты и CLI

| Страница | Что внутри |
|----------|------------|
| [`cli-installer.md`](./cli-installer.md) | Все флаги headless/GUI установщика: `--launcher`, `--minecraft`, `--dir`, `--download`, коды выхода. |

## Миграции между мажорами

| Страница | Аудитория |
|----------|-----------|
| [Миграция 1.0.0](../migration/1.0.0.md) | Переход на первую стабильную линию API. |
| [Миграция 2.0.0](../migration/2.0.0.md) | Мажор «Масштаб»: Vifada 2, Fuente loot/worldgen, mundo, телеметрия, hot-reload — краткий чеклист. |
| [Профили платформы](../migration/platform-profiles.md) | Поколения MC и `platform-profiles/`. |

## Безопасность и наблюдаемость

| Страница | Что внутри |
|----------|------------|
| [Телеметрия v1 (opt-in)](../security/telemetry-v1.md) | Формат агрегатов, отсутствие PII, включение только явным согласием. |

## Связка с кодом и CI

- **`vidaDocTest`** (`./gradlew vidaDocTest`) — компилирует Java-примеры из `docs/**/*.md`, если в fenced-блоке указан полный `package dev.vida....`.
- Глоссарий терминов проекта — [../glossary.md](../glossary.md).
