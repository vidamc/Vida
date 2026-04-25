# susurro

Управляемый пул асинхронных задач Vida: приоритеты, back-pressure по этикеткам, безопасный marshal на главный поток. Появился в 0.3.0.

- Пакет: `dev.vida.susurro`
- Gradle: `dev.vida:vida-susurro`
- Стабильность: **`@ApiStatus.Stable`** с 1.0.0

Зачем собственный пул, а не `ForkJoinPool.commonPool()`:

- **Приоритеты** — `ALTA` / `NORMAL` / `BAJA` с FIFO внутри приоритета. Полезно, когда задача «подсчёт light-map чанка» должна обгонять «рендер-статистику».
- **Back-pressure по этикетке** — лимит одновременно работающих задач с одним тегом. Защищает от мода, который в cycle'е может отправить миллион задач и вытеснить всех остальных.
- **Main-thread marshalling** — результат асинхронной работы гарантированно применится на игровом тике через `HiloPrincipal`, без race conditions.
- **Именованные daemon-потоки** (`vida-susurro-N`) — удобно в профайлере и не держат JVM от выхода.

```kotlin
dependencies {
    compileOnly("dev.vida:vida-susurro:0.3.0")
}
```

## Главные типы

### `Susurro`

Facade пула. `AutoCloseable`, ровно одна живая инстанция на рантайм (движок поднимает при загрузке, останавливает при shutdown).

```java
try (Susurro sus = Susurro.iniciar()) {                 // дефолтная политика
    Tarea<String> t = sus.lanzar(
            Prioridad.NORMAL,
            Etiqueta.de("ejemplo/io"),
            () -> leerArchivoGrande());
    String resultado = t.esperar();       // блокирующее ожидание, в тестах
}
```

API:

- `Susurro.iniciar()` / `Susurro.iniciar(Politica)` — фабрики;
- `<T> Tarea<T> lanzar(Prioridad, Etiqueta, Supplier<T>)` — отправка задачи с результатом;
- `Tarea<Void> lanzar(Prioridad, Etiqueta, Runnable)` — без результата;
- `Estadisticas estadisticas()` — снимок метрик (`activos`, `pendientes`, `completadas`, `rechazadas`, `workers`);
- `detener()` / `close()` — плавная остановка (5 секунд grace, потом `shutdownNow`).

### `Susurro.Politica`

Иммутабельный record:

| Поле | Смысл | Дефолт |
|------|-------|--------|
| `workers` | размер пула | `max(2, ceil(cpu/2))` через `Politica.porDefecto()` |
| `maxCola` | max размер очереди | `1024` |
| `maxPorEtiqueta` | max одновременных задач с одним тегом; `0` = без лимита | `0` |
| `estrategiaColaLlena` | поведение при переполнении общей очереди: `RECHAZAR` или выполнение в потоке вызывающего (`EJECUTOR_LLAMANTE`) | `RECHAZAR` |
| `apagadoEsperaMsMax` | верхняя граница ожидания завершения задач при `detener()` / `close()` | `30000` |

Трёхаргументный конструктор `Politica(workers, maxCola, maxPorEtiqueta)` сохранён для совместимости и эквивалентен вызову с `estrategiaColaLlena = RECHAZAR` и `apagadoEsperaMsMax = 30_000`.

```java
Susurro sus = Susurro.iniciar(new Susurro.Politica(4, 256, 8));
```

### `Prioridad`

Enum трёх уровней. Задачи ALTA всегда обгоняют NORMAL, NORMAL обгоняет BAJA. Внутри уровня — FIFO по монотонно растущему `seq`.

```java
Prioridad.ALTA    // peso=0 — критичные (load chunk, network ack)
Prioridad.NORMAL  // peso=1 — дефолт
Prioridad.BAJA    // peso=2 — фоновая работа (индексация, генерация кэша)
```

### `Etiqueta`

Строковый тег задачи для back-pressure. Не-пустой, до 128 символов. Конвенция:

```java
Etiqueta.de("mi-mod/ia");       // ИИ моба
Etiqueta.de("mi-mod/io");       // чтение-запись
Etiqueta.de("mi-mod/red");      // сетевая работа
Etiqueta.de("mi-mod/renderer"); // подготовка буферов для рендера
```

Если `Politica.maxPorEtiqueta > 0`, пул учитывает текущее количество активных задач с данной этикеткой и отвергает (`RejectedExecutionException` через `Tarea`) новую, если предел достигнут. Это даёт честное разделение ресурса между подсистемами одного мода.

### `Tarea<T>`

Обёртка над `CompletableFuture<T>` с дополнительной семантикой.

**Состояние** (`Tarea.Estado`): `PENDIENTE → EN_EJECUCION → {COMPLETADA, FALLADA, CANCELADA}`. `TimeoutException` из `conPlazo(...)` приравнивается к `CANCELADA`.

**Чтение:**

```java
Tarea<String> t = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("io"), this::leer);

if (t.terminada()) { ... }
Tarea.Estado e = t.estado();
boolean stop = t.revisada();   // worker проверяет cooperative-cancel
```

**Cancel:**

```java
t.cancelar();   // выставляет CANCELADA + CompletableFuture.cancel(false)
```

Пуловый `SusurroRunnable` до запуска задачи сверяется с `revisada()` и возвращается, если отменено.

**Callbacks:**

```java
t.alCompletar(res -> log.info("готово: {}", res))
 .alFallar(err -> log.error("упало", err))
 .mapa(String::length);       // Tarea<Integer>
```

**Deadline:**

```java
t.conPlazo(Duration.ofMillis(200));
// если 200 мс прошли — estado=CANCELADA и future завершается TimeoutException
```

**Marshal результата на главный поток:**

```java
t.enHiloPrincipal(hp, res -> actualizarInventario(res));
// callback исполнится синхронно в ближайшем hp.pulso()
```

**Блокирующее ожидание** (только в тестах/CLI):

```java
String r = t.esperar();                    // throws IE/EE
String r2 = t.esperar(Duration.ofSeconds(1));
```

### `HiloPrincipal`

«Главный поток» — очередь `Runnable`, которую сливает `pulso()`, вызываемый из тика игры.

```java
HiloPrincipal hp = new HiloPrincipal();       // без лимита на pulso
HiloPrincipal safe = new HiloPrincipal(256);  // не более 256 задач за один pulso
```

API:

- `enviar(Runnable)` — кладёт задачу (thread-safe со всех потоков);
- `pulso()` — исполняет накопленное в порядке FIFO, возвращает число выполненных задач; бросает `IllegalStateException`, если вызван из другого потока, чем первый раз;
- `pendientes()` — сколько ждёт;
- `reiniciar()` — для тестов.

Конвенция: движок (`vida-loader`) вызывает `pulso()` ровно один раз на server tick. Мод-авторы — **никогда не вызывайте** это сами; кладите задачи через `enviar(...)` или `Tarea.enHiloPrincipal(hp, ...)`.

## Типичные паттерны

### Heavy-compute → UI-update

```java
public final class MiAventura implements VidaMod {
    private Susurro sus;
    private HiloPrincipal hp;

    @Override public void iniciar(ModContext ctx) {
        this.sus = ctx.susurro();      // provision'ится движком (0.4.x — см. roadmap)
        this.hp  = ctx.hiloPrincipal();

        ctx.latidos().suscribir(LatidoAbrirInventario.TIPO, this::enAbrir);
    }

    private void enAbrir(LatidoAbrirInventario ev) {
        sus.lanzar(Prioridad.NORMAL, Etiqueta.de("miaventura/stats"),
                () -> calcularEstadisticas(ev.jugador()))
           .enHiloPrincipal(hp, stats -> ev.abrirPantalla(stats));
    }
}
```

### Throttled I/O

```java
Susurro.Politica p = new Susurro.Politica(
        /*workers*/ 6,
        /*maxCola*/ 512,
        /*maxPorEtiqueta*/ 4);   // одновременно ≤4 IO-задачи на мод
Susurro sus = Susurro.iniciar(p);
```

### Монитор времени

```java
long t0 = System.nanoTime();
sus.lanzar(Prioridad.BAJA, Etiqueta.de("ejemplo/index"), this::indexar)
   .alCompletar(v -> log.info("индексация: {} мс",
           (System.nanoTime() - t0) / 1_000_000));
```

## Back-pressure: что происходит при переполнении

1. `Politica.maxCola` достигнут → `lanzar` сразу возвращает `Tarea`, чей `futuro` завершён `RejectedExecutionException`. `Estadisticas.rechazadas` инкрементится.
2. `Politica.maxPorEtiqueta > 0` и лимит по этикетке достигнут → то же поведение, но с другим сообщением.
3. `ThreadPoolExecutor` внутренне отверг (очень редко, только на shutdown) → аналогично.

Мод получает структурированный отказ через `Tarea.alFallar(...)`, а не падает с `RejectedExecutionException` из главного потока.

## Потокобезопасность

- `Susurro` — thread-safe во всех public-методах.
- `Tarea` — thread-safe на чтение/cancel; callbacks регистрируются через CAS-free `CompletableFuture`, что защищает от лост-updates.
- `HiloPrincipal.enviar(...)` — thread-safe; `pulso()` — строго из одного потока, владельца.
- `Etiqueta`, `Prioridad`, `Politica`, `Estadisticas` — иммутабельные.

## Производительность

- Пустая очередь + одна задача: ≈ 1.2 мкс от `lanzar` до начала исполнения на M1/JDK 21 (JMH, `bench/`).
- `HiloPrincipal.pulso()` с пустой очередью — одно volatile-чтение; с N задачами — линейно по N, без lock'ов (ConcurrentLinkedQueue).
- Приоритетная очередь — `PriorityBlockingQueue` с компаратором `(peso, seq)`; FIFO внутри приоритета сохраняется.

## Интеграция с `base`

`Ejecutor.susurro(Susurro, Prioridad, Etiqueta)` из [`vida-base`](./base.md#devvidabaselatidos) позволяет явно маршрутизировать слушателей событий в пул:

```java
Ejecutor ej = Ejecutor.susurro(sus, Prioridad.BAJA, Etiqueta.de("miaventura/metrics"));
ctx.latidos().suscribir(TIPO, Prioridad.MONITOR, Fase.DESPUES, ej,
        ev -> enviarMetricaVPN(ev));
```

Подробно — [guides/latidos.md](../guides/latidos.md).

## Тесты

`susurro/src/test/java`:

- `SusurroTest` — порядок по приоритетам, back-pressure по этикетке, переполнение очереди, dead-letter.
- `TareaTest` — состояния, `conPlazo` (включая таймаут → CANCELADA), `enHiloPrincipal`, `mapa`, `alFallar`.
- `HiloPrincipalTest` — FIFO, limit-per-pulso, thread-affinity guard, `reiniciar`.
- `EtiquetaTest` — валидация длины и пустых строк.

~93% покрытия + JMH-бенч `bench/pool/SusurroBench`.

## Что ещё будет в `susurro`

- Work-stealing — для сценариев с большим количеством мелких задач (0.4.x).
- Cancel-propagation вниз по `mapa()` chain.
- `Susurro.lanzarRetrasado(Duration, ...)` — отложенное исполнение без `ScheduledExecutorService`.
- `Ajustes`-контроль политики на рантайме (`vida.susurro.workers=6`).

Сроки — в [session-roadmap: 0.4.0 «DX и наблюдаемость»](../session-roadmap.md#session-2--040--dx-и-наблюдаемость).

## Что читать дальше

- [`base`](./base.md) — `Ejecutor` и `LatidoBus.suscribir(..., Ejecutor, ...)`.
- [guides/latidos.md](../guides/latidos.md) — async-подписчики на основе `Susurro`.
- [architecture/performance.md](../architecture/performance.md) — почему мы избегаем блокирующих вызовов в hot-path.
