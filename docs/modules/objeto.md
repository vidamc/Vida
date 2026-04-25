# objeto

API предметов: `Objeto` + `PropiedadesObjeto` + типизированная data-components-система 1.21.1 + инструменты. Side-agnostic, иммутабельный, без прямой зависимости от vanilla классов.

- Пакет: `dev.vida.objeto`
- Gradle: `dev.vida:vida-objeto`
- Стабильность: **`@ApiStatus.Stable`** с 1.0.0

Модуль появился в 0.3.0; публичный API заморожен по SemVer (см. [api-stability.md](../reference/api-stability.md)).

```kotlin
dependencies {
    compileOnly("dev.vida:vida-objeto:0.3.0")
}
```

Зависит от [`vida-bloque`](./bloque.md) ради `ObjetoDeBloque` (BlockItem) и `TipoHerramienta`.

## Главные типы

### `Objeto`

Data-объект: `Identifier` + `PropiedadesObjeto`.

```java
Objeto rubi = new Objeto(
        Identifier.of("ejemplo", "rubi"),
        PropiedadesObjeto.con()
                .tipo(TipoObjeto.MATERIAL)
                .raridad(Raridad.RARO)
                .construir());
```

Shortcuts:

- `id()`, `propiedades()`;
- `raridad()` — цвет имени (`Raridad.COMUN.colorRgb() == 0xFFFFFF`);
- `tipo()` — базовая категория.

### `PropiedadesObjeto`

`record` с fluent-билдером. Пять полей:

| Поле | Тип | Дефолт |
|------|-----|--------|
| `tipo` | `TipoObjeto` | `GENERICO` |
| `maxPila` | `int` | `64` (`1` для брони/инструмента) |
| `raridad` | `Raridad` | `COMUN` |
| `herramienta` | `Optional<Herramienta>` | empty |
| `componentes` | `MapaComponentes` | vacio |

Валидация в канонической форме:

- `maxPila` ∈ `[1..99]` (vanilla предел);
- `tipo=HERRAMIENTA` ⇒ `herramienta.isPresent()` и `maxPila=1`;
- выставление `.herramienta(...)` в билдере автоматически переключает `tipo` на `HERRAMIENTA` и `maxPila` на `1`.

```java
PropiedadesObjeto espadaProps = PropiedadesObjeto.con()
        .herramienta(Herramienta.espada(Material.HIERRO))
        .raridad(Raridad.INFRECUENTE)
        .componente(ClaveComponente.NOMBRE,
                new Componente.NombrePersonalizado("Hoja templada"))
        .construir();
```

### `TipoObjeto`

Enum из 9 категорий: `GENERICO`, `BLOQUE`, `HERRAMIENTA`, `ARMA`, `ARMADURA`, `CONSUMIBLE`, `CONTENEDOR`, `PROYECTIL`, `MATERIAL`. Подсказка для движка: `ARMA`/`ARMADURA`/`HERRAMIENTA` автоматически получают `maxPila=1` в билдере.

### `Raridad`

`COMUN` / `INFRECUENTE` / `RARO` / `EPICO` с RGB-цветом имени. Аналог vanilla `Rarity`; перенос в клиент — через bridge `vida-render`.

### `Herramienta` + `Material`

`Herramienta` — связка:

```java
record Herramienta(Material material,
                   Set<TipoHerramienta> tipos,
                   float danoAtaque,
                   float velocidadAtaque) {}
```

Фабрики для типичных инструментов:

```java
Herramienta pico   = Herramienta.pico(Material.HIERRO);
Herramienta hacha  = Herramienta.hacha(Material.DIAMANTE);
Herramienta pala   = Herramienta.pala(Material.PIEDRA);
Herramienta azada  = Herramienta.azada(Material.NETHERITA);
Herramienta espada = Herramienta.espada(Material.ORO);
```

`Material` — `record`, соответствующий vanilla `Tiers`:

| Константа | `nivelAddicion` | `durabilidad` | `velocidadAccion` | `danoBase` | `tasaEncantamiento` |
|-----------|-----------------|---------------|-------------------|------------|---------------------|
| `MADERA` | MADERA | 59 | 2.0 | 0.0 | 15 |
| `PIEDRA` | PIEDRA | 131 | 4.0 | 1.0 | 5 |
| `HIERRO` | HIERRO | 250 | 6.0 | 2.0 | 14 |
| `ORO` | MADERA | 32 | 12.0 | 0.0 | 22 |
| `DIAMANTE` | DIAMANTE | 1561 | 8.0 | 3.0 | 10 |
| `NETHERITA` | NETHERITA | 2031 | 9.0 | 4.0 | 15 |

Пользовательские материалы — `Material.personalizado(name, nivel, dur, vel, dano, enc)`.

### `ObjetoDeBloque`

BlockItem: предмет-обёртка над `Bloque`. По дефолту id совпадает с id блока.

```java
Bloque piedra = /* ... */;
ObjetoDeBloque item = ObjetoDeBloque.de(piedra);
// item.id() == piedra.id()
// item.tipo() == TipoObjeto.BLOQUE

// Кастомные свойства:
ObjetoDeBloque rara = ObjetoDeBloque.de(piedra,
        PropiedadesObjeto.con()
                .tipo(TipoObjeto.BLOQUE)
                .raridad(Raridad.RARO)
                .maxPila(16)
                .construir());

// Полный override id:
ObjetoDeBloque aliased = ObjetoDeBloque.conId(piedra,
        Identifier.of("ejemplo", "piedra_en_inventario"),
        PropiedadesObjeto.con().tipo(TipoObjeto.BLOQUE).construir());
```

## Data-components (`dev.vida.objeto.componentes`)

Vida 1.21.1 целиком уехал на data-components вместо NBT. Модуль `objeto` даёт типизированную карту:

### `Componente` — sealed иерархия

Все известные vanilla-компоненты как записи (record). `sealed` — чтобы при обновлении на новый мажор Minecraft пропущенный компонент виден как compile-error в `switch`-pattern, а не как silent-missing-field.

Перечень в 0.3.0:

| Запись | vanilla-компонент | Поля |
|--------|-------------------|------|
| `DatosModeloPersonalizados` | `minecraft:custom_model_data` | `int valor` |
| `Irrompible` | `minecraft:unbreakable` | `boolean mostrarEnTooltip` |
| `Durabilidad` | `minecraft:damage` + `max_damage` | `int dano`, `int maximo` |
| `NombrePersonalizado` | `minecraft:custom_name` | `String texto` |
| `Lore` | `minecraft:lore` | `List<String> lineas` |
| `MaxPila` | `minecraft:max_stack_size` | `int tamanio` |
| `Comida` | `minecraft:food` | `saciedad`, `saturacion`, `puedeSiempre`, `tiempoComer` |
| `EncantamientoOculto` | `minecraft:hide_tooltip` | — |
| `FuegoResistente` | `minecraft:fire_resistant` | — |
| `BrilloEncantado` | `minecraft:enchantment_glint_override` | `boolean mostrar` |
| `PerfilJugador` | `minecraft:profile` | `String nombre`, `Optional<UUID> uuid` |
| `Raro` | `minecraft:rarity` | `Raridad` |
| `AtributosModificados` | `minecraft:attribute_modifiers` | `List<ModificadorAtributo>` |
| `ColorTinte` | `minecraft:dyed_color` | `int rgb` (0xRRGGBB) |

Каждая запись валидируется в каноническом конструкторе (диапазоны, null-проверки).

### `ClaveComponente<T>`

Типизированный ключ: связь текстового `minecraft:food` с конкретным типом `Componente.Comida`. Vanilla-ключи объявлены константами:

```java
ClaveComponente.DATOS_MODELO       // minecraft:custom_model_data
ClaveComponente.IRROMPIBLE         // minecraft:unbreakable
ClaveComponente.DURABILIDAD        // minecraft:damage
ClaveComponente.NOMBRE             // minecraft:custom_name
ClaveComponente.LORE               // minecraft:lore
ClaveComponente.MAX_PILA           // minecraft:max_stack_size
ClaveComponente.COMIDA             // minecraft:food
ClaveComponente.FUEGO_RESISTENTE   // minecraft:fire_resistant
ClaveComponente.BRILLO             // minecraft:enchantment_glint_override
ClaveComponente.PERFIL             // minecraft:profile
ClaveComponente.RARIDAD            // minecraft:rarity
ClaveComponente.ATRIBUTOS          // minecraft:attribute_modifiers
ClaveComponente.COLOR              // minecraft:dyed_color
```

Свой ключ: `ClaveComponente.de("miaventura:custom_stat", MiStat.class)`.

### `MapaComponentes`

Иммутабельная карта `ClaveComponente<T> → T`. Создаётся через fluent-билдер, читается через типизированный `Optional`:

```java
MapaComponentes mapa = MapaComponentes.con()
        .poner(ClaveComponente.COMIDA,
                new Componente.Comida(6, 0.8f, false, 32))
        .poner(ClaveComponente.NOMBRE,
                new Componente.NombrePersonalizado("Manzana de jade"))
        .construir();

Optional<Componente.Comida> comida = mapa.obtener(ClaveComponente.COMIDA);
// comida.get().saciedad() == 6
```

API:

- `tamanio()`, `esVacio()`, `contiene(clave)`;
- `obtener(clave)` — типизированный `Optional<T>`;
- `fusionar(otra)` — merge, значения `otra` побеждают, возвращает новый объект;
- `valores()` / `ids()` — итерация.

Класс иммутабельный — `fusionar` всегда возвращает новый экземпляр.

## Регистрация

### `RegistroObjetos`

Зеркало `RegistroBloques`:

```java
RegistroObjetos reg = RegistroObjetos.conectar(ctx.catalogos(), "ejemplo");

reg.registrarOExigir(rubi,
        EtiquetaObjeto.de("ejemplo", "materiales"));

Optional<Objeto> r = reg.obtener("rubi");
```

API (см. [`bloque.md`](./bloque.md#регистрация) для зеркального поведения): `registrar` / `registrarOExigir` / `etiquetar` / `miembros` / `obtener` / `todos` / `congelar`. Все теги хранятся в том же formate — `Map<Identifier, Set<Identifier>>` и пополняются через `etiquetar(...)`.

### `EtiquetaObjeto`

`EtiquetaObjeto.de("ejemplo", "comida_magica")` или `EtiquetaObjeto.de("ejemplo:comida_magica")`.

## Почему собственная data-component-система

vanilla 1.21.1 `DataComponentType<T>` полноценен, но:

1. Требует JVM-классов `net.minecraft.core.component.*` — они живут в классе-лоадере `juego`, моды их компилируют только через remap-слой. Мы хотим, чтобы мод собирался без Minecraft на компайл-пасе.
2. `sealed interface Componente` даёт compile-time проверку списка компонентов в `switch`-expression — critical для обновлений.
3. Наш `MapaComponentes` иммутабельный и thread-safe by construction; vanilla `DataComponentMap` — не всегда.

Bridge к vanilla `DataComponentMap` будет в `vida-mundo` (0.5.x) — там по `ClaveComponente.id()` найдётся vanilla `DataComponentType`, а `Componente`-запись сериализуется в соответствующий codec.

## Потокобезопасность

- `Objeto`, `PropiedadesObjeto`, `Herramienta`, `Material`, все `Componente.*` — иммутабельны, thread-safe.
- `MapaComponentes` — иммутабельна, `fusionar` возвращает новый объект.
- `MapaComponentes.Constructor` — не thread-safe (не используйте из нескольких потоков одновременно, как любой билдер).
- `RegistroObjetos` — thread-safe на чтение после `congelar()`.

## Тесты

`objeto/src/test/java`:

- `PropiedadesObjetoTest` — валидация, дефолты, auto-тип для инструментов.
- `ComponenteTest` — конструкторы всех 14 записей + инварианты полей.
- `MapaComponentesTest` — `poner`/`quitar`/`fusionar`/`obtener`/`contiene`.
- `ClaveComponenteTest` — равенство ключей, типизация.
- `HerramientaTest` / `MaterialTest` — фабрики, предел значений.
- `ObjetoDeBloqueTest` — id-matching, override.
- `RegistroObjetosTest` — регистрация, теги, заморозка.

~92% покрытия.

## Что читать дальше

- [`bloque`](./bloque.md) — что именно упаковывает `ObjetoDeBloque`.
- [`base`](./base.md) — `CatalogoManejador`.
- [session-roadmap: 0.5.0](../session-roadmap.md#session-3--050--мир-и-сущности) — когда появятся loot-таблицы, крафты, inventory-API.
