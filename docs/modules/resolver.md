# resolver

Резолвер зависимостей модов. Принимает набор `ModCandidate` и корневой набор требуемых `id`, возвращает либо валидную `Resolution`, либо структурированную `ResolverError`.

- Пакет: `dev.vida.resolver`
- Gradle: `dev.vida:vida-resolver`
- Стабильность: `@ApiStatus.Stable`

## Модель

### `Provider`

Один мод, одна версия, «кандидат» с точки зрения резолвера.

```java
record Provider(
    String id,
    Version version,
    List<String> provides,       // aliases (например, "minecraft:blocks")
    List<Dependency> dependencies
) { ... }
```

### `Dependency`

Одна запись требования:

```java
record Dependency(String targetId, VersionRange range, DependencyKind kind) { ... }

enum DependencyKind { REQUIRED, OPTIONAL, INCOMPATIBLE }
```

- `REQUIRED` — должен быть выбран провайдер, удовлетворяющий `range`.
- `OPTIONAL` — если выбран, должен соответствовать `range`; если не выбран — ок.
- `INCOMPATIBLE` — НЕ должен быть выбран провайдер в диапазоне.

### `Universe`

Каталог всех известных `Provider`'ов. Строится один раз из дискавери (через `ManifestAdapter`) либо из тестовых фикстур.

### `ResolverOptions`

Флажки поведения:

| Поле | Что делает |
|------|-----------|
| `preferNewest` | при конфликте выбирать более новую версию (дефолт `true`) |
| `pins` | мапа `id → Version`, жёстко фиксирует выбор |
| `timeoutMs` | лимит по времени |
| `solutionLimit` | сколько решений перебрать перед fail'ом |

### `Resolution`

Результат:

```java
record Resolution(
    Map<String, Provider> selected,  // id → выбранный Provider
    List<Dependency> optionalMisses  // OPTIONAL, которые не сошлись
) { ... }
```

### `ResolverError`

Структурированные неудачи:

- `MissingRequired(String id, VersionRange range, String byMod)` — мод `byMod` требует `id` в диапазоне, провайдера нет.
- `VersionConflict(String id, List<(String, VersionRange)> demanders)` — несколько модов требуют несовместимые диапазоны одного `id`.
- `Incompatibility(String byMod, Dependency dep, Provider match)` — `byMod` объявил `INCOMPATIBLE`, а `match` всё равно выбран.
- `LimitExceeded(String kind, long limit)` — таймаут или перебор.
- `CycleDetected(List<String> cycle)` — циклическая зависимость (через REQUIRED); мягкие циклы разрешены.

## Алгоритм

Классический хронологический бэктрекинг с unit-пропагацией диапазонов:

```
stack = [initial constraints from roots]
selected = {}

loop:
    if stack empty: return SUCCESS(selected)
    pick most-constrained variable X
    for each candidate in X.candidates (preferNewest order):
        tentative = selected ∪ {X: candidate}
        add candidate.dependencies to stack
        if unit-propagation fails: backtrack
        if all units satisfied: recurse
    if no candidate works: mark X as FAILED and backtrack
```

Unit-propagation: если для `id` остался ровно один провайдер, удовлетворяющий всем активным ограничениям, мы выбираем его без перебора. Это типично резолвит 80% зависимостей без ветвления.

Лимиты (`timeoutMs`, `solutionLimit`) защищают от патологических входов: если у моддера 50 модов с диапазонами `">=0.0.0"`, пространство решений экспоненциально, и мы честно говорим «не смогли за отведённое время».

## `ManifestAdapter`

Мост от `manifest` к `resolver`:

```java
Universe universe = ManifestAdapter.toUniverse(discoveryReport.successes());
Set<String> roots = manifests.stream().map(ModManifest::id).collect(toSet());

Result<Resolution, ResolverError> r = Resolver.resolve(universe, roots, ResolverOptions.defaults());
```

Преобразует:

- `ModManifest.id + version` → `Provider.id + version`;
- `ModManifest.dependencies.required/optional/incompatible` → список `Dependency`.

## `provides` и alias'ы

Мод может заявить себя как поставщика альтернативных `id`:

```json
{
  "id": "miaventura",
  "version": "1.0.0",
  "provides": ["generic-dungeon-api@1.0.0"]
}
```

Тогда другой мод может зависеть от `generic-dungeon-api` и получить `miaventura` как замену. Это аналог «виртуальных пакетов». Диапазон в `provides` резолвится по тем же правилам SemVer.

## Детерминированность

Два одинаковых запуска с одинаковыми входами дают одинаковый `Resolution`. Это важно для кэшей, автотестов и воспроизводимых баг-репортов.

Детерминированность достигается:

- сортировкой `Universe.providersOf(id)` по (`version` по убыванию, `toString()` provider'а для tie-breaker'а);
- фиксированным порядком обхода stack'а (FIFO в рамках слоя).

## Performance

- 150 модов, типичный набор зависимостей — < 50 мс.
- 500 модов, глубокие цепочки — < 500 мс (если пространство решений не взрывается).
- Патологический вход с 10^6+ ветвлений — отсекается `timeoutMs` (по умолчанию 5 секунд) с развёрнутой диагностикой.

## Тесты

`resolver/src/test/java` — ~100 тестов:

- elementary: одна цепочка, конфликт, инкомпатибилити;
- property-based (jqwik): случайные миры, проверка инвариантов;
- регрессионные: реальные конфигурации, которые ломались.

## Что читать дальше

- [modules/manifest.md](./manifest.md) — откуда мы читаем `dependencies`.
- Javadoc `dev.vida.resolver.ResolverError` — диагностируемые case'ы с примерами.
- [architecture/lifecycle.md](../architecture/lifecycle.md) — как резолвер встроен в boot.
