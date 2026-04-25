# Escultores — низкоуровневые трансформеры



Когда `@VifadaMorph` недостаточно — например, нужно модифицировать класс без статического описания в morph-классе, или работать с байткодом напрямую — используются **`Escultor`**'ы.



Публичный интерфейс **`dev.vida.escultores.Escultor`** помечен **`@ApiStatus.Stable`**. Для большинства модов по-прежнему достаточно Vifada; Escultor — осознанный «спуск ниже».



## Концепция



`Escultor` — функция `byte[] → byte[]` с дешёвым предикатом:



```java

public interface Escultor {

    String nombre();

    boolean mightMatch(String nombreClaseInternal, byte[] archivoClase);

    byte[] tryPatch(String nombreClaseInternal, byte[] archivoClase);

}

```



- **`nombre()`** — диагностическое имя для логов.

- **`mightMatch(...)`** — **дешёвый** pre-check. Если `false`, `tryPatch` не вызывается.

- **`tryPatch(...)`** — настоящая работа. Возвращает `null`, если класс не изменён.



Принцип: **`mightMatch` должен быть O(1) или O(размер класса)** — он вызывается на каждом загружаемом классе игры. `tryPatch` — дороже, но только после положительного `mightMatch`.



## Регистрация через `vida.mod.json`



Классы Escultor перечисляются в поле **`escultores`** манифеста (строки FQCN и/или объекты с **`priority`**). Загрузчик создаёт экземпляры через `ModLoader` в **`BootSequence`** и подключает их в **`VidaClassTransformer`** после встроенного **`BrandingEscultor`**. Подробности — [reference/manifest-schema.md](../reference/manifest-schema.md#escultores--optional-array), [modules/escultores.md](../modules/escultores.md).



Отдельного программного `registerEscultor` в публичном API нет: контракт — декларативный список в манифесте.



## Пример: `BrandingEscultor`



Реализация в репозитории — **`dev.vida.escultores.BrandingEscultor`** (`INSTANCE`): byte-scan по сырому `.class`, затем ASM только при совпадении маркера; замена строки формата F3 на вариант с `(vida)`. Используйте её как эталон паттерна «дешёвый pre-check, дорогая обработка», без копирования устаревших примеров из старых версий документации.



## Когда писать свой `Escultor`



- Имя цели заранее неизвестно или меняется между версиями игры.

- Нужна логика «применить только если…», которую не выразить через `@VifadaMorph`.

- Нужна работа с постоянным пулом / атрибутами класса без генерации морф-класса.



Для большинства задач достаточно **[Vifada](./vifada.md)**.



## Правила хорошего тона



### 1. Делайте `mightMatch` _очень_ быстрым



Любой полный ASM-parse в `mightMatch` — признак ошибки дизайна.



### 2. Никогда не бросайте из `tryPatch` проверяемые исключения наружу



Возвращайте `null` при неожиданном состоянии. Исключения на hot-path загрузки класса превращаются в аварию JVM.



### 3. Логируйте успешные патчи на INFO



Это упрощает диагностику пользовательских логов.



### 4. Тестируйте loadability



Минимальный тест — прогон `tryPatch` на эталонном `.class` и загрузка результата в изолированном `ClassLoader` (см. тесты `:escultores` / `:loader`).



## Порядок с Puertas и Vifada



1. **Puertas** (`.ptr`) расширяют видимость — должны быть применены до логики, которая генерирует обращения к расширенным символам.

2. **Vifada** изменяет методы по морф-классам.

3. **Escultor** — точечный патч «сырых» байтов, если предыдущие слои не подходят.



В Gradle: `vidaValidatePuertas` → сборка → рантайм-трансформер с тем же порядком внутри агента.



## Что читать дальше



- [modules/loader.md](../modules/loader.md#brandingescultor) — где цепочка подключается.

- [architecture/performance.md](../architecture/performance.md) — зачем нужен pre-check.

- [guides/vifada.md](./vifada.md) — высокоуровневая альтернатива.


