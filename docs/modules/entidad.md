# entidad

API сущностей: `Entidad` + `PropiedadesEntidad` + типизированные data-components для entity-types. Как и `bloque`/`objeto`, модуль deliberately side-agnostic: здесь нет прямой зависимости на vanilla `EntityType`, `MobCategory` или runtime-классы игры.

- Пакет: `dev.vida.entidad`
- Gradle: `dev.vida:vida-entidad`
- Стабильность: `@ApiStatus.Preview("entidad")`

`vida-entidad` появился в `0.5.0`. Его задача — дать авторам модов декларативную модель сущности, не заставляя тянуть Minecraft-классы на compile classpath.

```kotlin
dependencies {
    compileOnly("dev.vida:vida-entidad:0.5.0")
}
```

## Главные типы

### `Entidad`

Декларативный descriptor сущности: `Identifier` + `TipoEntidad` + `PropiedadesEntidad`.

```java
Entidad cierva = new Entidad(
        Identifier.of("bosque", "cierva"),
        TipoEntidad.CRIATURA,
        PropiedadesEntidad.con()
                .masa(180.0)
                .hitbox(0.9, 1.4, 0.9)
                .grupoIa(PropiedadesEntidad.GrupoIa.TERRESTRE)
                .grupoIa(PropiedadesEntidad.GrupoIa.HERBIVORO)
                .construir());
```

Гарантии:

- `Entidad` иммутабельна;
- `equals` / `hashCode` учитывают `id`, `tipo` и `propiedades`;
- shortcuts `masa()`, `hitbox()` и `componentes()` позволяют не нырять каждый раз в `propiedades()`.

### `TipoEntidad`

Базовая категория сущности. В `0.5.0` есть набор типовых bucket'ов:

- `CRIATURA`
- `MONSTRUO`
- `AMBIENTAL`
- `ACUATICA`
- `UTILIDAD`
- `PROYECTIL`
- `JEFE`
- `MISCELANEA`

У каждого типа есть предикаты вроде `esHostil()` и `esViva()`, чтобы код мода не сравнивал enum-константы вручную.

### `PropiedadesEntidad`

Иммутабельный `record` с fluent-билдером `PropiedadesEntidad.con()`.

Поля `0.5.0`:

| Поле | Тип | Смысл |
|------|-----|-------|
| `masa` | `double` | Масса в условных единицах Vida; используется для knockback/physics-bridge |
| `hitbox` | `Hitbox` | Трёхмерный bounding-box (`ancho`, `alto`, `profundidad`) |
| `gruposIa` | `Set<GrupoIa>` | Семейства поведения: `TERRESTRE`, `VOLADOR`, `NADADOR`, `HOSTIL`, ... |
| `componentes` | `MapaComponentesEntidad` | Типизированные data-components entity-type |

```java
PropiedadesEntidad props = PropiedadesEntidad.con()
        .masa(24.0)
        .hitbox(0.6, 1.95, 0.6)
        .grupoIa(PropiedadesEntidad.GrupoIa.TERRESTRE)
        .grupoIa(PropiedadesEntidad.GrupoIa.HOSTIL)
        .componente(ClaveComponenteEntidad.SALUD,
                new ComponenteEntidad.Salud(40.0, 40.0))
        .componente(ClaveComponenteEntidad.BRILLO,
                new ComponenteEntidad.Brillo(false))
        .construir();
```

Валидация живёт в канонической форме:

- `masa > 0`;
- размеры `hitbox` строго положительные;
- `gruposIa` и `componentes` никогда не `null`;
- дублирующиеся компоненты заменяются по ключу, как в vanilla data-components.

Полезные shortcut'ы:

- `tieneIa()` — есть ли хотя бы одна AI-группа;
- `volumenHitbox()` — быстрый объём коллизии;
- `hitbox()` — готовый value-type hitbox без привязки к runtime-миру.

## Data-components (`dev.vida.entidad.componentes`)

Сущности в Minecraft 1.21.1 тоже всё чаще описываются через data-components, поэтому Vida даёт отдельную типизированную карту, не смешивая item- и entity-компоненты.

### `ComponenteEntidad`

`sealed interface` с набором стандартных записей `0.5.0`:

- `Salud(actual, maxima)`
- `VelocidadMovimiento(bloquesPorSegundo)`
- `NombreVisible(texto)`
- `Brillo(visible)`
- `InmuneFuego()`
- `TablaBotin(id)`

Набор небольшой сознательно: мы закрываем самые частые case'ы и оставляем место для расширения без привязки к vanilla internals.

### `ClaveComponenteEntidad<T>`

Типизированный ключ компонента. Встроенные константы соответствуют стандартным компонентам `ComponenteEntidad`, например:

```java
ClaveComponenteEntidad.SALUD
ClaveComponenteEntidad.VELOCIDAD
ClaveComponenteEntidad.NOMBRE_VISIBLE
ClaveComponenteEntidad.BRILLO
ClaveComponenteEntidad.INMUNE_FUEGO
ClaveComponenteEntidad.TABLA_BOTIN
```

Свой ключ:

```java
ClaveComponenteEntidad.de("bosque:agresion", ComponenteAgresion.class)
```

### `MapaComponentesEntidad`

Иммутабельная карта `ClaveComponenteEntidad<T> -> T` с API:

- `vacio()`
- `con()`
- `contiene(clave)`
- `obtener(clave)`
- `fusionar(otra)`
- `ids()` / `valores()`

Поведение совпадает с `vida-objeto`: одна запись на ключ, типизированный `Optional` на чтение, builder на запись.

## Регистрация

### `RegistroEntidades`

Типизированная обёртка над `CatalogoManejador`.

```java
RegistroEntidades entidades = RegistroEntidades.conectar(ctx.catalogos(), "bosque");
entidades.registrarOExigir(cierva);
```

API:

- `registrar(Entidad)` -> `Result<Inscripcion<Entidad>, CatalogoError>`
- `registrarOExigir(Entidad)`
- `obtener(Identifier)` / `obtener(path)`
- `todos()`
- `congelar()` / `congelado()`

Контракт такой же, как у `RegistroBloques` и `RegistroObjetos`: записи добавляются только на этапе инициализации, после `congelar()` модуль переходит в read-only.

## Потокобезопасность

- `Entidad`, `PropiedadesEntidad`, `Hitbox`, все `ComponenteEntidad.*` и `MapaComponentesEntidad` — иммутабельны и thread-safe by construction.
- `RegistroEntidades` — thread-safe на чтение после заморозки.
- Builder'ы (`PropiedadesEntidad.Constructor`, `MapaComponentesEntidad.Constructor`) не thread-safe.

## Что читать дальше

- [`mundo`](./mundo.md) — как сущности появляются в мире и какие world-латидосы доступны.
- [`guides/first-entity.md`](../guides/first-entity.md) — минимальный мод с первой сущностью.
- [`objeto`](./objeto.md) — item data-components и общая философия immutable API.
