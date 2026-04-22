# Latidos — события

Высокопроизводительная event-шина Vida. Диспетчеры генерируются через `LambdaMetafactory` (без рефлексии на hot-path), обработчики хранятся в плоских массивах, аллокаций на `publicar(...)` — ноль для некапчерящих lambda.

Базовые типы — в [`modules/base.md`](../modules/base.md#devvidabaselatidos). Здесь — практика.

## Подписка

```java
public final class MiAventura implements VidaMod {
    @Override
    public void iniciar(ModContext ctx) {
        ctx.latidos().suscribir(
            LatidoPulso.TIPO,
            Prioridad.NORMAL,
            this::onPulse
        );
    }

    private void onPulse(LatidoPulso pulso) {
        if (pulso.tickActual() % 20 == 0) {
            // раз в секунду
        }
    }
}
```

Метод-обработчик возвращает `void` — обратно в шину ничего не уходит. Если событие `cancelable`, обработчик может позвать `pulso.cancelar()` (для применимых событий).

## Приоритеты и фазы

```java
enum Prioridad { URGENTE, ALTA, NORMAL, BAJA, MONITOR } // порядок вызова
enum Fase      { ANTES, PRINCIPAL, DESPUES }            // подпорядок внутри приоритета
```

Обработчики сортируются сначала по `Prioridad`, потом по `Fase`. `MONITOR` предназначен для логгирования/наблюдения — он видит финальное состояние события (включая флаг отмены), но сам его не меняет.

## Фильтры

Подписка может быть ограничена по условию — не вызывать обработчик, если условие не выполнено:

```java
ctx.latidos().suscribirSi(
    LatidoPulso.TIPO,
    Prioridad.NORMAL,
    pulso -> pulso.tickActual() % 20 == 0,
    this::onSecondly
);
```

Под капотом фильтр компилируется в отдельный диспетчер — проверка происходит до виртуального вызова обработчика и обычно JIT-инлайнится.

## Отписка

```java
Suscripcion sub = ctx.latidos().suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, this::onPulse);
// ...
sub.cancelar();
```

`Suscripcion` — value-тип; держите ссылку, если хотите отписаться. Если нет — подписка живёт пока жив `ModLoader` мода.

## Создание своего события

```java
public record EventoExplosion(Bloque bloque, Punto centro, float fuerza) implements Latido {
    public static final LatidoTipo<EventoExplosion> TIPO =
        LatidoTipo.of("miaventura:explosion");
}
```

Правила:

- `record` реализует `Latido`.
- `TIPO` — `public static final`, имя в формате `namespace:event_name` (валидируется).
- Если событие `cancelable`, добавьте флаг и API для отмены:

```java
public final class EventoExplosion implements LatidoCancelable {
    private boolean cancelado;
    public void cancelar() { cancelado = true; }
    public boolean cancelado() { return cancelado; }
    // ...
}
```

Публикация:

```java
ctx.latidos().publicar(new EventoExplosion(bloque, centro, 4.0f));
```

## Thread-affinity

По умолчанию обработчик вызывается на **том потоке**, на котором произошёл `emitir(...)`. Никакой магической маршализации — если событие опубликовали из MC-main-thread, ваш код там и исполнится.

### Latidos profundos — `Ejecutor`

Если вам нужно изменить поток исполнения (main-thread, фоновый пул, выделенный serialized worker) — передайте `Ejecutor` при подписке. Это стабилизировано в 0.3.0.

```java
// Main-thread: пригодится, когда событие приходит из worker'а,
// а реакция должна быть строго в главном потоке.
ctx.latidos().suscribir(
        Latidos.CARGA_CHUNK, Prioridad.NORMAL, Fase.PRINCIPAL,
        Ejecutor.hiloPrincipal(ctx.hiloPrincipal()),
        this::onChunkMain);

// Фон через Susurro: удобно для метрик, логов, I/O.
ctx.latidos().suscribir(
        EventoExplosion.TIPO, Prioridad.MONITOR, Fase.DESPUES,
        Ejecutor.susurro(ctx.susurro(), Prioridad.BAJA, Etiqueta.de("mi-mod/explosion-log")),
        this::registrarExplosion);

// Выделенный single-thread worker (FIFO, без конкуренции с другими подписчиками).
ctx.latidos().suscribir(
        Latidos.GUARDAR_MUNDO, Prioridad.NORMAL, Fase.DESPUES,
        Ejecutor.serializado("mi-mod/persist"),
        this::escribirEnDisco);
```

Фабрики:

| Фабрика | Где исполняется |
|---------|-----------------|
| `Ejecutor.SINCRONO` | в потоке `emitir` (дефолт) |
| `Ejecutor.hiloPrincipal(hp)` | в ближайшем `pulso()` главного потока |
| `Ejecutor.susurro(sus, prioridad, etiqueta)` | в пуле [`vida-susurro`](../modules/susurro.md) |
| `Ejecutor.serializado(nombre)` | собственный single-thread worker, FIFO |

Контракт:

- исключения внутри `Ejecutor` логируются шиной, но никогда не распространяются обратно в `emitir`;
- для `SINCRONO` сохраняется тот же стек, что и у `emitir`; для остальных стек независимый;
- `MONITOR` отлично сочетается с async — наблюдение, которое не тормозит главный цикл.

Подробнее: [`modules/base.md` → Latidos profundos](../modules/base.md#latidos-profundos-ejecutor-030) и [`modules/susurro.md`](../modules/susurro.md).

## Производительность

### Zero-alloc для простых lambda

Если ваш обработчик не захватывает переменные:

```java
ctx.latidos().suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, pulso -> { /* ... */ });
```

`LambdaMetafactory` создаёт singleton-реализацию интерфейса. Аллокаций на каждом `publicar(...)` — ноль. C2 инлайнит.

Если обработчик захватывает `this` (как в `this::onPulse`) — происходит одна аллокация на момент подписки; дальше — ноль.

### Fan-out

Стандартные события Vida (`LatidoPulso`, `LatidoArranque`, etc.) на 150 модов + 10 подписчиков каждый раз на главном потоке дают < 200 нс на `publicar(...)`. Это измерено JMH-бенчами в `bench/`.

## Стандартные события Vida

| Тип | Когда публикуется |
|-----|-------------------|
| `LatidoArranque` | клиент/сервер завершил инициализацию, готов принимать подключения |
| `LatidoPulso` | каждый server-tick (20 раз в секунду) |
| `LatidoParada` | начало shutdown |
| `LatidoConfiguracionRecargada` | горячая перезагрузка `Ajustes` |

Точный список и поля — в Javadoc пакета `dev.vida.base.latidos.eventos`.

## Частые ошибки

- **Подписка в конструкторе `VidaMod`.** Нельзя — `LatidoBus` ещё не передан. Подписывайтесь в `iniciar(ctx)`.
- **`null` в handler.** `Oyente<E>` не может быть null; проверка в debug, NPE в release.
- **Блокирующие операции в `SINCRONO`.** Если обработчик делает I/O или ждёт lock, перенесите его в `Ejecutor.susurro(...)` или `Ejecutor.serializado(...)` — иначе зависнет весь `emitir` со всеми подписчиками.
- **Мутация общего state из `Ejecutor.susurro(...)`.** Под `Susurro` исполнение многопоточное; либо защищайте state сами, либо используйте `Ejecutor.serializado(...)` / `Ejecutor.hiloPrincipal(...)`.

## Что читать дальше

- [modules/base.md](../modules/base.md#devvidabaselatidos) — API reference, включая `Ejecutor`.
- [modules/susurro.md](../modules/susurro.md) — управляемый thread-pool, который стоит за `Ejecutor.susurro(...)`.
- [architecture/performance.md](../architecture/performance.md) — почему диспетчер такой быстрый.
- [guides/catalogo.md](./catalogo.md) — типичный сосед Latidos в коде мода.
