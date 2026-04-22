# mundo

API мира и world-латидосов: `Mundo`, `Coordenada`, `Dimension`, `Bioma` и события из `LatidosMundo`. Модуль соединяет декларативные типы из `vida-base`/`vida-entidad` с реальным world-runtime, но сам по себе остаётся максимально лёгким и тестируемым.

- Пакет: `dev.vida.mundo`
- Gradle: `dev.vida:vida-mundo`
- Стабильность: `@ApiStatus.Preview("mundo")`

`vida-mundo` появился в `0.5.0` как bridge-слой между чистыми data-типами и Minecraft world runtime.

```kotlin
dependencies {
    compileOnly("dev.vida:vida-mundo:0.5.0")
}
```

## Главные типы

### `Mundo`

Публичный интерфейс снимка мира. В `0.5.0` он intentionally компактный:

```java
public interface Mundo {
    Identifier id();
    Dimension dimension();
    Bioma biomaEn(Coordenada coordenada);
    boolean estaCargado(Coordenada coordenada);
    long tiempoDelDia();
}
```

Дополнительные default-методы:

- `esDeDia()`
- `esDeNoche()`

Этого достаточно для безопасных подписчиков и тестируемой доменной логики без утечки vanilla-классов в публичный API.

### `Coordenada`

Трёхмерный `record` (`x`, `y`, `z`) с утилитами для world-кода:

```java
Coordenada origen = new Coordenada(120, 64, -30);
Coordenada arriba = origen.desplazar(0, 1, 0);

int chunkX = origen.chunkX();
int chunkZ = origen.chunkZ();
long d2 = origen.distanciaCuadrada(arriba);
```

`Coordenada` неизменяема, поэтому её можно безопасно кэшировать, использовать в ключах карт и гонять между потоками.

### `Dimension`

Value-type измерения:

- `id`
- `natural`
- `permiteCama`
- `techoFijo`

Есть предустановленные константы `OVERWORLD`, `NETHER`, `END`, но моды могут создавать и свои измерения через `Dimension.de(...)`.

### `Bioma`

Описание биома:

- `id`
- `temperatura`
- `humedad`
- `precipitacion`

```java
Bioma tundra = new Bioma(
        Identifier.of("minecraft", "snowy_plains"),
        0.0f,
        0.5f,
        Bioma.Precipitacion.NIEVE);
```

Полезные shortcut'ы:

- `esFrio()`
- `tienePrecipitacion()`

## World-латидосы (`dev.vida.mundo.latidos`)

Модуль добавляет типизированные события уровня мира:

### `LatidosMundo.MundoCargado`

Срабатывает после появления `Mundo` в рантайме Vida.

```java
@EjecutorLatido
public void alCargar(LatidosMundo.MundoCargado ev) {
    Mundo mundo = ev.mundo();
    if (mundo.dimension().natural()) {
        // ...
    }
}
```

### `LatidosMundo.ChunkCargado`

Событие загрузки чанка:

- `mundo`
- `chunkX`
- `chunkZ`
- `completo`

`completo=false` полезен для случаев, когда runtime знает про chunk только частично (например, ранняя стадия генерации).

### `LatidosMundo.Tick`

World-specific тик:

- `mundo`
- `tickActual`
- `tiempoDelDia`

Это дополняет общий `LatidoPulso` из `vida-base`: если нужен именно мир, а не абстрактный «пульс сервера», слушайте `LatidosMundo.Tick`.

### `LatidosMundo.NocheAmanece`

Событие перехода суток:

- `mundo`
- `tiempoAnterior`
- `tiempoActual`
- `transicion` (`AMANECER` / `ANOCHECER`)

Хорошо подходит для AI-режимов, ambient-музыки, scheduled spawn'ов и light-sensitive механик.

## `@OyenteDeTick`

`vida-base` в `0.5.0` добавляет аннотацию `@OyenteDeTick(tps = 20)` как shortcut для подписки на `LatidoPulso` с частотным throttling'ом.

```java
@OyenteDeTick(tps = 1)
public void guardarCadaSegundo(LatidoPulso ev) {
    // вызывается примерно раз в секунду
}
```

Особенности:

- работает по корневым тикам (`profundidad == 0`);
- поддерживает те же executor-параметры, что и `@EjecutorLatido`;
- если нужен каждый subtick без фильтрации, используйте обычный `@EjecutorLatido`.

## Потокобезопасность

- `Coordenada`, `Dimension`, `Bioma` и payload'ы `LatidosMundo.*` — иммутабельны.
- Поток выполнения обработчика по-прежнему определяется `Ejecutor` / `@EjecutorLatido` / `@OyenteDeTick`.
- `Mundo` сам по себе может быть backed runtime-объектом; thread-safety конкретной реализации определяется движком, но её публичные value-поля должны оставаться согласованными.

## Что читать дальше

- [`entidad`](./entidad.md) — регистрация и свойства сущностей.
- [`guides/first-entity.md`](../guides/first-entity.md) — пример мода с собственной сущностью и подписчиками.
- [`base-ejecutor`](./base-ejecutor.md) — `Ejecutor`, `@EjecutorLatido`, `LatidoRegistrador`.
