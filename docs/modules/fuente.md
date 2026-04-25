# fuente

Модуль **Fuente** — чтение **data-driven** деклараций из JAR мода: JSON, уложенные в путях ванильного datapack’а относительно `custom["vida:dataDriven"].datapackRoot` в `vida.mod.json`. Загрузчик вызывает парсер в `BootSequence` и публикует снимок в [`VidaEnvironment.fuenteDataDriven()`](./loader.md); сам модуль **`:fuente`** не зависит от **`:loader`**.

- Пакет: `dev.vida.fuente`
- Gradle: `:fuente` (артефакт `dev.vida:vida-fuente` в BOM)
- Стабильность: **`@ApiStatus.Stable`** (форматы JSON, публичные record’ы и сиды ошибок в линии 2.x)

## Назначение

| Кто | Зачем |
|-----|--------|
| Мододел | Описать часть контента декларативно (блоки, предметы, рецепты, loot, плюс **проверка** ссылок в `worldgen/`) без дублирования id в Java. |
| Загрузчик | Один проход по zip: валидный снимок до entrypoint’ов; при ошибке — `LoaderError.DataDrivenFailure` (см. [loader.md](./loader.md)). |
| Тесты / инструменты | `FuentePrototipoParser.leer(ModManifest, ZipReader)` — без Minecraft. |

## Зависимости (Gradle)

- `core`, `manifest`, `discovery` — `Identifier`, `Result`, `ModManifest`, чтение zip.
- `bloque`, `objeto` — сопоставление строк в JSON с `MaterialBloque` и `TipoObjeto`.

## Включение в манифест

См. [reference/manifest-schema.md](../reference/manifest-schema.md#специальный-ключ-vidadatadriven-контракт-2x). Кратко:

```json
"custom": {
  "vida:dataDriven": {
    "enabled": true,
    "datapackRoot": "data/mi_mod/vida"
  }
}
```

- `datapackRoot` по умолчанию: `data/<mod-id>/vida` (см. генератор плагина `dev.vida.mod`).

## Каталоги относительно `datapackRoot`

| Путь | Содержимое | Результат в `FuenteContenidoMod` |
|------|------------|----------------------------------|
| `bloques/*.json` | Декларация блока (id, material, dureza) | `List<FuenteBloque>` |
| `objetos/*.json` | Декларация предмета | `List<FuenteObjeto>` |
| `recipes/*.json` | Только `type: "vida:shaped"` | `List<FuenteRecetaShaped>` |
| `loot_tables/**/*.json` | Ванильные loot JSON | `List<FuenteLootTable>` (пулы, item id, вложенные `minecraft:loot_table`) |
| `worldgen/**/*.json` | Placed/configured feature, template pool, processor list, structure, … | `List<FuenteWorldgenHuella>` (извлечённые **ссылки**, не полная семантика worldgen) |

Пустой `datapackRoot` в конфиге — ошибка. Каталог в **исходниках** мода (`src/main/resources/...`) при `enabled: true` должен существовать, иначе `vidaValidateManifest` падает (см. [gradle-plugin.md](./gradle-plugin.md)).

## Ключевые типы

| Тип | Роль |
|-----|------|
| `FuentePrototipoParser` | `leer(ModManifest, ZipReader)` — единая точка входа. |
| `FuenteContenidoMod` | Снимок: `bloques`, `objetos`, `recetasShaped`, `tablasLoot`, **`worldgen`**, `rootDatapack`, флаг `habilitado`. |
| `FuenteBloque`, `FuenteObjeto`, `FuenteRecetaShaped`, `FuenteLootTable` | Модели соответствующих JSON. |
| `FuenteWorldgenHuella` | Один файл `worldgen/...json`: путь в zip + отсортированные списки `refsLoot` / `refsBloque`. |
| `FuenteLootPipeline` | `validarReferenciasInternas` — вложенные `minecraft:loot_table` в loot JSON согласованы с другими `loot_tables` снимка. |
| `FuenteWorldgenPipeline` | `validarVsDatapack` — ссылки из worldgen на **loot/блоки в namespace мода** (`ModManifest.id()`) должны иметь JSON в `loot_tables` / `bloques`. |
| `FuenteError` | Закрытая иерархия: JSON, enum, loot, worldgen (см. ниже). |
| `FuenteManifestoDatapackValidador` | Gradle: существование корня datapack в resources. |

## Парсинг loot-таблиц

- Item id: `pools[].entries` с `type: "minecraft:item"` и `name` (рекурсия по `children`).
- Вложенные таблицы: `type: "minecraft:loot_table"`, имя в `name` / `value` / `value.name` (как в ваниле).
- Id таблицы выводится из пути: `.../loot_tables/<ruta>.json` → `namespace:<ruta>` (namespace — default при разборе id манифеста).

## Worldgen: извлечение ссылок

Парсер **не** валидирует схему worldgen движка: он строит **эвристический** обход JSON (`FuenteWorldgenEscan`):

- Любой ключ **`loot_table`** со строковым значением → `Identifier`.
- Ключи **`block`**, массив **`blocks`[]** — идентификаторы блоков.
- Пара **`Name` + `Properties`** в одном объекте — `Name` трактуется как block state (id блока).

Всё остальное (вложенные `feature`, jigsaw, `template_pool`, и т.д.) обходится **рекурсивно**, чтобы поймать ссылки в глубине. Ссылки в namespace **`minecraft`** (и любой чужой, не равный `manifest.id`) **не** требуют декларации в Fuente — предполагается ванильный/внешний registry.

## Порядок проверок в `leer`

1. Разбор всех JSON в перечисленных ветках; сортировка списков для детерминизма.
2. `FuenteLootPipeline.validarReferenciasInternas` — целостность **между** loot-файлами.
3. `FuenteWorldgenPipeline.validarVsDatapack` — для id с namespace **мода** — наличие целевой loot-таблицы / блока в снимке.

Любая ошибка → `Result.err(FuenteError)`.

## Таблица `FuenteError` (публичный контракт)

| Вариант | Когда |
|---------|--------|
| `ConfigInvalida` | Кривой `vida:dataDriven` в custom. |
| `JsonInvalido` | Не JSON, неверный корень, сбой `JsonReader`. |
| `CampoFaltante` / `TipoDesconocido` | Не хватает поля или enum-строка не из `MaterialBloque` / `TipoObjeto`. |
| `TablaLootReferenciaRota` | Внутри loot ссылка на другую таблицу мода без соответствующего файла. |
| `WorldgenLootReferenciaRota` | В `worldgen` указан `loot_table` с id мода без JSON в `loot_tables/`. |
| `WorldgenBloqueReferenciaRoto` | В worldgen — блок мода без `bloques/*.json`. |

## Перезагрузка ресурсов (production)

Отдельно от **dev-only** hot-reload классов (см. [hot-reload.md](../guides/hot-reload.md)) существует контракт **перезагрузки data-driven снимка**: при событии ресурсов клиента загрузчик эмитит **`LatidoFuenteRecargada`**. Мод подписывается и сбрасывает кэши, завязанные на `FuenteContenidoMod`, через **`FuenteRecarga.alRecargar(bus, runnable)`**; повторные вызовы идемпотентны по смыслу подписки (каждый `alRecargar` добавляет обработчик). Повторный **`FuentePrototipoParser.leer`** на одном и том же `ZipReader` даёт тот же снимок (см. тест идемпотентности в `FuentePrototipoParserTest`).

## Кодировки и имена файлов

JSON читается как **UTF-8**. Имена записей ZIP могут содержать не-ASCII (см. тест `FuenteZipUtf8NombresTest`).

## Gradle и CI

Сценарий только-datapack (без кода) и разбор loot_tables описаны в `docs/reference/manifest-schema.md` и в разделе про loot в миграции 2.0; отдельного эталонного модуля в каталоге `mods/` в этом репозитории нет.

## См. также

- [reference/manifest-schema.md](../reference/manifest-schema.md) — ключ `vida:dataDriven`
- [modules/loader.md](./loader.md) — bootstrap и `fuenteDataDriven`
- [guides/modder-toolkit.md](../guides/modder-toolkit.md) — DSL и быстрые ссылки
- [architecture/bootstrap.md](../architecture/bootstrap.md) — шаг Fuente в `BootSequence`
