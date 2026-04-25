# cartografia — Cartografía

Подсистема маппингов имён. Позволяет разработчикам писать моды в читаемых именах Mojang, а рантайму применять их к обфусцированным именам на реальном клиенте/сервере Minecraft.

- Пакет: `dev.vida.cartografia`
- Gradle: `dev.vida:vida-cartografia`
- Стабильность: `@ApiStatus.Stable`
- Имя в проекте: **Cartografía**

## Задача

Minecraft Java Edition поставляется с обфусцированными именами (`a`, `b`, `ajp`). Mojang публикует Proguard-маппинги для каждой версии — там указаны канонические имена (`net.minecraft.world.level.Level`). Модам нужен способ:

1. В dev-окружении видеть читаемые имена.
2. В рантайме (на пользовательской машине) применять изменения к обфусцированным именам.
3. Все компоненты — стабильные, быстро читаемые, версионируемые.

Cartografía решает это тремя вещами: `MappingTree` (структура), `.ctg` (формат), `CartografiaRemapper` (применение).

## `MappingTree`

Иммутабельное дерево классов, полей, методов в нескольких namespace.

```java
MappingTree tree = ...;

// поиск класса во всех известных namespace
ClassMapping cls = tree.findClass("net.minecraft.world.level.Level", Namespace.MOJANG)
    .orElseThrow();

// обращение к полю по его имени в одном namespace, получение в другом
FieldMapping tickCount = cls.field("tickCount", Namespace.MOJANG).orElseThrow();
String obf = tickCount.name(Namespace.OBF);
```

`Namespace` — value-тип (`OBF`, `INTERMEDIARY`, `MOJANG`, плюс произвольные пользовательские). Хранение — плотные массивы `int` индексов в дедуплицированной таблице строк; это и быстро, и компактно.

## Форматы I/O

### Proguard (Mojang)

Стандартный формат Mojang: `<oldFQCN> -> <newFQCN>:`, `<returnType> <oldName>(<args>) -> <newName>`. Парсер в `dev.vida.cartografia.io.ProguardReader` — потоковый, без построения промежуточных структур.

```java
try (Reader r = Files.newBufferedReader(path)) {
    MappingTree tree = ProguardReader.read(r, Namespace.OBF, Namespace.MOJANG);
}
```

### `.ctg` — собственный формат

Бинарный, компактный. Сохраняет всё, что есть в Proguard, плюс опциональные `param`-имена методов и документацию. Размер ~30% от Proguard-текста, скорость чтения ~5× выше.

Используется как промежуточное хранилище: инсталлятор/Gradle-плагин конвертируют Proguard в `.ctg` и кладут рядом с модом.

```java
try (InputStream in = Files.newInputStream(path)) {
    Result<MappingTree, MappingError> r = CtgReader.read(path.getFileName().toString(), in);
}
```

#### Версия файла и обновление между релизами Minecraft

Публичные константы: `dev.vida.cartografia.io.CtgMappingFormat` — `VERSION_MAJOR`, `VERSION_MINOR`, `MAX_SUPPORTED_MINOR`. Заголовок каждого `.ctg` хранит major/minor; при несовпадении `major` или при `minor > MAX_SUPPORTED_MINOR` чтение возвращает `MappingError.UnsupportedVersion` с номером версии из файла (не «немая» ошибка).

Процедура обновления при новой версии Minecraft: получить свежий Proguard от поставщика версии → прогнать через конвертер в `.ctg` инструментами Vida → закоммитить артефакт под именем, включающим версию игры; при смене формата `.ctg` обновить зависимость `vida-cartografia` до релиза, где поднят `MAX_SUPPORTED_MINOR`, и пересобрать `.ctg`.

Полная бинарная схема секций — во внутреннем `CtgFormat` и в Javadoc `CtgReader`/`CtgWriter`.

## `CartografiaRemapper` (ASM)

Класс в подпакете `dev.vida.cartografia.asm`. Реализует `org.objectweb.asm.commons.Remapper` — подходит для:

- ремаппинга JAR-ов в Gradle-плагине (`vidaRemapJar`);
- применения в рантайме через `ClassRemapper` (в связке с Vifada).

```java
CartografiaRemapper remapper = new CartografiaRemapper(tree, Namespace.MOJANG, Namespace.OBF);
ClassReader cr = new ClassReader(input);
ClassWriter cw = new ClassWriter(0);
cr.accept(new ClassRemapper(cw, remapper), 0);
byte[] out = cw.toByteArray();
```

### Правила работы

- Не паникует на неизвестных именах: если поле/метод не в `MappingTree`, оно пропускается без изменений.
- Корректно обрабатывает inner-classes, generic-сигнатуры, bootstrap-методы.
- Не инлайнит и не удаляет атрибуты — только переименовывает.

## Использование в Gradle

```kotlin
vida {
    minecraft {
        version.set("1.21.1")
        mappings {
            proguard.set(file("mappings/mojang_1_21_1.txt"))
            // Опционально:
            // ctgCache.set(layout.buildDirectory.file("vida/mappings.ctg"))
        }
    }
}
```

При первой сборке плагин:

1. Читает Proguard.
2. Строит `MappingTree`.
3. Кэширует в `.ctg` (быстрее на последующих запусках).
4. Передаёт ремаппер задачам компиляции (IntelliJ/Eclipse через `javaCompileOptions`) и `vidaRemapJar`.

## Почему не Tiny / Serdes-v2

Tiny-формат и его варианты оптимизированы под SIM-card сценарии other mod-loaders — у них много специфичных для них полей и конвенций. Vida не хочет привязывать `cartografia` к чужой эволюции. `.ctg` — наш, покрывает все нужды Vida и открыт (читаемая спецификация).

## Потокобезопасность

`MappingTree` — глубоко иммутабелен; потоко-безопасен на чтение. Построение — однопоточное (строим в одном воркере, потом публикуем).

## Ошибки

`MappingError` — sealed-иерархия:

- `FormatError` — синтаксис Proguard/`.ctg` сломан.
- `DuplicateEntry` — в одном файле два раза указан один класс/поле/метод.
- `NamespaceNotFound` — запрошен `Namespace`, которого в `MappingTree` нет.

## Поколения платформы и поле `mappingMode`

Рантайм-источник правды для дропов Minecraft и правил импорта Cartografía задаётся **профилем платформы** (`platform-profiles/generations/.../profile.json`), не этим модулем напрямую.

- Для семантической линии **1.21.x** поколение `LEGACY_121`: по умолчанию стратегия `GAME_DIR_PROGUARD` читает Proguard-текст из дерева лаунчера (`client_mappings.txt` под версией клиента).
- Для календарной линии **26.x** поколение `CALENDAR_26`: могут понадобиться другие пайплайны импорта и артефакты — см. профиль и поле **`mappingMode`** в JSON (ярлык политики для будущего связывания с парсерами Cartografía).

Импорт в `MappingLoader` и связка с деревом имён для `MorphMethodResolution` описаны в [modules/platform-profiles.md](./platform-profiles.md) и [architecture/classloading.md](../architecture/classloading.md).

## Что читать дальше

- [Vifada](./vifada.md) и [guides/vifada.md](../guides/vifada.md) — как ремаппер применяется в трансформациях.
- [modules/gradle-plugin.md](./gradle-plugin.md) — задачи `vidaRemapJar`, настройка mappings{}.
- Javadoc `dev.vida.cartografia.io.CtgFormat` — бинарная спецификация `.ctg`.
