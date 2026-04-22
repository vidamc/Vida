# manifest

Схема `vida.mod.json` и её парсер. Живёт на границе между «мир пользователя» (JSON-файл в JAR мода) и «мир Vida» (строго типизированная `ModManifest`).

- Пакет: `dev.vida.manifest`
- Gradle: `dev.vida:vida-manifest`
- Стабильность: `@ApiStatus.Stable`

## Что это

`vida.mod.json` — единственный обязательный файл мода Vida. Минимальный:

```json
{
  "schema": 1,
  "id": "miaventura",
  "version": "0.1.0",
  "name": "Mi Aventura"
}
```

Полная схема — [reference/manifest-schema.md](../reference/manifest-schema.md). Здесь — только про модуль.

## Ключевые типы

### `ModManifest`

Иммутабельный `record` с разобранным содержимым. Обязательные поля — `schema`, `id`, `version`, `name`; остальное — `Optional<>` или пустые коллекции по умолчанию.

```java
ModManifest mf = ModManifest.builder("miaventura", Version.parse("0.1.0"), "Mi Aventura")
    .description("Пример мода")
    .authors(List.of(new ModAuthor("Ana", null, null)))
    .build();
```

Валидация идёт в compact-конструкторе: `id` должен матчить `[a-z0-9_.-]+`, `name` — не пустой, коллекции — копируются в `List.copyOf` для иммутабельности.

### `ManifestParser`

Чтение JSON → `ModManifest`. Использует собственный JSON-парсер (`dev.vida.manifest.json.VidaJson`) — без Jackson/Gson.

```java
Result<ModManifest, ManifestError> r = ManifestParser.parse(bytes);
switch (r.asEither()) {
    case Left(ManifestError err) -> log.error("невалидный манифест: {}", err);
    case Right(ModManifest mf)   -> registerMod(mf);
}
```

### `ManifestError`

Типизированная иерархия ошибок: `Syntax`, `MissingField(String path)`, `InvalidField(String path, String reason)`, `UnsupportedSchema(int seen, int supported)`. Даёт диагностируемые сообщения — важно, потому что у игрока мод часто «просто не грузится».

## Почему свой JSON-парсер

Три причины:

1. **Ранняя фаза.** `manifest` работает до того, как мы настроили classloading модов. Любая внешняя зависимость нагружает boot-classpath.
2. **Строгость.** Наш парсер отклоняет trailing-запятые, комментарии, неявное `undefined`-поведение — мод-авторы должны писать валидный JSON.
3. **Размер.** Vida — проект про производительность; лишняя мегабайтная библиотека на старте неуместна.

Парсер находится в подпакете `dev.vida.manifest.json`. Он `@ApiStatus.Internal`; если вы хотите парсить JSON из своего мода, используйте любую библиотеку по вкусу.

## Интеграция с Gradle-плагином

Плагин `dev.vida.mod` (см. [gradle-plugin.md](./gradle-plugin.md)) содержит задачи:

- `vidaGenerateManifest` — рендерит `vida.mod.json` из DSL в `build/generated/vida/resources`.
- `vidaValidateManifest` — прогоняет полученный JSON через `ManifestParser.parse(...)`. Если результат `Err`, сборка падает до компиляции Kotlin/Java.

Это даёт моддеру фидбэк «на красном», а не в рантайме, и исключает невалидные JAR'ы в продакшене.

## `VifadaConfig` и `ModDependencies`

Вспомогательные records, живут в том же пакете:

- `ModDependencies.required/optional/incompatible` — списки пар `(id, range)`.
- `VifadaConfig.morphs` — список FQCN классов, отмеченных `@VifadaMorph`. Резолвер Vifada использует это как white-list.
- `ModEntrypoints.main/client/server/preLaunch` — FQCN классов, реализующих `VidaMod`. На данный момент используется только `main`.

## Эволюция схемы

Поле `schema` позволяет версионировать формат. Сейчас поддерживается `schema = 1`. Появление `schema = 2` означает breaking change в формате; парсер будет знать об обеих версиях и маршалить старый формат в новый внутренне.

Мод-автор смотрит на `schema` редко: Gradle-плагин ставит актуальный номер автоматически.

## Что читать дальше

- Полная схема полей — [reference/manifest-schema.md](../reference/manifest-schema.md).
- Как использовать из Gradle — [modules/gradle-plugin.md](./gradle-plugin.md).
- Как моды оказываются в дискавери — [modules/discovery.md](./discovery.md).
