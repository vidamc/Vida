# Ajustes — конфиги

Типизированное API для настроек мода. Схема описывается в коде, данные — в TOML-файле рядом с модом.

Базовые типы — в `[modules/config.md](../modules/config.md)` и `[modules/base.md](../modules/base.md#devvidabaseajustes)`. Здесь — практика.

## Базовый сценарий

### 1. Описываем схему

```java
public final class MiAjustes {
    public static final Ajuste<Integer> RENDER_DISTANCE =
        Ajuste.entero("render.distance", 32).min(2).max(64);

    public static final Ajuste<Boolean> DEBUG_HUD =
        Ajuste.booleano("debug.hud", false);

    public static final Ajuste<String> PREFIX =
        Ajuste.cadena("mensajes.prefijo", "[MiMod] ")
            .validar(s -> s.length() <= 32, "prefix too long");
}
```

`Ajuste<T>` — value-тип. Определяйте их в отдельном классе как константы.

Доступные фабрики:

- `Ajuste.entero(path, default)` → `Ajuste<Integer>` с `.min(...)` / `.max(...)`
- `Ajuste.largo(path, default)` → `Ajuste<Long>`
- `Ajuste.real(path, default)` → `Ajuste<Double>`
- `Ajuste.booleano(path, default)` → `Ajuste<Boolean>`
- `Ajuste.cadena(path, default)` → `Ajuste<String>` с `.validar(...)` / `.patron(...)`
- `Ajuste.listaDeCadenas(path, default)` → `Ajuste<List<String>>`
- `Ajuste.enumeracion(path, default, Enum.class)` → `Ajuste<E>`

### 2. Читаем значения

```java
@Override
public void iniciar(ModContext ctx) {
    int distance = ctx.ajustes().valor(MiAjustes.RENDER_DISTANCE);
    boolean hud = ctx.ajustes().valor(MiAjustes.DEBUG_HUD);

    ctx.log().info("render={}, hud={}", distance, hud);
}
```

При первом обращении:

1. Читается значение из TOML-файла по пути `render.distance`.
2. Если отсутствует — возвращается дефолт (`32`).
3. Проверяются `min`/`max` / `validar`; при нарушении — `WARN` в лог, fallback к дефолту.

## TOML-файл

По умолчанию — `<config>/miaventura.toml`:

```toml
[render]
distance = 48

[debug]
hud = true

[mensajes]
prefijo = "[Mi Mod] "
```

Путь `render.distance` матчит секцию `[render]` + ключ `distance`. Глубина произвольная.

## Профили

Встроенная поддержка через `[profile.<name>]`:

```toml
[render]
distance = 32

[profile.performance.render]
distance = 16

[profile.eyecandy.render]
distance = 64
```

Активация:

```bash
java -Dvida.profile=performance -javaagent:... -jar ...
```

Накладывается overlay'ем поверх корневых значений — массивы и скаляры заменяются целиком, таблицы сливаются по правилам `[ConfigMerger](../modules/config.md#configmerger)`.

## Внешний overlay

Для cli/launcher'ов, которые хотят переопределить настройки без редактирования основного файла:

```bash
java -Dvida.ajustes.overlay=/path/to/override.toml ...
```

`override.toml`:

```toml
[render]
distance = 8
```

Порядок: `base.toml` → `[profile.<active>]` → `overlay`.

## Валидация

### Встроенные

```java
Ajuste.entero("render.distance", 32)
    .min(2).max(64);

Ajuste.cadena("mensajes.prefijo", "[Mod] ")
    .patron("\\[[A-Za-z ]+\\] ");

Ajuste.enumeracion("render.modo", RenderModo.NORMAL, RenderModo.class);
```

### Кастомные

```java
Ajuste.cadena("api.url", "https://api.example.com")
    .validar(
        url -> url.startsWith("https://"),
        "API URL must use HTTPS"
    );
```

Валидатор вызывается один раз при первом чтении; если кидает `false` — сообщение логгируется и значение заменяется дефолтом.

## Обработка ошибок

Подход «дефолт при невалидном значении» — сознательный: у игрока не должен ломаться мод от того, что он набил опечатку в TOML. Вместо этого:

- `INFO` — значение прочитано из файла (в `DEBUG`-уровне);
- `WARN` — значение невалидно, используем дефолт (полный путь + причина);
- `ERROR` — фатальная ошибка в самом файле (синтаксис TOML), мод не поднимается.

Если вам нужна strict-семантика (падать при невалидном конфиге), используйте Result-путь:

```java
Result<Integer, AjustesError> r = ctx.ajustes().intentarValor(MiAjustes.RENDER_DISTANCE);
if (r.isErr()) {
    throw new IllegalStateException("invalid config: " + r.error());
}
```

## Перезагрузка

После успешного бутстрапа Vida эмитит **`LatidoConfiguracionRecargada`** (тип `dev.vida.base.latidos.eventos.LatidoConfiguracionRecargada`) при перезагрузке ресурсов клиента, в т.ч. пути F3+T (см. платформенный мост загрузчика). Подписка — как на любой `Latido`:

```java
ctx.latidos().suscribir(LatidoConfiguracionRecargada.TIPO, Prioridad.NORMAL, evento -> {
    int newDistance = ctx.ajustes().valor(MiAjustes.RENDER_DISTANCE);
    // применить
});
```

Команда `/vida reloadconfig` (если включена в сборке) должна использовать тот же контракт через `LatidoGlobal` / мост, без дублирования логики в каждом моде.

## Синхронизация клиент↔сервер

Не все настройки — клиентские. Для тех, что должны совпадать на обеих сторонах:

```java
public static final Ajuste<Integer> TICK_RATE =
    Ajuste.entero("world.tickRate", 20)
        .min(1).max(100)
        .sincronizar();  // серверное значение приходит клиенту при handshake
```

При `sincronizar()` сервер шлёт клиенту снимок этих значений; клиент локально переопределяет свой файл на время сессии. После дисконнекта — возврат к своим настройкам.

Поле wire для серверного снимка — `PaqueteAjustesSincronizacionServidor` в `dev.vida.red` (только объявленные пути `Ajuste`). Каталог путей регистрируется через `AjustesSincronizacionCatalogo`.

## Частые ошибки

- **Ajuste-константа в экземплярном поле.** Делайте `public static final` — иначе `Ajuste` создаётся на каждом обращении (аллокация на каждом чтении настройки).
- **Чтение Ajustes до `iniciar`.** `ctx.ajustes()` ещё не доступен. Не делайте это в `static {}` блоках.
- **Валидация с побочным эффектом.** Валидатор вызывается лениво и один раз; не полагайтесь на «срабатывание» валидатора как на триггер.

## Что читать дальше

- [modules/config.md](../modules/config.md) — устройство модуля.
- [modules/base.md](../modules/base.md#devvidabaseajustes) — реализация `AjustesTipados`.
- [glossary.md#ajustes](../glossary.md#ajustes) — этимология.

