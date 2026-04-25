# bloque

API описания блоков. Side-agnostic data-модель: `Bloque` + `PropiedadesBloque` + форма коллизии + звук. Всё иммутабельное, без зависимости от vanilla-классов Minecraft — bridge к `net.minecraft.world.level.block.Block` живёт в будущем `vida-mundo`.

- Пакет: `dev.vida.bloque`
- Gradle: `dev.vida:vida-bloque`
- Стабильность: **`@ApiStatus.Stable`** с 1.0.0

Модуль появился в 0.3.0; публичный API заморожен по SemVer начиная с линии 1.x (см. [reference/api-stability.md](../reference/api-stability.md)).

```kotlin
dependencies {
    compileOnly("dev.vida:vida-bloque:0.3.0")
}
```

`compileOnly` — по той же причине, что и для `vida-base`: на рантайме модуль живёт в `JuegoLoader`, шаринг между модами.

## Главные типы

### `Bloque`

Точка входа. Data-объект: `Identifier` + `PropiedadesBloque`.

```java
import dev.vida.bloque.*;
import dev.vida.core.Identifier;

Bloque piedraOscura = new Bloque(
        Identifier.of("ejemplo", "piedra_oscura"),
        PropiedadesBloque.con(MaterialBloque.PIEDRA)
                .dureza(2.0f)
                .herramientas(TipoHerramienta.PICO)
                .construir());
```

Гарантии:

- `equals` / `hashCode` — по `id` + `propiedades`;
- shortcuts `material()` и `forma()` — без лишнего ковыряния `propiedades().material()`;
- `final` поля, mutable state отсутствует.

### `PropiedadesBloque`

`record` с fluent-билдером `PropiedadesBloque.con()`. Собирает 16 независимых свойств: материал, дурость, сопротивление взрыву, световое излучение, трение, speed-модификаторы, требуемый инструмент, форма коллизии, звук.

```java
PropiedadesBloque props = PropiedadesBloque.con(MaterialBloque.METAL)
        .dureza(5.0f)
        .resistenciaExplosion(6.0f)
        .luzEmitida(7)
        .herramientas(TipoHerramienta.PICO)
        .nivelMinimo(NivelHerramienta.HIERRO)
        .forma(FormaColision.aabb(0, 0, 0, 1, 0.75, 1))
        .sonido(SonidoBloque.metal())
        .construir();
```

Валидация — в канонической форме record'а. Невалидные значения бросают `IllegalArgumentException` с читаемым сообщением:

- `luzEmitida` вне `[0..15]`;
- `friccion` вне `[0..1]`;
- `dureza < -1` (bedrock = `-1` допускается);
- `material=LIQUIDO`, но `replaceable=false` (жидкости всегда проходимы).

Дополнительно: `propiedades.esFuenteLuz()` и `propiedades.esIndestructible()` — быстрые предикаты для логики движка.

### `MaterialBloque`

Enum-классификация из 11 значений: `AIRE`, `PIEDRA`, `MADERA`, `METAL`, `TIERRA`, `PLANTA`, `CRISTAL`, `TELA`, `LIQUIDO`, `EFIMERO`, `GENERICO`. Каждый материал несёт флаги `solido`, `inflamable`, `liquido`, `bloqueaMovimiento`, `traversable`, `permiteLuz`.

Зачем собственный enum, а не теги: tag-система 1.21.1 разбросана по datapack'ам и не годится compile-time для валидации. При переносе на следующий мажор Minecraft меняется только внутренний mapping `MaterialBloque → tags` — код модов остаётся как есть.

Builder `PropiedadesBloque.con(material)` подставляет согласованные дефолты:

- `LIQUIDO` → `replaceable=true`, `opacidad=1`, `forma=VACIO`;
- `AIRE`/`EFIMERO` → `replaceable=true`, `opacidad=0`, `forma=VACIO`;
- `CRISTAL`/`PLANTA` → `opacidad=0`;
- звук подбирается по материалу (`MADERA → SonidoBloque.madera()`, и т.д.).

### `FormaColision`

AABB-based форма. Два ключевых конструктора:

```java
FormaColision lleno = FormaColision.completo();             // 1×1×1
FormaColision vacia = FormaColision.vacio();                // ничего
FormaColision paso  = FormaColision.aabb(0, 0, 0, 1, 0.5, 1); // ступенька

FormaColision escalera = FormaColision.union(
        FormaColision.aabb(0, 0,   0, 1, 0.5, 1),
        FormaColision.aabb(0, 0.5, 0, 1, 1.0, 0.5));
```

Полезные запросы:

- `cajas()` — список всех `Caja`;
- `exterior()` — наименьший AABB, охватывающий все ящики;
- `contiene(x, y, z)` — быстрая проверка точки;
- `volumen()` — суммарный объём (без учёта перекрытий, для сортировок).

Ограничение: координаты каждой `Caja` должны лежать в `[-1..2]` по каждой оси. Это vanilla-лимит «блок не может выходить более чем на одну клетку в любую сторону». Выход за пределы бросает `IllegalArgumentException` в конструкторе.

VoxelShape-оптимизации (бит-маска 16×16×16) — в будущем `vida-mundo`, где соседи будут знать форму друг друга.

### `SonidoBloque`

`record` с шестью событиями (break / step / place / hit / fall / ambient) + громкость/тон. Идентификаторы — абстрактные `Identifier`; связь с vanilla `SoundEvent`-реестром — через bridge `vida-render` / `vida-mundo`.

Пресеты:

```java
SonidoBloque.piedra();   // minecraft:block.stone.*
SonidoBloque.madera();   // minecraft:block.wood.*
SonidoBloque.metal();    // minecraft:block.metal.*
SonidoBloque.hierba();   // minecraft:block.grass.*
SonidoBloque.cristal();  // minecraft:block.glass.*
SonidoBloque.arena();    // minecraft:block.sand.*
```

Кастомный профиль:

```java
SonidoBloque propio = SonidoBloque.uniforme(
        Identifier.of("ejemplo", "magico"), 0.8f, 1.2f);
```

### `NivelHerramienta` / `TipoHerramienta`

- `NivelHerramienta` — пирамида `NINGUNO → MADERA → PIEDRA → HIERRO → DIAMANTE → NETHERITA` с числовым `rango()` и методом `satisfechoPor(que)` для проверки «подойдёт ли данный инструмент».
- `TipoHerramienta` — семейство (`MANO`, `PICO`, `HACHA`, `PALA`, `AZADA`, `TIJERAS`, `ESPADA`).

Набор эффективных инструментов блока хранится в `PropiedadesBloque.herramientas()` — `Set<TipoHerramienta>`, иммутабельный.

### `BloqueEntidad<C>` + `ContextoBloqueEntidad`

Для блоков с per-position state (сундуки, печи, пользовательские структуры).

```java
public record ContextoHorno(int temperatura) implements ContextoBloqueEntidad {
    @Override public byte[] serializar()          { return ByteBuffer.allocate(4).putInt(temperatura).array(); }
    @Override public void deserializar(byte[] d)  { /* восстановление */ }
}

Bloque horno = new BloqueEntidad<>(
        Identifier.of("ejemplo", "horno_magico"),
        PropiedadesBloque.con(MaterialBloque.PIEDRA).ticking(true).construir(),
        () -> new ContextoHorno(20));
```

Формат сериализации — ответственность мода: `byte[] serializar()` + `void deserializar(byte[])`. Vida не навязывает NBT/JSON/binary; мост к vanilla save-формату — за `vida-mundo`.

## Регистрация

### `RegistroBloques`

Обёртка над `CatalogoManejador` из `vida-base`, типизированная по `Bloque`. Регистрация — только в фазе `INICIADO`, до `congelar()`.

```java
RegistroBloques reg = RegistroBloques.conectar(ctx.catalogos(), "ejemplo");

reg.registrarOExigir(
        new Bloque(
                Identifier.of("ejemplo", "piedra_oscura"),
                PropiedadesBloque.con(MaterialBloque.PIEDRA)
                        .dureza(2.0f)
                        .herramientas(TipoHerramienta.PICO)
                        .construir()),
        EtiquetaBloque.de("vida", "mineable/pico"),
        EtiquetaBloque.de("ejemplo", "oscuros"));
```

API:

- `registrar(Bloque)` / `registrar(Bloque, EtiquetaBloque...)` → `Result<Inscripcion<Bloque>, CatalogoError>`;
- `registrarOExigir(...)` — бросает `IllegalStateException` при провале;
- `etiquetar(Identifier, EtiquetaBloque)` — добавить в тег, не регистрируя блок сразу;
- `miembros(tag)` / `contiene(tag, id)` — чтение тегов;
- `obtener(Identifier)` / `obtener(path)` / `todos()` — чтение реестра;
- `congelar()` / `congelado()` — заморозка; после неё `registrar` вернёт `Err(...)`.

### `EtiquetaBloque`

Типизированный id тега: `EtiquetaBloque.de("vida", "mineable/pico")` или `EtiquetaBloque.de("ejemplo:oscuros")`. Аналог vanilla `TagKey<Block>`, но пополняемый модами через `RegistroBloques.etiquetar(...)`.

Vida не наследует список тегов из vanilla datapack'ов автоматически — это сознательно: datapack-слой считается side-specific и относится к `vida-mundo`.

## Потокобезопасность

- `Bloque`, `PropiedadesBloque`, `FormaColision`, `SonidoBloque` — иммутабельные, thread-safe by construction.
- `RegistroBloques` — thread-safe на чтение после заморозки; регистрации — только из фазы инициализации мода.
- `BloqueEntidad.fabrica()` — вызывается движком для каждой новой позиции; фабрика должна быть thread-safe, если движок создаёт контексты асинхронно (в 0.3.0 — только main-thread).

## Что ещё будет в `bloque`

- `BloqueEstado` — per-position state без BlockEntity (эквивалент vanilla `BlockState` property map).
- `DropsBloque` — декларативные loot-таблицы.
- Интеграция с `vida-mundo`: посадка блоков в мир, хит-тест по `FormaColision`.

Сроки — в [session-roadmap: 0.5.0 «Мир и сущности»](../session-roadmap.md).

## Тесты

`bloque/src/test/java` — JUnit 5 + AssertJ:

- `PropiedadesBloqueTest` — 11 сценариев валидации и дефолтов.
- `FormaColisionTest` — AABB, union, exterior, contiene.
- `SonidoBloqueTest` / `MaterialBloqueTest` / `NivelHerramientaTest` — инварианты enum'ов и пресетов.
- `RegistroBloquesTest` — регистрация, теги, заморозка, повторная регистрация → `CatalogoError`.
- `BloqueEntidadTest` — контекст, сериализация round-trip.

~95% покрытия.

## Что читать дальше

- [`objeto`](./objeto.md) — предметы, включая `ObjetoDeBloque` (BlockItem).
- [`base`](./base.md) — `CatalogoManejador`, в который пишет `RegistroBloques`.
- [session-roadmap](../session-roadmap.md#session-3--050--мир-и-сущности) — что появится вокруг блока в 0.5.x.
