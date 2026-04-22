# red

Публичный сетевой API Vida (Tejido): типизированные пакеты клиент↔сервер, авто-сериализация `record`-ов, versioned codecs и back-pressure очередь отправки.

- Пакет: `dev.vida.red`
- Gradle: `dev.vida:vida-red`
- Стабильность: `@ApiStatus.Preview("red")`

## Что есть в 0.6.0

### Маркеры направления

- `PaqueteCliente` — пакет от клиента к серверу.
- `PaqueteServidor` — пакет от сервера к клиенту.
- `DireccionPaquete` — wire-направление (`CLIENTE_A_SERVIDOR`, `SERVIDOR_A_CLIENTE`).

### `CodecPaquete<T>`

Контракт сериализации одного packet-type:

- `codificar(T paquete) -> byte[]`
- `decodificar(byte[] payload) -> T`

### `CodificadorRegistros`

Авто-генерация codec для Java record:

```java
CodecPaquete<MiPaquete> codec = CodificadorRegistros.para(MiPaquete.class);
```

Поддерживаемые типы полей:

- `int`, `long`, `boolean`, `float`, `double`
- `String`
- `Identifier`
- `enum`

### `TejidoCanal`

Памятный канал отправки/приёма:

- регистрация record-пакетов по версии codec:
  - `registrarRecordCliente(Tipo.class, version)`
  - `registrarRecordServidor(Tipo.class, version)`
- отправка в очередь: `encolar(paquete, version)`
- декодирование wire-frame: `decodificar(trama)`
- дренаж очереди: `drenarPendientes()`
- метрики: `estadisticas()`

Versioning-контракт:

- при отправке используется точное совпадение версии codec;
- при приёме допустим fallback на ближайшую меньшую версию (`floorEntry`), если точной нет.

Back-pressure:

- канал имеет `maxCola`;
- при переполнении `encolar(...)` возвращает `Err(TejidoError.BackPressure)`.

### `TramaPaquete`

Wire-level frame:

- направление;
- canonical имя packet-type;
- версия codec;
- бинарный payload.

### `TejidoError`

Типизированные ошибки:

- `TipoNoRegistrado`
- `VersionNoSoportada`
- `BackPressure`
- `PayloadInvalido`

## Тесты

`red/src/test/java`:

- `TejidoCanalTest` — round-trip, version fallback, back-pressure, queue drain.
- `CodificadorRegistrosTest` — авто-кодек примитивов/`Identifier`, валидация unsupported типов.

## Что читать дальше

- [`base`](./base.md) — Latidos/Catalogo/Ajustes вокруг сетевого API.
- [session-roadmap](../session-roadmap.md#session-4--060--рендер-сеть-дата) — план Session 4.
