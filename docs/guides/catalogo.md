# Catalogo — реестры контента

Типизированное API для регистрации блоков, предметов, звуков, рецептов, сущностей. Одна концепция — одна реализация, без разброса по разным «subregistry» как в других системах.

Базовые типы — в [`modules/base.md`](../modules/base.md#devvidabasecatalogo). Здесь — практические сценарии.

## Базовый сценарий

```java
@Override
public void iniciar(ModContext ctx) {
    Catalogo<Bloque> bloques = ctx.catalogos().obtener(Bloque.CATALOGO);

    bloques.registrar(
        CatalogoClave.de("miaventura", "espada_sagrada"),
        new Bloque(Bloque.Propiedades.piedra())
    );
}
```

Что здесь происходит:

1. `obtener(Bloque.CATALOGO)` — получили типизированный `Catalogo<Bloque>`. `Bloque.CATALOGO` — это `CatalogoHandle<Bloque>` (value-тип с именем реестра).
2. `CatalogoClave.de("miaventura", "espada_sagrada")` — namespaced-ключ. Namespace обязан совпадать с `id` мода (или быть в `provides` мода).
3. `registrar(...)` — добавляем запись. Бросает `IllegalStateException`, если ключ уже занят или реестр заморожен.

## Ключи

`CatalogoClave` — это валидированный `Identifier`:

- namespace: `[a-z0-9_.-]+`
- path: `[a-z0-9_./-]+`

Совпадение namespace с `ModContext.id()` проверяется в дебаге, на production — `ERROR` в лог, но регистрация проходит (на случай `provides`).

```java
CatalogoClave clave = CatalogoClave.de("miaventura", "mundo/espada_sagrada");
```

## Фаза `INICIADO` vs `ACTIVO`

Все регистрации **обязаны** происходить до первого тика игры. `Catalogo` явно тракает две фазы:

- **OPEN** — во время `VidaMod.iniciar(ctx)`. Разрешены `registrar(...)`.
- **FROZEN** — после. `registrar(...)` бросает `IllegalStateException`.

Заморозка происходит автоматически перед фазой `ACTIVO`. Это даёт:

- детерминированный порядок контента (важно для сетевой синхронизации);
- возможность оптимизировать внутренние структуры (упаковать записи в плотные массивы после заморозки).

## Чтение реестра

После заморозки можно читать:

```java
Bloque b = bloques.obtenerO(CatalogoClave.de("miaventura", "espada_sagrada"))
    .orElseThrow();

Set<CatalogoClave> claves = bloques.claves();
```

Итерация:

```java
bloques.forEach((clave, valor) -> {
    ctx.log().debug("{} → {}", clave, valor);
});
```

## Реестры Minecraft 1.21.1

Базовые vanilla-реестры (блоки, предметы, звуки) доступны через предопределённые `CatalogoHandle`:

```java
ctx.catalogos().obtener(Bloque.CATALOGO)   // блоки
ctx.catalogos().obtener(Objeto.CATALOGO)   // предметы
ctx.catalogos().obtener(Sonido.CATALOGO)   // звуки
// и так далее
```

Эти handle живут в API-модулях (`vida-bloque`, `vida-objeto`, `vida-sonido` — см. [roadmap](../roadmap.md)).

## Кастомные реестры

Мод может создавать свои реестры — например, если вы пишете библиотечный мод:

```java
public static final CatalogoHandle<MiTipo> MI_CATALOGO =
    CatalogoHandle.de("miaventura", "mis_cosas", MiTipo.class);

@Override
public void iniciar(ModContext ctx) {
    ctx.catalogos().crear(MI_CATALOGO);

    Catalogo<MiTipo> c = ctx.catalogos().obtener(MI_CATALOGO);
    c.registrar(CatalogoClave.de("miaventura", "cosa_uno"), new MiTipo(...));
}
```

`crear(...)` должен быть вызван хотя бы одним модом — после этого реестр виден всем. Если вызвали несколько модов одним handle'ом (например, через `provides`), конфликта нет — `crear` идемпотентен по handle'у.

## Data Components

Minecraft 1.21.1 ввёл `DataComponent`-систему поверх предметов. Vida работает с ней нативно через `Objeto.ComponentSet`:

```java
Objeto.ComponentSet components = Objeto.ComponentSet.vacio()
    .con(DataComponents.MAX_STACK_SIZE, 1)
    .con(DataComponents.DAMAGE, 100);

Objeto espada = new Objeto(new Objeto.Propiedades().componentes(components));
```

API `Objeto` — в модуле `vida-objeto` (в работе).

## Синхронизация клиент↔сервер

Catalogo-записи сериализуются как `CatalogoClave` (строка), поэтому клиент и сервер должны иметь один и тот же набор имён. Vida делает это автоматически:

- при handshake сервер отправляет список `(CatalogoHandle, содержимое)`;
- клиент сверяет со своим и помечает расхождения как `ERROR` (с возможностью отключения через `Ajustes`).

## Частые ошибки

### «already frozen»

Вы пытаетесь `registrar(...)` после `iniciar(...)`. Проверьте:

- что регистрация не в обработчике события (`LatidoPulso` — уже поздно);
- что не в ленивом инициализаторе (`static { ... }` может не успеть).

### «namespace mismatch»

`CatalogoClave.de("otromod", ...)` в `MiAventuraMod` — регистрация от имени чужого namespace. Используйте `provides`, если это осознанно.

### «handle not created»

Обращение к `obtener(MI_CATALOGO)` до `crear(MI_CATALOGO)`. Вызовите `crear` первым в том же `iniciar(...)`.

## Что читать дальше

- [modules/base.md](../modules/base.md#devvidabasecatalogo) — API reference.
- [guides/latidos.md](./latidos.md) — типовой сосед в коде.
- `vida-bloque` / `vida-objeto` Javadoc — когда эти модули будут опубликованы.
