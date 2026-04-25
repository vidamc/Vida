# escultores

Публичный API низкоуровневых трансформеров байткода **`Escultor`**: патч `byte[] → byte[]` с дешёвым предикатом `mightMatch`. Отличаются от **Vifada** тем, что не требуют статических аннотаций на целях — подходят для точечных правок и внутреннего брендинга.

- Пакет: `dev.vida.escultores`
- Gradle: `:escultores`
- Стабильность: `@ApiStatus.Stable` (интерфейс `Escultor`; встроенная реализация — см. ниже)

## Зависимости

- `core`, ASM (`ClassReader` / `ClassWriter` только во встроенных реализациях).

## Реализации

| Класс | Назначение |
|-------|------------|
| `BrandingEscultor` | Патч строки F3 debug (входит в состав loader через этот модуль) |

Загрузчик **`VidaClassTransformer`** применяет цепочку: встроенный branding → экземпляры `Escultor`, объявленные в **`vida.mod.json`** (`escultores`), загружаемые через `ModLoader`. Метрики вызовов — **`EscultorRegistroMetricas`** в модуле `:loader` (`@Internal`): по умолчанию в поле «наносекунды» пишется **`0`** (без двойного `nanoTime` на каждый класс); детальный тайминг включается флагом **`-Dvida.loader.profileEscultorNanos=true`**.

## Объявление в манифесте

Список классов и приоритет — см. [reference/manifest-schema.md](../reference/manifest-schema.md#escultores--optional-array) и практический разбор в [guides/escultores.md](../guides/escultores.md).

## См. также

- [guides/escultores.md](../guides/escultores.md) — паттерны `mightMatch` / `tryPatch`
- [modules/loader.md](./loader.md) — где цепочка подключается к `Instrumentation`
