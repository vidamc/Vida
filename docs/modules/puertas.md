# puertas

Access wideners Vida. Формат `.ptr` + parser + ASM-аппликатор. Эквивалент Fabric `*.accesswidener` / Forge access transformers — но с явным namespace-тегом и без специальной семантики, которую нельзя выразить в JVM access flag'ах.

- Пакет: `dev.vida.puertas`
- Gradle: `dev.vida:vida-puertas`
- Стабильность: `@ApiStatus.Preview("puertas")`

Введён в 0.3.0.

Практическое руководство с примерами и troubleshooting — [guides/puertas.md](../guides/puertas.md).

## Зачем ещё один access widener

1. Vida уже держит свой слой маппингов (`Cartografía`). Разные моды могут писать `.ptr` против `INTERMEDIO` / `EXTERIOR` / `CRUDO` — аппликатор понимает, в какой namespace перевести имена перед наложением.
2. `.ptr` парсер возвращает **все** ошибки сразу, не останавливается на первой. В dev-цикле это экономит десятки циклов «fix-compile-fix».
3. Integration с [`gradle-plugin`](./gradle-plugin.md) — таск `vidaValidatePuertas` ломает сборку при невалидных файлах, `vidaPackagePuertas` упаковывает `.ptr` в jar с правильной структурой ресурсов.

## Формат .ptr

```
# Комментарии начинаются с #
vida-puertas 1 namespace=intermedio

# Сделать класс публичным
accesible  class   net/example/Foo

# Разрешить наследование
extensible class   net/example/Foo

# Сделать метод публичным (+ снять final, если был)
accesible  method  net/example/Foo   bar  (I)Ljava/lang/String;

# Сделать приватное поле публичным (+ снять final)
accesible  field   net/example/Foo   count I

# Снять final с поля, не трогая видимость
mutable    field   net/example/Foo   count I
```

**Заголовок** — первая значимая строка:

```
vida-puertas <version> namespace=<crudo|intermedio|exterior>
```

В 0.3.0 поддерживается только `version=1`. Несовпадение → `PuertaError.VersionNoSoportada`, парсинг прерывается.

**Директивы** — по одной на строку: `<accion> <objetivo> <claseInternal> [<nombreMiembro> <descriptor>]`.

Комментарии: `#` до конца строки. Пустые строки игнорируются.

## API

### `PuertaParser`

Входная точка парсинга.

```java
import dev.vida.puertas.*;

PuertaParser.ParseResult r = PuertaParser.parsear(Path.of("src/main/resources/puertas/mi.ptr"));
if (!r.esExitoso()) {
    for (PuertaError e : r.errores()) {
        System.err.println(e);     // структурированное сообщение
    }
    throw new IOException("битые .ptr");
}
PuertaArchivo archivo = r.archivo();
```

Три фабрики:

- `parsear(Path)` — читает файл с диска (UTF-8);
- `parsear(String origen, String contenido)` — для тестов и in-memory сценариев;
- `parsear(String origen, Reader r)` — общий случай, для произвольного источника.

**Soft-fail семантика:** парсер старается дочитать файл до конца и выдать все ошибки сразу. Исключение — битый заголовок (дальнейший парсинг не имеет смысла, парсинг прерывается, но `ParseResult.archivo()` всё равно не `null` — это заглушка с пустым списком директив, чтобы сверху-уровневый код не делал `null`-проверок).

### `PuertaArchivo`

Иммутабельный результат парсинга:

```java
String nombreOrigen = archivo.nombreOrigen();   // путь/имя, для диагностики
int version = archivo.version();                // 1
Namespace ns = archivo.namespace();             // CRUDO / INTERMEDIO / EXTERIOR
List<PuertaDirectiva> todas = archivo.directivas();
List<PuertaDirectiva> soloParaFoo = archivo.paraClase("net/example/Foo");
Set<String> clasesZatronutye = archivo.clases();
```

Индекс `порClase` строится один раз в конструкторе — `paraClase()` — O(1).

**Слияние нескольких файлов** (для одного мода или для всего рантайма):

```java
PuertaArchivo combinado = PuertaArchivo.combinar(
        "mi-mod",
        List.of(archivo1, archivo2, archivo3));
```

Все части должны иметь одинаковый `namespace`; иначе — `IllegalArgumentException`.

### `PuertaDirectiva`

`record` одной строки:

```java
PuertaDirectiva(
    Accion accion,                       // ACCESIBLE / EXTENSIBLE / MUTABLE
    Objetivo objetivo,                   // CLASE / METODO / CAMPO
    String claseInternal,                // "net/example/Foo" (always internal-name)
    Optional<String> nombreMiembro,      // empty для CLASE
    Optional<String> descriptor,         // empty для CLASE
    int linea                            // для сообщений
)
```

`objetivoEs(claseInternal)` — быстрый match (equals).

Валидация в каноническом конструкторе:

- `claseInternal` не должен содержать `.` (только internal-name с `/`);
- для `CLASE` — `nombreMiembro.isEmpty() && descriptor.isEmpty()`;
- для `METODO` / `CAMPO` — оба обязательны.

### `Accion`

| Константа | Класс | Метод | Поле |
|-----------|-------|-------|------|
| `ACCESIBLE` | → `public` | → `public` | → `public`, `final` снят |
| `EXTENSIBLE` | `final` снят | `final` снят | — (парсер отвергает) |
| `MUTABLE` | — (парсер отвергает) | — (парсер отвергает) | `final` снят, видимость как была |

Правила парсера:

- `mutable` допустим только для `field` → иначе `PuertaError.MutableNoAplicable`.
- `extensible field` — невалидно, парсер его не пропускает.

### `Objetivo`

`CLASE`, `METODO`, `CAMPO`. Определяет, сколько токенов ожидается в директиве (3 для CLASE, 5 для METODO/CAMPO).

### `Namespace`

Namespace маппинга, в котором записаны имена `claseInternal` / `nombreMiembro` / `descriptor`.

```java
Namespace.CRUDO        // "crudo"     — obfuscated
Namespace.INTERMEDIO   // "intermedio"— стабильные промежуточные (рекомендовано для .ptr)
Namespace.EXTERIOR     // "exterior"  — Mojang-mapped (для dev-сборки)
```

Рантайм ожидает имена в `INTERMEDIO`. Если мод писал `.ptr` в `EXTERIOR` (частый случай в dev), `Cartografía` должна перевести имена **до** `AplicadorPuertas.aplicar(...)` — интеграция на стороне `loader` (см. session-roadmap 0.4.x).

### `AplicadorPuertas`

ASM-применение директив к одному классу.

```java
byte[] bytesOriginales = /* загружены class-loader'ом */;
AplicadorPuertas.Resultado res = AplicadorPuertas.aplicar(
        bytesOriginales,
        archivo.directivas());

if (res.informe().aplicadas() == 0) {
    // Ни одна директива не нашла целевой класс — байты те же самые.
}
byte[] bytesModificados = res.bytes();
```

`Resultado` — байты + `Informe`:

```java
record Informe(int aplicadas, List<PuertaDirectiva> perdidas) {}
```

`perdidas` — директивы, для которых целевой метод/поле не нашлись в классе. Это диагностика: мод написал `.ptr` под старую версию vanilla, а после обновления членов не осталось.

**Оптимизация:** если ни одна директива не касается целевого класса, `AplicadorPuertas.aplicar` возвращает исходные байты без ре-кодирования — это быстрее и не шумит в диффах при инструментах сравнения jar'ов.

### `PuertaError` (sealed)

Структурированные ошибки. Все включают `ubicacion()` для читаемых сообщений:

| Запись | Когда |
|--------|-------|
| `CabeceraInvalida(ubicacion, linea, razon)` | заголовок битый или отсутствует |
| `NamespaceDesconocido(ubicacion, valor)` | `namespace=xxx`, где `xxx` не `crudo/intermedio/exterior` |
| `VersionNoSoportada(ubicacion, version)` | версия > 1 |
| `DirectivaInvalida(ubicacion, linea, texto, razon)` | неизвестная accion/objetivo/лишние токены |
| `DirectivaTruncada(ubicacion, linea, texto)` | не хватает токенов |
| `DescriptorMalformado(ubicacion, linea, descriptor)` | JVM descriptor не парсится |
| `MutableNoAplicable(ubicacion, linea, objetivo)` | `mutable` применено не к полю |

## Интеграция со сборкой

См. [modules/gradle-plugin.md](./gradle-plugin.md#puertas):

- `vidaValidatePuertas` — запускает `PuertaParser.parsear(Path)` на все `.ptr` файлы мода, ломает сборку при ошибках;
- `vidaPackagePuertas` — собирает `.ptr` в `META-INF/vida/puertas/` внутри jar'а мода;
- настраиваются через `vida { puertas { srcDirs.add(...) } }`.

## Валидация и упаковка через CLI

Локально, без Gradle:

```bash
java -jar vida-installer.jar --headless --validate-puertas path/to/puertas.ptr
# или рекурсивно по директории:
java -jar vida-installer.jar --headless --validate-puertas src/main/resources/puertas/
```

Подробно — [reference/cli-installer.md](../reference/cli-installer.md#--validate-puertas).

## Потокобезопасность

- `PuertaParser` — stateless, все методы static, thread-safe.
- `PuertaArchivo` / `PuertaDirectiva` / `PuertaError` — иммутабельные.
- `AplicadorPuertas.aplicar(...)` — static, не делится state'ом; можно звать из нескольких потоков для разных классов параллельно.

## Производительность

- Парсинг — один `BufferedReader` проход, без регекспов кроме `\\s+` для токенизации.
- `PuertaArchivo.paraClase(...)` — O(1) через `Map<claseInternal, List<directivas>>`.
- Аппликатор без затронутых классов — чистая `ClassReader.getClassName()` + bitmap lookup, ≈ 1 мкс.
- С применением — `ClassReader → ClassNode → ClassWriter(COMPUTE_MAXS)`; на классе 10k инструкций ≈ 2 мс.

## Тесты

`puertas/src/test/java`:

- `PuertaParserTest` — 30+ кейсов, в т.ч. soft-fail, soft-recovery после одной ошибки, валидные descriptor'ы (primitives, array, L-typed, void), невалидные (`Labc`, `(L;)`, `Q`).
- `PuertaArchivoTest` — `paraClase`, `combinar`, несовпадение namespace.
- `AplicadorPuertasTest` — все семь комбинаций (accion × objetivo), `perdidas`, no-op для неизменённого класса. Генерируется синтетический класс через ASM в тесте, применяется aplicador, проверяются итоговые access flags.
- `PuertaDirectivaTest` / `NamespaceTest` — валидация record'ов.

~97% покрытия.

## Что ещё будет в `puertas`

- Namespace-remapping через Cartografía прямо внутри `AplicadorPuertas` (сейчас — ответственность loader'а).
- `extensible method` — автоматическая замена `invokespecial` на `invokevirtual` для super-вызовов (сейчас — только флаг `final` снимается).
- Конфликт-резолвер для ситуации, когда два мода хотят противоречивые изменения доступа одного члена.

Сроки — в [session-roadmap: 0.5.0](../session-roadmap.md#session-3--050--мир-и-сущности).

## Что читать дальше

- [guides/puertas.md](../guides/puertas.md) — как написать свой `.ptr`, частые ошибки, reference `.ptr` на реальных vanilla-классах.
- [modules/gradle-plugin.md](./gradle-plugin.md#puertas) — Gradle-таски.
- [reference/cli-installer.md](../reference/cli-installer.md#--validate-puertas) — CLI-валидация.
- [modules/cartografia.md](./cartografia.md) — как переводятся имена между namespace'ами.
