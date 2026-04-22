# config — Ajustes

Типизированный конфиг-движок Vida. На выходе — иммутабельный снимок с dotted-path доступом и строгой валидацией.

- Пакет: `dev.vida.config`
- Gradle: `dev.vida:vida-config`
- Стабильность: `@ApiStatus.Stable`
- Имя в проекте: **Ajustes** (см. [глоссарий](../glossary.md#ajustes))

Практическое руководство для мод-автора — [guides/ajustes.md](../guides/ajustes.md). Эта страница — про модуль.

## Главные типы

### `ConfigNode`

Нейтральное дерево: `Table(Map<String, ConfigNode>)`, `Array(List<ConfigNode>)`, `Scalar(Object)`. Не привязано к формату — может быть построено из TOML, JSON, YAML, кода. Все реализации иммутабельны.

```java
ConfigNode tree = ConfigNode.table(Map.of(
    "render", ConfigNode.table(Map.of(
        "distance", ConfigNode.scalar(32)
    ))
));
```

### `ConfigMerger`

Чистое глубокое слияние таблиц. Правила:

- таблица `+` таблица → merge с приоритетом правой стороны;
- массив `+` массив → **замена целиком** (не concat — сознательно, чтобы избежать магии);
- скаляр `+` что угодно → замена.

```java
ConfigNode base = ...;
ConfigNode overlay = ...;
ConfigNode merged = ConfigMerger.merge(base, overlay);
```

Используется для профилей (`[profile.dev]` поверх дефолтов) и внешних overlay (`--config-overlay file.toml`).

### `Ajustes`

Фасад, через который мод получает значения. Иммутабельный снимок + dotted-path + четыре варианта доступа.

```java
Ajustes ajustes = ...;

// strict — бросит IllegalStateException, если нет или тип не тот
int distance = ajustes.requireInt("render.distance");

// optional — Optional<Integer>
OptionalInt lazy = ajustes.optionalInt("render.lazy");

// with default
int fallback = ajustes.intOr("render.distance", 16);

// Result-style — для кода, который предпочитает Result
Result<Integer, AjustesError> r = ajustes.getInt("render.distance");
```

Поддерживаемые типы скаляров: `boolean`, `int`, `long`, `double`, `String`. Для списков: `intList`, `stringList`, etc. Для подtable: `ajustes.table("render")` возвращает новый `Ajustes` со сдвинутым корнем.

### `AjustesLoader`

Fluent-загрузчик. Типовой сценарий:

```java
Ajustes a = AjustesLoader.create()
    .loadToml(Path.of("config/miaventura.toml"))
    .overlayToml(Path.of("config/miaventura.dev.toml"))  // для dev-профиля
    .profile("dev")                                       // применить [profile.dev]
    .load()
    .orElseThrow();  // Result<Ajustes, AjustesError>
```

Профили — это секция `[profile.<name>]` в том же TOML-файле. Её содержимое накладывается overlay'ем при `profile("<name>")`. Это «встроенная» альтернатива отдельным файлам:

```toml
[render]
distance = 32

[profile.dev.render]
distance = 64
```

### `AjustesError`

Типизированные ошибки: `ParseError`, `MissingKey(String path)`, `TypeMismatch(String path, String expected, String actual)`, `ValidationError(String path, String message)`.

## Типизированные настройки (`AjustesTipados`)

Поверх `Ajustes` модуль `base` предоставляет `AjustesTipados` и `Ajuste<T>` — «схему» настройки с валидацией.

```java
Ajuste<Integer> RENDER_DIST = Ajuste.entero("render.distance", 32).min(2).max(64);

int d = ctx.ajustes().valor(RENDER_DIST);  // читает и валидирует
```

При первом обращении к `RENDER_DIST`:

1. Читается значение по пути `render.distance`.
2. Если отсутствует — возвращается дефолт (`32`).
3. Проверяются `min`/`max` — при нарушении лог `WARN` и fallback к дефолту.

`Ajuste` — value-type; определяйте их как константы в классах мода. Подробнее — [guides/ajustes.md](../guides/ajustes.md).

## TOML под капотом

Парсер — `tomlj 1.1.x`, изолированный в `dev.vida.config.internal`. Если вам нужен доступ к исходному TOML-дереву, не используйте его напрямую — это `@Internal`. Мы оставляем за собой право заменить `tomlj` без breaking-changes в публичном API `Ajustes`.

## Почему не JSON / YAML

- **JSON** — не человекочитаем (нет комментариев, требует запятых, кавычек). Для модов, которых правят руками, это боль.
- **YAML** — слишком сложный (anchors, multi-document, тонкости отступов). Один indentation bug ломает конфиг.
- **TOML** — простой, хорошо читается, хорошо пишется, стабильный.

Для межпроцессного обмена (манифесты, сетевые сообщения) JSON уместен; для конфигов, которые правит человек, — TOML.

## Потокобезопасность

`Ajustes` иммутабелен, поэтому _thread-safe by construction_. Перезагрузка конфига (на данный момент — только при рестарте Vida) означает создание нового экземпляра, который заменяет старый атомарной ссылкой в `ModContext`.

Горячая перезагрузка по F3+T / `/reload` — запланировано; будет срабатывать через `LatidoAjustesCambiados`.

## Тесты и контракты

- Purity `ConfigMerger.merge(...)`: не мутирует входы, `merge(merge(a, b), c) == merge(a, merge(b, c))` только если ассоциативность соблюдена (проверяется в тестах).
- Идемпотентность: `merge(a, a) == a`.
- Round-trip TOML→`ConfigNode`→TOML через reference-парсер (проверка, что мы не теряем информацию).
