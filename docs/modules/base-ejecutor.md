# base — Ejecutor и reflection-биндер

Расширение модуля `vida-base`: потоко-осознанные обработчики событий (`Ejecutor`), авто-регистрация подписчиков через `LatidoRegistrador` и shortcut-аннотация `@OyenteDeTick`.

- Пакет: `dev.vida.base.latidos`
- Gradle: `dev.vida:vida-base`
- Стабильность: `@ApiStatus.Preview("base")`

## Зачем

В 0.1.x все обработчики событий выполнялись синхронно в потоке `emitir`. Это безопасно, но тяжёлые задачи (сетевой I/O, сериализация чанков) блокируют игровой тик. `Ejecutor` (0.3.0) дал явный контроль над потоком выполнения. `LatidoRegistrador` (0.4.0) убирает boilerplate — подписчики регистрируются автоматически из аннотаций.

## Ejecutor

SAM-интерфейс стратегии исполнения:

```java
@FunctionalInterface
public interface Ejecutor {
    void ejecutar(Runnable r);
}
```

### Встроенные фабрики

| Фабрика | Поведение |
|---------|-----------|
| `Ejecutor.SINCRONO` | Немедленно `r.run()` в вызывающем потоке |
| `Ejecutor.hiloPrincipal(hp)` | На следующем `pulso()` главного потока |
| `Ejecutor.susurro(s, prioridad, etiqueta)` | В пуле Susurro с приоритетом |
| `Ejecutor.serializado(nombre)` | Однопоточный FIFO (для тестов и строгого порядка) |

### Пример (ручная подписка)

```java
HiloPrincipal hp = new HiloPrincipal();
Susurro susurro = Susurro.iniciar();

bus.suscribir(MiEvento.TIPO, Prioridad.NORMAL, Fase.PRINCIPAL,
        Ejecutor.hiloPrincipal(hp),
        ev -> { /* будет вызван на main-thread при hp.pulso() */ });

bus.suscribir(MiEvento.TIPO, Prioridad.NORMAL, Fase.PRINCIPAL,
        Ejecutor.susurro(susurro),
        ev -> { /* будет вызван в пуле vida-susurro-N */ });
```

## @EjecutorLatido

Аннотация-маркер для метода-подписчика:

```java
@EjecutorLatido(
    kind = Kind.HILO_PRINCIPAL,
    prioridadBus = PrioridadBus.ALTA,   // приоритет подписки (URGENTE..MONITOR)
    fase = FaseBus.ANTES                 // фаза (ANTES/PRINCIPAL/DESPUES)
)
public void alCargarMundo(CargaMundoLatido ev) { ... }
```

### Параметры

| Параметр | Тип | По умолчанию | Назначение |
|----------|-----|--------------|------------|
| `kind` | `Kind` | `SINCRONO` | Стратегия исполнения |
| `etiqueta` | `String` | `"vida/latido"` | Back-pressure тег для SUSURRO |
| `prioridad` | `Prioridad` | `NORMAL` | Приоритет задачи в Susurro |
| `prioridadBus` | `PrioridadBus` | `NORMAL` | Приоритет подписки в шине |
| `fase` | `FaseBus` | `PRINCIPAL` | Фаза подписки в шине |

## @OyenteDeTick

Shortcut над подпиской на `LatidoPulso`: биндер сам привязывает метод к
`LatidoPulso.TIPO` и вызывает его с частотным throttling'ом.

```java
@OyenteDeTick(tps = 1, kind = Kind.SUSURRO, etiqueta = "mi-mod/cache")
public void limpiarCache(LatidoPulso ev) { ... }
```

Правила:

- параметр метода должен быть ровно один и иметь тип `LatidoPulso`;
- `tps` лежит в диапазоне `[1..20]`;
- вызываются только корневые тики (`profundidad == 0`);
- executor-параметры (`kind`, `prioridad`, `etiqueta`, `prioridadBus`, `fase`) зеркалят `@EjecutorLatido`.

## LatidoRegistrador

Reflection-биндер: сканирует объект и подписывает аннотированные методы.

```java
public final class LatidoRegistrador {
    static List<Suscripcion> registrarEnObjeto(
            LatidoBus bus, Object instance,
            Susurro susurro, HiloPrincipal hp);

    static List<Suscripcion> registrarEnObjeto(
            LatidoBus bus, Object instance);  // только SINCRONO
}
```

### Алгоритм привязки

1. Сканирует `instance.getClass()` на методы с `@EjecutorLatido` и `@OyenteDeTick`.
2. Валидирует сигнатуру: **ровно один параметр** типа `E`.
3. Ищет `static final Latido<E>` на классе `E` (конвенция: поле `TIPO`).
4. Если не найден — ищет в классе-владельце метода.
5. Создаёт `Ejecutor` по `kind()`.
6. Подписывает на шину с `prioridadBus()` и `fase()`.

### Пример

```java
public class MiMod {
    @EjecutorLatido(kind = Kind.HILO_PRINCIPAL)
    public void alArrancar(LatidoArranque ev) {
        System.out.println("Vida started: " + ev.vidaVersion());
    }

    @EjecutorLatido(kind = Kind.SUSURRO, etiqueta = "mi-mod/io")
    public void alPulso(LatidoPulso ev) {
        guardarEstadisticas();
    }
}

// Одна строка — все подписки:
var subs = LatidoRegistrador.registrarEnObjeto(bus, new MiMod(), susurro, hp);

// Для отписки:
subs.forEach(Suscripcion::cancelar);
```

## Ошибки: LatidoRegistradorError

Типизированная sealed-иерархия:

| Ошибка | Причина |
|--------|---------|
| `FirmaInvalida` | Метод имеет ≠ 1 параметр |
| `LatidoNoEncontrado` | Нет `Latido<E>` ни на классе события, ни на владельце |
| `LatidoAmbiguo` | Несколько подходящих `Latido`-полей |
| `EjecutorFaltante` | Указан SUSURRO/HILO_PRINCIPAL, но не передан Susurro/HiloPrincipal |
| `TipoIncompatible` | Тип параметра не совместим с `Latido.claseEvento()` |
| `AnotacionesConflictivas` | На одном методе одновременно стоят `@EjecutorLatido` и `@OyenteDeTick` |
| `TpsInvalido` | У `@OyenteDeTick` указан `tps` вне `[1..20]` |

Все ошибки содержат `metodo()` — ссылку на проблемный метод для диагностики.

## Тесты

- `LatidoRegistradorTest` — синхронная/async регистрация, множественные методы, отмена подписок, `@OyenteDeTick`, все типы ошибок, интеграция с Susurro и HiloPrincipal.
- `EjecutorTest` — контракты Ejecutor (inline, marshalling, async, FIFO serializado).

## Что читать дальше

- [modules/base.md](./base.md) — общий обзор модуля base.
- [modules/susurro.md](./susurro.md) — Susurro thread-pool.
- [guides/latidos.md](../guides/latidos.md) — практическое руководство по событиям.
- [modules/vigia.md](./vigia.md) — профайлер Vigia.
