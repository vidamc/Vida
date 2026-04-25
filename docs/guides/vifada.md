# Vifada — модификация байткода

Практическое руководство по работе с Vifada. Справочник API — в [`modules/vifada.md`](../modules/vifada.md).

## Когда нужна Vifada

Большую часть кастомизации Minecraft можно сделать через события (`Latidos`) и регистрации (`Catalogo`). Vifada нужна, когда:

1. Vanilla не бросает событие в нужном месте.
2. Нужно изменить возвращаемое значение vanilla-метода.
3. Нужно подменить поведение частной логики (AI моба, рендер, сетевой протокол).

Если задачу можно решить событием — решайте событием. Vifada — дорогой инструмент: она привязывает ваш мод к внутренней структуре Minecraft, которая может поменяться между версиями.

## Минимальный morph

```java
package com.ejemplo.morphs;

import dev.vida.vifada.*;

@VifadaMorph(target = "net.minecraft.client.gui.components.DebugScreenOverlay")
public abstract class DebugScreenMorph {

    @VifadaInject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @VifadaAt(InjectionPoint.HEAD)
    )
    public void antesRender(GuiGraphics graphics, CallbackInfo ci) {
        // ваш код, который сработает перед рендером debug-скрина
    }
}
```

Что нужно:

1. Класс `abstract` — Vifada генерирует reference-вызовы; экземпляр morph'а никогда не создаётся.
2. Аннотация `@VifadaMorph(target = "FQCN")` — полное имя целевого класса.
3. Inject-метод с `CallbackInfo` последним параметром.

## Декларация в `vida.mod.json`

Плагин `dev.vida.mod` подхватывает это автоматически, если вы объявили `vida.mod.json` вручную, укажите:

```json
{
  "id": "miaventura",
  "version": "0.1.0",
  "name": "Mi Aventura",
  "vifada": {
    "morphs": [
      "com.ejemplo.morphs.DebugScreenMorph"
    ]
  }
}
```

Если не объявить — morph не применится. Это защита от случайных mixin'ов при наличии тестовых классов в JAR.

## `@VifadaInject`

Вставка кода в указанную точку:

```java
@VifadaInject(
    method = "tick()V",                        // имя + JVM-дескриптор
    at = @VifadaAt(InjectionPoint.HEAD)       // точка
)
public void beforeTick(CallbackInfo ci) {
    ctx.log().debug("tick begin");
}
```

### Дескрипторы

Минимальный справочник JVM-дескрипторов:

- `V` — void
- `Z` — boolean, `B` — byte, `C` — char, `S` — short, `I` — int, `J` — long, `F` — float, `D` — double
- `Lpackage/ClassName;` — reference (обратите внимание: слэши, не точки)
- `[X` — массив X

Пример: `boolean isBlockAvailable(BlockPos pos)` → `(Lnet/minecraft/core/BlockPos;)Z`.

### InjectionPoint'ы

В версии MVP:

- `HEAD` — первая строка метода.
- `RETURN` — перед каждой точкой возврата из метода.

Зарезервированы (не реализованы):

- `INVOKE`, `CONSTANT`, `FIELD`, `NEW`

### Отмена

```java
@VifadaInject(method = "shouldRender()Z", at = @VifadaAt(InjectionPoint.HEAD))
public void forceShow(CallbackInfo ci) {
    ci.setReturnValue(true);
    ci.cancel();
}
```

`setReturnValue` работает только для ненулевых типов и должен совпадать с возвращаемым типом target-метода.

## `@VifadaOverwrite`

Полная замена тела метода:

```java
@VifadaOverwrite(method = "getVersion()Ljava/lang/String;")
public String version() {
    return "1.21.1 (modded by miaventura)";
}
```

Используйте осторожно: overwrite несовместим с другими модами, которые инжектят в тот же метод — трансформер сообщит об ошибке конфигурации (см. sealed-типы `VifadaError`).

## `@VifadaShadow`

Обращение к приватным членам целевого класса:

```java
@VifadaMorph(target = "net.minecraft.server.level.ServerLevel")
public abstract class ServerLevelMorph {

    @VifadaShadow private long tickCount;

    @VifadaShadow
    public abstract void tickNonPassenger(Entity entity);

    @VifadaInject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @VifadaAt(InjectionPoint.HEAD))
    public void beforeTick(CallbackInfo ci) {
        if (tickCount > 1_000_000) {
            // доступ к приватному полю
        }
    }
}
```

Vifada перехватывает `GETFIELD` / `INVOKE`-инструкции морфа, меняет owner с `ServerLevelMorph` на `ServerLevel`. Поле/метод должны существовать в целевом классе — иначе `VifadaError.TargetNotFound`.

## `@VifadaAt` (тонкая настройка)

```java
@VifadaAt(
    value = InjectionPoint.RETURN,
    ordinal = 1  // только 2-й RETURN в методе (0-indexed)
)
```

`ordinal` нужен для методов с несколькими `return` — позволяет точечно попасть в нужный.

## Приоритеты

```java
@VifadaMorph(target = "...", priority = 500)
```

Меньше значит раньше. Дефолт — `1000`. Если несколько морфов должны работать в строгом порядке, проставляйте разные приоритеты и документируйте зависимость.

## Как отлаживать

### Дамп патченных классов

```bash
java -Dvida.debug.dumpMorphs=true -javaagent:vida-loader.jar ...
```

Файлы — в `run/vida/dumps/<package>/<class>.class`. Читайте через `javap -p -v` или откройте в IntelliJ.

### Лог трансформаций

`vida.vifada` на уровне `DEBUG`:

```
DEBUG vida.vifada — applied DebugScreenMorph to net.minecraft.client.gui.components.DebugScreenOverlay#render
DEBUG vida.vifada — applied 3 morphs to net.minecraft.server.level.ServerLevel
```

### Валидация байткода

```bash
java -Dvida.debug.verifyClasses=true ...
```

Прогоняет патченный класс через `CheckClassAdapter`. Медленно, но находит 100% невалидного байткода.

## Частые ошибки

### `TargetNotFound`

Сигнатура метода не совпадает:

- Проверьте дескриптор (точные типы аргументов, slash не dot).
- Проверьте, что вы пишете на правильной стороне маппингов (dev-окружение — Mojang, production — уже ремапнуто).
- Возможно, метод был inline'ен компилятором — ищите enclosing.

### `VerifyError`

Нарушен контракт: тип возврата `CallbackInfo.setReturnValue(...)` не совпадает с target, или вы пытаетесь `cancel()` в void-методе.

### Morph не применяется

1. Класс не в `vida.mod.json:vifada.morphs`.
2. Class ещё не загружен — проверьте логи, что JVM реально дошла до него.
3. Неправильный `target` — FQCN с точками, не slash'ами.

## Vifada 2

Расширения **`@VifadaMulti`** (несколько целевых методов одним инъектором), **`@VifadaLocal`** (чтение LVT в `HEAD`), **`@VifadaRedirect`** (подмена выбранного `INVOKESTATIC`), а также диагностика **`MorphConflict`** — описаны в [modules/vifada.md](../modules/vifada.md) (раздел «Vifada 2»).

## Что читать дальше

- [modules/vifada.md](../modules/vifada.md) — reference.
- [guides/escultores.md](./escultores.md) — когда Vifada не хватает.
- Javadoc `dev.vida.vifada.CallbackInfo` — полный API.
