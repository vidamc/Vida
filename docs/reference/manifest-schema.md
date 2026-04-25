# `vida.mod.json` — полная схема

Каждый мод Vida обязан положить в корень JAR файл `vida.mod.json`. Ниже — полное описание полей; обязательные помечены `REQ`.

## Минимальный пример

```json
{
  "schema": 1,
  "id": "miaventura",
  "version": "0.1.0",
  "name": "Mi Aventura"
}
```

## Полный пример

```json
{
  "schema": 1,
  "id": "miaventura",
  "version": "1.2.3",
  "name": "Mi Aventura",
  "description": "Пример мода на Vida",
  "authors": [
    { "name": "Ana", "email": "ana@example.com", "url": "https://ana.example.com" },
    { "name": "Beto" }
  ],
  "license": "MIT",
  "entrypoints": {
    "main":       "com.ejemplo.MiAventura",
    "client":     "com.ejemplo.client.MiAventuraClient",
    "server":     "com.ejemplo.server.MiAventuraServer",
    "preLaunch":  "com.ejemplo.PreLaunch"
  },
  "dependencies": {
    "required": {
      "vida": "^0.1.0",
      "minecraft": "1.21.1"
    },
    "optional": {
      "cool-library": "^2.0.0"
    },
    "incompatibilities": {
      "broken-mod": "*"
    }
  },
  "provides": [
    "generic-dungeon-api@1.0.0"
  ],
  "vifada": {
    "morphs": [
      "com.ejemplo.morphs.DebugScreenMorph",
      "com.ejemplo.morphs.ServerLevelMorph"
    ]
  },
  "puertas": [
    "puertas/miaventura.puertas.txt"
  ],
  "modules": [
    "META-INF/jars/miaventura-api.jar"
  ],
  "escultores": [
    "com.ejemplo.escultores.PrioridadCero",
    { "class": "com.ejemplo.escultores.PrioridadAlta", "priority": -10 }
  ],
  "incompatibilities": [
    "some-other-mod"
  ],
  "custom": {
    "my-tool:icon": "assets/miaventura/icon.png",
    "vida:dataDriven": {
      "enabled": true,
      "datapackRoot": "data/miaventura/vida"
    }
  }
}
```

## Поля

### `schema` — REQ, integer

Версия формата манифеста. Поддерживается только `1` на момент этой документации. Vida игнорирует манифесты с неизвестной `schema` и пишет `WARN`.

### `id` — REQ, string

Уникальный идентификатор мода. Регулярка: `[a-z0-9_.-]+`. Без двоеточия (`:` зарезервирован для полного `Identifier`).

### `version` — REQ, string

Версия мода по SemVer 2.0.0 (`MAJOR.MINOR.PATCH[-pre][+build]`). Примеры:

- `1.0.0`
- `0.1.0-alpha.1`
- `2.3.4-rc.2+build.42`

### `name` — REQ, string

Человекочитаемое имя мода. Показывается в UI лаунчеров и `/mods`. Может содержать пробелы и non-ASCII.

### `description` — optional, string

Короткое описание (до 512 символов). Plain-text, без Markdown.

### `authors` — optional, array of author

```json
{ "name": "Ana", "email": "ana@example.com", "url": "https://ana.example.com" }
```

Обязательно `name`; `email` и `url` — опциональны. Порядок сохраняется.

### `license` — optional, string

SPDX-идентификатор (`MIT`, `Apache-2.0`, `GPL-3.0-or-later`). Свободная строка тоже допустима, но SPDX предпочтителен для инструментов.

### `entrypoints` — optional, object

| Ключ | Что |
|------|-----|
| `main` | FQCN класса, реализующего `VidaMod` |
| `client` | FQCN класса, вызываемого только в клиентской сборке (опционально) |
| `server` | FQCN класса для серверной сборки |
| `preLaunch` | Классы, запускаемые в фазе до инициализации Vifada (для ранних хуков) |

На 0.x активно используется только `main`. Остальные — preview.

### `dependencies` — optional, object

Три секции:

```json
"dependencies": {
  "required":         { "id": "range", ... },
  "optional":         { "id": "range", ... },
  "incompatibilities":{ "id": "range", ... }
}
```

`range` — SemVer-диапазон по NPM-совместимому синтаксису:

- `"1.2.3"` — строго `1.2.3`
- `"^1.2.3"` — `>=1.2.3 <2.0.0`
- `"~1.2.3"` — `>=1.2.3 <1.3.0`
- `"1.2.*"` — `>=1.2.0 <1.3.0`
- `"*"` — любая
- `"1.0.0 - 2.0.0"` — inclusive range
- `"^1.0.0 || ^2.0.0"` — объединение

Специальные `id`:

- `vida` — сам загрузчик (обычно `"^0.1.0"` или `">=0.1.0"`)
- `minecraft` — версия игры (обычно `"1.21.1"`)

### `provides` — optional, array of string

Мод заявляет себя как альтернативного провайдера:

```json
"provides": ["generic-dungeon-api@1.0.0"]
```

Формат: `<id>@<version>`. Разрешает другим модам указать `"generic-dungeon-api": "^1.0.0"` в своих `dependencies` — Vida найдёт этот мод как удовлетворяющего.

### `vifada` — optional, object

Конфигурация системы модификации байткода:

```json
"vifada": {
  "morphs": ["com.ejemplo.morphs.X"]
}
```

`morphs` — FQCN классов с `@VifadaMorph`. Резолвер Vifada использует это как white-list; классы не в списке игнорируются (защита от случайных классов в JAR).

### `puertas` — optional, array of string

Пути к файлам конфигурации Puertas (access-wideners) внутри JAR. Формат файла Puertas — в будущем документе.

### `modules` — optional, array of string

Пути к вложенным JAR-модам внутри текущего JAR. Типичный путь — `META-INF/jars/<name>.jar`. Каждый nested-JAR должен быть корректным модом Vida (иметь свой `vida.mod.json`).

### `escultores` — optional, array

Цепочка классов **`Escultor`** (ASM-патч `byte[] → byte[]`), объявляемых модом. Элемент — либо строка **FQCN**, либо объект:

```json
{ "class": "com.ejemplo.MiEscultor", "priority": 10 }
```

Ключ класса допускает альтернативы: `"class"`, `"className"` или `"fqn"`. Поле **`priority`** — целое; **меньшее значение** выполняется **раньше** относительно других Escultor этого мода (после встроенного `BrandingEscultor` загрузчика). По умолчанию `0`.

Регистрация и загрузка классов — в `BootSequence` / `VidaClassTransformer` (см. [modules/loader.md](../modules/loader.md), [guides/escultores.md](../guides/escultores.md)).

### `incompatibilities` — optional, array of string

Список `id` модов, с которыми этот мод принципиально несовместим. Эквивалентно `dependencies.incompatibilities` с `range = "*"`, но в плоском формате.

### `custom` — optional, object

Произвольные данные для инструментов и API. Ключи — namespaced (например, `"my-tool:icon"`). Vida не интерпретирует содержимое; API модов может прочитать через `ModContext.metadata().custom()`.

#### Специальный ключ `vida:dataDriven` (контракт 2.x)

Модуль [`:fuente`](../modules/fuente.md) (`dev.vida.fuente`) читает из `custom` прототип data-driven мода; загрузчик оркестрирует вызов при bootstrap:

```json
"custom": {
  "vida:dataDriven": {
    "enabled": true,
    "datapackRoot": "data/miaventura/vida"
  }
}
```

- `enabled` (`boolean`) — включает prototype-парсер.
- `datapackRoot` (`string`, optional) — корень datapack-путей; по умолчанию `data/<mod-id>/vida`.

Ожидаемая структура datapack:

- `<root>/bloques/*.json`
- `<root>/objetos/*.json`
- `<root>/recipes/*.json` (`type = "vida:shaped"`)
- `<root>/loot_tables/**/*.json` — минимальный парсинг пулов с `minecraft:item` (2.0)

Формат JSON для loot tables и shaped-рецептов считается частью контракта `@Stable` для `:fuente` в линии 2.x.

## JSON-нюансы

- Строгий JSON: без комментариев, trailing-запятых, `undefined`.
- Порядок полей в объекте не важен.
- Строки — UTF-8.
- Числа — как integer или decimal, но все поля выше — строки (`version` — тоже строка).

## Эволюция

Если схема станет `2`, это означает breaking change (например, переименование `authors` в `credits`). Парсер будет знать об обоих форматах и конвертировать при чтении. Плагин `dev.vida.mod` всегда генерирует актуальную версию `schema`.

## См. также

- [modules/manifest.md](../modules/manifest.md) — парсер и типы.
- [modules/fuente.md](../modules/fuente.md) — модуль `:fuente` и `vida:dataDriven`.
- [modules/gradle-plugin.md](../modules/gradle-plugin.md) — DSL, генерирующий манифест.
- [modules/resolver.md](../modules/resolver.md) — как читается `dependencies`.
