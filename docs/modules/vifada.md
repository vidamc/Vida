# vifada

Байткод-трансформер Vida. Декларативный фреймворк модификации классов через аннотации `@VifadaMorph`, `@VifadaInject`, `@VifadaOverwrite`, `@VifadaShadow`.

- Пакет: `dev.vida.vifada`
- Gradle: `dev.vida:vida-vifada`
- Стабильность: `@ApiStatus.Preview("vifada")`
- Имя в проекте: **Vifada**

Эта страница — про модуль. Практическое руководство мод-автора — [guides/vifada.md](../guides/vifada.md).

## Модель

### Morph

Класс, отмеченный `@VifadaMorph(target = "fully.qualified.TargetClass")`. Описывает, что делать с целевым классом Minecraft.

```java
@VifadaMorph(target = "net.minecraft.client.gui.components.DebugScreenOverlay")
public abstract class DebugScreenMorph {
    @VifadaShadow private Font font;

    @VifadaInject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V",
                  at = @VifadaAt(InjectionPoint.HEAD))
    public void beforeRender(GuiGraphics g, CallbackInfo ci) {
        // свой код
    }
}
```

Morph обязан быть `abstract`, потому что он сам по себе не инстанцируется — его код инжектится в целевой класс.

### `@VifadaInject`

Вставка метода morph в указанную точку целевого метода.

- `method` — JVM-сигнатура целевого метода (имя + дескриптор: `"tick()V"`, `"render(Lnet/minecraft/client/gui/GuiGraphics;)V"`).
- `at` — `@VifadaAt` с `InjectionPoint` (`HEAD`, `RETURN`, зарезервированы остальные).
- Последним параметром метода-инъектора должен быть `CallbackInfo`.

Через `ci.cancel()` можно прервать выполнение целевого метода (и вернуть дефолтное значение, если метод возвращает тип).

### `@VifadaOverwrite`

Полная замена тела целевого метода:

```java
@VifadaOverwrite(method = "shouldShowFps()Z")
public boolean shouldShowFps() {
    return false;
}
```

Приоритет `VifadaOverwrite` выше любых `VifadaInject` — если на метод одновременно стоят `Overwrite` и `Inject`, резолвер выдаёт диагностику и помечает конфигурацию невалидной.

### `@VifadaShadow`

Декларация «у целевого класса есть такое поле/метод». Компилятор видит обычное поле/метод; Vifada во время трансформации подменяет owner на `TargetClass`. Это позволяет писать код, как будто вы внутри целевого класса.

```java
@VifadaShadow private int ticks;
@VifadaShadow public abstract void tick();
```

### `@VifadaAt`

Точка инъекции:

- `HEAD` — самое начало метода.
- `RETURN` — все `xRETURN`-инструкции (подменяются на ветку к инъектору).
- `INVOKE`, `CONSTANT`, `FIELD`, `NEW` — зарезервированы, не реализованы в 0.x.

### `@VifadaMulti` и `@VifadaLocal` (preview: `vifada-next`)

С `0.6.0` в API доступны ранние аннотации следующего поколения:

- `@VifadaMulti` — один инъектор для нескольких target-методов (`methods = {...}`).
- `@VifadaLocal` — декларация доступа к локальной переменной target-метода (по `ordinal`/`descriptor`).

Важно: это именно preview-контракт (`@ApiStatus.Preview("vifada-next")`). Transformer 0.6.0 ещё не применяет эти аннотации в рантайме; они добавлены, чтобы моды и инструменты могли заранее компилироваться под будущий API.

## Главный API

### `Transformer`

Главный класс модуля. Принимает байты целевого класса и коллекцию `MorphSource`, возвращает трансформированные байты либо `VifadaError`.

```java
Result<byte[], VifadaError> r = Transformer.transform(targetClassBytes, morphs);
```

`MorphSource` — один скомпилированный morph (байты его `.class` + метаданные). Загрузчик строит `MorphIndex: target → List<MorphSource>` на старте и передаёт здесь.

### `VifadaError`

Типизированные ошибки:

- `TargetNotFound(String method)` — метод с указанной сигнатурой не существует в целевом классе.
- `InvalidMorph(String morph, String reason)` — morph нарушает контракт (не abstract, несовпадение сигнатур).
- `PriorityConflict(String method, List<String> morphs)` — несколько morph'ов претендуют на одну точку с одинаковым приоритетом.
- `AsmError(String method, Throwable cause)` — байткод-валидатор ASM отклонил результат.

## Приоритеты и порядок

`@VifadaMorph(priority = 1000)` — меньше значит раньше в цепочке. Дефолт `1000`. Если два мода инъектят в одну точку — порядок детерминированный (сортировка по `priority`, при равенстве — по алфавиту `morph class FQCN`).

Для случаев, где порядок критичен, используйте `@VifadaMorph(order = VifadaOrder.AFTER, afterTargets = {"otra:morph"})` (preview-API).

## Как работает внутри

1. **Классификация morph'ов.** На старте `MorphIndex` строит мапу `target-class → List<MorphSource>`. Морфы сортируются по приоритету.
2. **Применение.** Когда JVM грузит целевой класс, `VidaClassTransformer` находит список и идёт по нему:
   - Для каждого `VifadaInject` — ищет target-метод в `ClassNode`, вставляет ASM-вызов в нужной точке.
   - Для `VifadaOverwrite` — подменяет тело метода целиком.
   - `VifadaShadow`-обращения в байткоде morph'а переписываются: `morph.field` → `target.field`.
3. **Фрейминг.** `ClassReader.accept(cn, 0)` — с сохранением frame'ов; ASM пересчитает их сам.
4. **Валидация.** Опционально (`vida.debug.verifyClasses=true`) — `CheckClassAdapter` прогоняет результат и падает на невалидном байткоде.

## `CallbackInfo`

Объект, который инъектор получает последним параметром:

```java
public void onTick(CallbackInfo ci) {
    if (condicion) ci.cancel();
}
```

- `cancel()` — прерывает выполнение target-метода. Если метод возвращает `void`, управление просто уходит из метода. Если возвращает тип — возвращается default (0/null/false) или значение, указанное через `ci.setReturnValue(...)`.
- `isCancelled()` — для цепочек инъекторов, чтобы понимать, что предыдущий отменил выполнение.

## Отладка

- `-Dvida.debug.dumpMorphs=true` — сохраняет transformed классы в `run/vida/dumps/`.
- `-Dvida.debug.morphReport=html` — генерирует HTML-отчёт «какой morph что сделал с каким методом» (в работе).
- Лог `vida.vifada` — уровень `DEBUG` показывает каждую трансформацию с указанием morph'а, target'а и точки.

## Почему `@Preview`

Текущий набор InjectionPoint'ов (`HEAD`, `RETURN`) покрывает большинство кейсов, но для сложных мод-инженерных задач нужны `INVOKE`/`CONSTANT`/`FIELD`. Мы ждём реального фидбэка от ранних пользователей перед тем, как зафиксировать API. До 1.0.0 возможны breaking changes — они будут задокументированы в CHANGELOG.

## Что читать дальше

- [guides/vifada.md](../guides/vifada.md) — развёрнутые примеры для типичных задач.
- [modules/loader.md](./loader.md) — где и когда Vifada вызывается.
- [modules/cartografia.md](./cartografia.md) — как рантайм-имена маппятся на Mojang-имена.
