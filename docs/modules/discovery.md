# discovery

Сканер `mods/`: находит `.jar`-файлы, читает `vida.mod.json`, разворачивает вложенные JAR'ы, кэширует результат.

- Пакет: `dev.vida.discovery`
- Gradle: `dev.vida:vida-discovery`
- Стабильность: `@ApiStatus.Stable`

## Главные типы

### `ZipReader`

Абстракция над zip/jar с двумя реализациями:

- `FileZipReader` — дисковый файл; использует JDK-шный `java.util.zip.ZipFile` под капотом.
- `BytesZipReader` — in-memory; нужен для вложенных JAR, которые лежат внутри других JAR.

Единый API позволяет дискавери не знать, с чем он работает.

```java
try (ZipReader r = FileZipReader.open(Path.of("mods/miaventura.jar"))) {
    Optional<byte[]> manifest = r.read("vida.mod.json");
    // ...
}
```

### `ModSource`

Sealed-иерархия источников:

- `ModSource.OnDisk(Path)` — JAR, лежащий в `mods/` напрямую.
- `ModSource.Embedded(ModSource parent, String entry)` — JAR внутри другого JAR.

Sealed гарантирует, что pattern matching покрывает все варианты.

### `ModCandidate`

«Нашли и разобрали»:

```java
record ModCandidate(
    ModSource source,
    ModManifest manifest,
    String contentFingerprint) { ... }
```

`contentFingerprint` — SHA-256 содержимого JAR. Используется кэшем и резолвером (для обнаружения «одного и того же мода из двух источников»).

### `ModScanner`

Основной entry-point:

```java
DiscoveryReport report = ModScanner.scan(Path.of("mods"), options);
log.info("successes: {}, errors: {}", report.successes().size(), report.errors().size());
```

### `DiscoveryReport`

Сгруппированный результат:

- `successes` — список `ModCandidate`;
- `errors` — список `DiscoveryError` с указанием, какой файл не удалось прочесть и почему;
- статистика: время, размер всех JAR'ов, счётчик кэш-хитов.

### `DiscoveryError`

Sealed-иерархия:

- `NotAZip(Path)` — файл с расширением `.jar`, но не zip;
- `MissingManifest(ModSource)` — zip есть, `vida.mod.json` внутри нет;
- `ManifestParseError(ModSource, ManifestError)` — JSON есть, но невалидный;
- `IoError(Path, IOException)` — файл не читается с диска.

## Вложенные JAR-ы

Vida поддерживает nested JAR (мод, который таскает свои зависимости или подкомпоненты внутри собственного JAR — в каталоге `META-INF/jars/`). Правила:

1. Внешний мод должен объявить их в `vida.mod.json` через поле `modules` (список путей внутри JAR).
2. Каждый вложенный JAR должен иметь собственный `vida.mod.json` и пройти парсинг.
3. Nested-JAR получают source type `ModSource.Embedded` — резолвер знает, что это не «отдельный мод в `mods/`».

Мотивация: позволяет крупным модам распространять свои API-компоненты как отдельные Vida-моды без дублирования JAR-ов в `mods/`.

## Кэш `mods.idx`

Собственный бинарный формат в подпакете `dev.vida.discovery.cache`.

Хранит:

- заголовок (`schema`, версия Vida, платформа);
- для каждого JAR: `path`, `mtime`, `size`, `sha256`, сериализованный `ModManifest`.

При повторном запуске `ModScanner` проверяет:

1. `mtime + size` — если совпало, пропускаем перечитывание.
2. Если не совпало, но `sha256` файла совпал с кэшем — обновляем `mtime/size` в кэше и используем его.
3. Иначе — полный парсинг.

На 150 модах: холодный старт ~250 мс, тёплый — ~40 мс.

### Инвалидация

Автоматическая:

- изменился JAR — изменились его mtime/size/hash;
- изменилась версия Vida — `schema` кэша не совпадёт;
- поменялась платформа — `platform` в заголовке отличается.

Ручная:

```bash
rm run/vida/mods.idx
```

## Параллелизм

Скан параллельный (workers = `Runtime.availableProcessors()`). На каждой фазе:

- **listing** (`Files.list`) — однопоточно, O(N) по числу файлов в `mods/`.
- **read manifest** — параллельно, по одному worker'у на файл.
- **parse manifest** — параллельно, на том же worker'е (дёшево).
- **cache write** — однопоточно, в конце.

Ограничения:

- JDK-шный `ZipFile` имеет внутреннюю блокировку; мы не шарим один reader между потоками.
- IO на HDD деградирует с количеством параллельных воркеров; если `discovery.io.maxThreads` ограничен (опция `BootOptions`), мы его уважаем.

## Что читать дальше

- [modules/resolver.md](./resolver.md) — как `ModCandidate`'ы превращаются в `Universe` и что делает резолвер.
- [modules/manifest.md](./manifest.md) — парсер, которому мы отдаём байты `vida.mod.json`.
- [architecture/performance.md](../architecture/performance.md) — контекст, почему дискавери должен быть быстрой.
