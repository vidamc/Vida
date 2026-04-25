# vigia

Лёгкий sampling-профайлер поверх JDK Flight Recorder (JFR). Даёт разработчикам модов и операторам серверов быстрый способ найти горячие методы, отслеживать статистику `Susurro` и оценивать нагрузку на шину событий `Latidos`.

- Пакет: `dev.vida.vigia`
- Gradle: `dev.vida:vida-vigia`
- Стабильность: **`@ApiStatus.Stable`** (основной публичный API: `VigiaSesion`, `Resumen`, `VigiaReporte`, `VigiaComando`)

## Зачем

Minecraft-серверы и клиенты часто страдают от «кто-то тормозит тик». Vigia позволяет за 30 секунд собрать профиль и получить человекочитаемый отчёт — без внешних инструментов, без перезапуска.

## Быстрый старт

### В игре (команда)

```
/vida profile start     — начать профилирование
/vida profile dump      — снимок без остановки
/vida profile stop      — остановить, записать .jfr + .html
```

Файлы записываются в `.minecraft/vida/vigia/`.

### В коде мода

```java
VigiaSesion sesion = VigiaSesion.iniciar();
sesion.conSusurro(miSusurro);  // optional: привязать пул

// ... нагрузка ...

Resumen res = sesion.detener(Path.of("vigia-report.jfr"));
VigiaReporte.escribir(res, Path.of("vigia-report.html"));
```

## Ключевые типы

### `VigiaSesion`

Управляет JFR-записью.

| Метод | Что делает |
|-------|------------|
| `iniciar()` | Запускает JFR с конфигурацией «profile» |
| `conSusurro(Susurro)` | Привязывает пул для захвата статистики |
| `instantanea()` | Создаёт `Resumen` без остановки записи |
| `detener(Path)` | Останавливает запись, сохраняет `.jfr`, возвращает `Resumen` |
| `cancelar()` | Останавливает без сохранения |
| `activa()` | Активна ли сессия |

Потокобезопасен. Состояние переходит `ACTIVA → DETENIDA` атомарно.

### `Resumen`

In-memory резюме профилирования (record):

```java
record Resumen(
    Duration duracion,
    long muestras,
    List<MetodoMuestra> topMetodos,       // top-20 горячих методов
    List<LatidoMetrica> latidoMetricas,   // метрики по каналам Latido
    int susurroActivos,
    int susurroPendientes,
    long susurroCompletadas
) { ... }
```

#### `MetodoMuestra`

```java
record MetodoMuestra(String metodo, long muestras, double porcentaje) { }
```

Формат `metodo`: `пакет.Класс#метод`.

#### `LatidoMetrica`

```java
record LatidoMetrica(String nombre, int suscriptores, long emisiones) { }
```

### `VigiaReporte`

Генератор HTML-отчётов:

| Метод | Что делает |
|-------|------------|
| `renderizar(Resumen)` | Возвращает self-contained HTML-строку |
| `escribir(Resumen, Path)` | Записывает HTML в файл |
| `nombreArchivo()` | Генерирует стандартное имя `vigia-report-<ts>.html` |

HTML-отчёт содержит:
- Карточки-summary (duration, samples, Susurro stats)
- Таблицу top-20 методов с CSS-барами (flame-style)
- Таблицу Latido-метрик (если есть)

### `VigiaComando`

Контракт для команды `/vida profile`:

```java
VigiaComando cmd = new VigiaComando(outputDir);
String response = cmd.ejecutar(new String[]{"start"}, susurro);
```

## Интеграция с Susurro

Если вызвать `sesion.conSusurro(susurro)`, в `Resumen` попадут данные из `Susurro.Estadisticas`: активные/ожидающие/завершённые задачи.

## Интеграция с DefaultLatidoBus

Метрики по каналам событий (количество подписчиков, число emit-вызовов) будут расширяться в следующих версиях. В 0.4.0 структура `LatidoMetrica` зафиксирована, но заполняется loader'ом при наличии instrumented bus.

## Тесты

- `ResumenTest` — контракты на иммутабельность и валидацию.
- `VigiaReporteTest` — smoke-тесты HTML-рендера, XSS-escaping, запись файлов.
- `VigiaSesionTest` — lifecycle (start/stop/snapshot), формат top-методов, интеграция с Susurro.
- `VigiaComandoTest` — все субкоманды, обработка ошибок.

## Что читать дальше

- [modules/susurro.md](./susurro.md) — Susurro thread-pool.
- [modules/base.md](./base.md) — система событий Latidos.
- [modules/base-ejecutor.md](./base-ejecutor.md) — Ejecutor и `LatidoRegistrador`.
