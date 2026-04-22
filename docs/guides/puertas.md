# Puertas — access wideners

Практическое руководство по `.ptr`-файлам: когда нужны, как писать, как подключить к сборке, как отладить.

Формальное описание API и перечень ошибок — [`modules/puertas.md`](../modules/puertas.md). Здесь — рецепты.

## Когда они нужны

Vanilla Minecraft в 1.21.1 прячет много полезного за `private` / `protected` / `final`. Три типичных сценария:

1. **Прочитать `private` поле игрока.** Пример: ваш мод показывает скрытую шкалу насыщения — vanilla поле `FoodData.saturationLevel` — `private float`. Через обычный Java-код недоступно.
2. **Унаследоваться от `final` класса.** Пример: ваша подкласс-`FurnaceBlockEntity` с другим GUI; vanilla `FurnaceBlockEntity` помечен `final`.
3. **Переопределить `final` метод.** Пример: изменить поведение `BlockItem.place(BlockPlaceContext)` в наследнике.

Во всех трёх случаях `.ptr` — инструмент минимального воздействия: вы меняете только флаги доступа, а не сам код. Если alternative — `@VifadaInject` (в runtime-трансформацию) или reflection (медленно и хрупко), `.ptr` предпочтительнее.

## Минимальный пример

Файл `src/main/resources/puertas/mi-mod.ptr`:

```
vida-puertas 1 namespace=intermedio

# 1. Открыть доступ к приватному полю
accesible field net/minecraft/world/food/FoodData saturationLevel F

# 2. Разрешить наследовать от финального класса
extensible class net/minecraft/world/level/block/entity/FurnaceBlockEntity

# 3. Разрешить override final-метода
extensible method net/minecraft/world/item/BlockItem place (Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;
```

Подключение в `build.gradle.kts` мода:

```kotlin
vida {
    mod {
        id.set("mi-mod")
        displayName.set("Mi Mod")
    }
    puertas {
        srcDirs.from(files("src/main/resources/puertas"))
    }
}
```

Плагин автоматически:

1. Прогоняет `vidaValidatePuertas` при сборке. Ошибки → `BUILD FAILED` с указанием файла и строки.
2. Упаковывает валидные `.ptr` в `META-INF/vida/puertas/<имя>.ptr` внутри jar'а.

На рантайме loader читает все `.ptr` из подгруженных модов, сливает через `PuertaArchivo.combinar(...)` и применяет `AplicadorPuertas.aplicar(...)` по мере загрузки целевых классов.

## Три действия

### `accesible` — сделать public

```
accesible class   net/example/Foo
accesible method  net/example/Foo bar (I)Ljava/lang/String;
accesible field   net/example/Foo count I
```

Убирает `private` / `protected`, ставит `public`. Для полей дополнительно снимает `final` — это общепринятая семантика (Fabric AW, Forge AT делают так же), и без неё `accesible field` на приватный final-static-поле было бы полу-бесполезным (читать можно, писать — нет).

### `extensible` — разрешить наследование/override

```
extensible class   net/example/Foo
extensible method  net/example/Foo bar (I)Ljava/lang/String;
```

Снимает `final`. Для класса — позволяет extends; для метода — позволяет override.

`extensible field` **невалидно** — у полей нет семантики «переопределения», для них используйте `mutable`.

### `mutable` — разрешить запись в `final` поле

```
mutable field net/example/Foo count I
```

Снимает `final` с поля, не трогая visibility. Полезно, когда поле уже `public` / `protected`, но final мешает записать значение.

`mutable class` и `mutable method` **невалидны** — парсер отвергнет.

## Namespace: какой выбрать

Заголовок `.ptr` обязан содержать `namespace=<...>`. Правила:

- **`namespace=intermedio` (рекомендовано)** — пишите ваши `.ptr` против стабильных промежуточных имён Vida (из `Cartografía`). Это правильный способ: имена не меняются между минорными версиями Minecraft.
- **`namespace=exterior` (dev-only)** — имена Mojang-mapped (как в JDT/Gradle-сборке). Удобно писать, читается, но `loader` должен remap'нуть их перед применением. В 0.3.0 это автоматика ещё не включена — рантайм ожидает `intermedio`. Если в dev-режиме вы пишете `exterior`, добавьте шаг `vidaRemapPuertas` (0.4.x, см. session-roadmap).
- **`namespace=crudo`** — obfuscated. Почти никогда не нужно — разве что вы пишете патч под production-jar без маппингов, что за пределами обычного модинга.

Несовпадающие namespace нельзя слить: `PuertaArchivo.combinar(...)` бросит `IllegalArgumentException`.

## Синтаксис descriptor'ов

`.ptr` использует стандартные JVM descriptor'ы (как в Javadoc / `javap -p`).

**Primitives:**

| Java | descriptor |
|------|------------|
| `boolean` | `Z` |
| `byte` | `B` |
| `short` | `S` |
| `int` | `I` |
| `long` | `J` |
| `float` | `F` |
| `double` | `D` |
| `char` | `C` |
| `void` (только return) | `V` |

**Reference:** `Lfully/qualified/Name;` — например, `Ljava/lang/String;`.

**Array:** `[` перед типом. Массив строк: `[Ljava/lang/String;`, массив int: `[I`, двумерный массив int: `[[I`.

**Метод:** `(args)ret`. Примеры:

```
()V                                  // void m()
(I)Z                                 // boolean m(int)
(Ljava/lang/String;)V                // void m(String)
([[ILjava/lang/String;)V             // void m(int[][], String)
()Ljava/util/List;                   // List m()
```

**Подглядеть:** `javap -p -s <class>` выдаёт точные descriptor'ы для уже скомпилированного класса. Это самый надёжный источник.

## Частые ошибки и как их чинить

### `DirectivaTruncada`

```
accesible field net/example/Foo count
```

Не хватает descriptor'а. Исправление: `accesible field net/example/Foo count I`.

### `DescriptorMalformado`

```
accesible method net/example/Foo m (String)V
```

`String` без `L` / `;`. Исправление: `(Ljava/lang/String;)V`.

### `MutableNoAplicable`

```
mutable method net/example/Foo m ()V
```

`mutable` применимо только к `field`. Для снятия `final` с метода — `extensible method`.

### `NamespaceDesconocido`

```
vida-puertas 1 namespace=mojang
```

Допустимы только `crudo` / `intermedio` / `exterior`. Исправление: `namespace=exterior`.

### `claseInternal должен быть в internal-name форме`

```
accesible class net.example.Foo
```

JVM internal-name использует `/`, не `.`. Исправление: `net/example/Foo`.

### Директива есть, но ничего не применилось

Посмотрите `Informe.perdidas` — туда попадают директивы, для которых не нашёлся целевой член. Возможные причины:

- опечатка в имени метода / дескриптора;
- мод написан под другую версию vanilla — член был переименован / удалён;
- namespace не тот: вы написали `EXTERIOR`, а loader работает в `INTERMEDIO`.

Для диагностики запустите `--validate-puertas` локально — он только парсит (не применяет) и покажет структурные ошибки. Для «применилось ли» — смотрите лог loader'а в `logs/latest.log` (уровень `DEBUG`, тэг `vida.puertas.AplicadorPuertas`).

## Рабочий процесс

1. Обнаружили, что нужно «открыть» vanilla-член.
2. `javap -p -s <class>` — узнайте точное имя и дескриптор.
3. Добавьте директиву в `src/main/resources/puertas/<mod-id>.ptr`.
4. `./gradlew vidaValidatePuertas` — проверка формата.
5. `./gradlew vidaRun` — запуск dev-клиента; убедитесь, что мод компилируется и работает.
6. Перед публикацией — `./gradlew build`; `.ptr` попадут в jar автоматически через `vidaPackagePuertas`.

## Что нельзя сделать через `puertas`

- Изменить сигнатуру метода (добавить параметр, изменить тип). Это задача `@VifadaMorph`/`@VifadaInject`.
- Добавить новое поле в класс. Это тоже `@VifadaMorph`.
- Поменять реализацию метода. Это `@VifadaOverwrite` или `@VifadaInject`.
- Снять `abstract`. Vida не даёт такой возможности — это обычно ведёт к `AbstractMethodError` на вызове. Если вам нужна реализация, делайте `@VifadaOverwrite`.

Если ваше изменение не выражается через access flags — нужен `Vifada`. См. [guides/vifada.md](./vifada.md).

## Лимиты в 0.3.0

- Автоматический namespace-remap (`exterior` → `intermedio`) пока не включён — пишите прямо в `intermedio`. Будет в 0.4.x.
- Конфликт-резолвер для двух модов, меняющих один член в противоположную сторону, отсутствует — последний зарегистрированный мод побеждает. В 0.5.x появится явное разрешение через `Ajustes`.
- Проверка того, что `.ptr` валиден под *текущую версию vanilla* (а не вообще валиден синтаксически), нужна вручную — `javap -p -s` + глаза.

## Что читать дальше

- [`modules/puertas.md`](../modules/puertas.md) — формальный API reference.
- [`modules/gradle-plugin.md`](../modules/gradle-plugin.md#puertas) — `vidaValidatePuertas`, `vidaPackagePuertas`, DSL.
- [`reference/cli-installer.md`](../reference/cli-installer.md#--validate-puertas) — CLI-валидация.
- [`guides/vifada.md`](./vifada.md) — когда одного `.ptr` недостаточно.
