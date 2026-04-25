# base

Публичный моддинг-API Vida. Модуль, с которым работает 99% мод-авторов.

- Пакет: `dev.vida.base`
- Gradle: `dev.vida:vida-base`
- Стабильность: **`@ApiStatus.Stable`** с 1.0.0

В `build.gradle.kts` мода:

```kotlin
dependencies {
    compileOnly("dev.vida:vida-base:0.1.0")
}
```

`compileOnly` — потому что в рантайме `vida-base` уже живёт в `JuegoLoader` (см. [architecture/classloading.md](../architecture/classloading.md)). Запаковывать его в JAR мода не нужно.

## Главные интерфейсы

### `VidaMod`

Точка входа мода. Каждый мод реализует ровно один класс:

```java
public final class MiAventura implements VidaMod {
    @Override
    public void iniciar(ModContext ctx) {
        ctx.log().info("¡Hola!");
    }
}
```

Контракт:

1. Vida находит класс-entrypoint из `vida.mod.json` (`entrypoints.main`).
2. Создаёт экземпляр через no-args-конструктор.
3. Вызывает `iniciar(ctx)` — один раз.
4. Мод переходит в `INICIADO → ACTIVO`.
5. При shutdown — `detener(ctx)` (дефолт no-op).

Методы не должны блокироваться надолго: инициализация синхронна. Тяжёлые задачи — в `LatidoArranque` / `Susurro`.

### `ModContext`

«Паспорт мода». Единственный объект, который мод получает от загрузчика:

```java
public interface ModContext {
    String id();
    Version version();
    ModMetadata metadata();

    LatidoBus latidos();
    CatalogoManejador catalogos();
    AjustesTipados ajustes();
    Log log();

    Path directorioDatos();
}
```

Контекст НЕ даёт доступ к другим модам напрямую — это сознательно. Кросс-модные взаимодействия идут только через публичные API (события, реестры).

### Снимок разрешённых модов (`ModulosInstaladosGlobal`)

Список установленных модов как **DTO манифеста** (`dev.vida.manifest.ModManifest`) без зависимости от `dev.vida.loader` — для UI, телеметрии мода, отладки:

```java
package dev.vida.docs.codegen.modulos_instalados;

import dev.vida.base.ModulosInstaladosGlobal;
import dev.vida.manifest.ModManifest;
import java.util.List;

public final class ListarModsEjemplo {
    private ListarModsEjemplo() {}

    public static void imprimirIds() {
        List<ModManifest> mods = ModulosInstaladosGlobal.vista();
        for (ModManifest m : mods) {
            System.out.println(m.id() + " " + m.version());
        }
    }
}
```

## Подпакеты

### `dev.vida.base.latidos`

Высокопроизводительная система событий. `DefaultLatidoBus` доставляет события прямыми вызовами `Oyente` по снимку подписчиков — **без `Method.invoke` на пути `emitir`**. `LatidoRegistrador` после сканирования аннотаций по возможности использует **`MethodHandle`** для вызова методов мода.

Ключевые типы:

- `LatidoBus` — шина событий. Один экземпляр на рантайм.
- `Latido<E>` — типизированный handle события.
- `Oyente<E>` — функциональный интерфейс обработчика.
- `Prioridad` — `URGENTE → ALTA → NORMAL → BAJA → MONITOR`.
- `Fase` — подпорядок внутри приоритета: `ANTES → PRINCIPAL → DESPUES`.
- `Ejecutor` — **новое в 0.3.0** — стратегия исполнения обработчика (см. ниже «Latidos profundos»).

Использование:

```java
ctx.latidos().suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, pulso -> {
    if (pulso.tickActual() % 20 == 0) {
        ctx.log().debug("секундный пульс");
    }
});
```

Создание собственного события:

```java
public record MiEvento(String razon, boolean cancelable) implements Latido {
    public static final LatidoTipo<MiEvento> TIPO = LatidoTipo.of("miaventura:mi_evento");
}

// публикация
ctx.latidos().publicar(new MiEvento("boot-done", false));
```

Подробно — [guides/latidos.md](../guides/latidos.md).

#### Latidos profundos (`Ejecutor`, 0.3.0+)

`Ejecutor` — SAM-интерфейс, описывающий _в каком потоке_ вызвать обработчик. Раньше всё было неявно синхронно в потоке `emitir(...)`. Теперь можно явно разделить:

```java
// Синхронно (как раньше, дефолт)
bus.suscribir(Latidos.PULSO, Prioridad.NORMAL, this::onPulse);

// На следующем пульсе главного потока
bus.suscribir(Latidos.PULSO, Prioridad.NORMAL, Fase.PRINCIPAL,
        Ejecutor.hiloPrincipal(ctx.hiloPrincipal()),
        this::onPulse);

// В пуле Susurro с заданным приоритетом и этикеткой
bus.suscribir(Latidos.EXPLOSION, Prioridad.MONITOR, Fase.DESPUES,
        Ejecutor.susurro(sus, Prioridad.BAJA, Etiqueta.de("mi-mod/metrics")),
        this::telemetria);

// Строго сериализованный worker (один поток, FIFO)
bus.suscribir(Latidos.CARGA_CHUNK, Prioridad.NORMAL, Fase.PRINCIPAL,
        Ejecutor.serializado("mi-mod/chunk-post"),
        this::escribirEnDisco);
```

Фабрики `Ejecutor`:

| Фабрика | Где исполняется |
|---------|-----------------|
| `Ejecutor.SINCRONO` | в потоке `emitir` (дефолт) |
| `Ejecutor.hiloPrincipal(HiloPrincipal)` | в ближайшем `pulso()` главного потока |
| `Ejecutor.susurro(Susurro, Prioridad, Etiqueta)` | в [`vida-susurro`](./susurro.md) пуле |
| `Ejecutor.susurro(Susurro)` | то же, с `NORMAL`+`"vida/latido"` |
| `Ejecutor.serializado(nombre)` | собственный single-thread worker, FIFO |

Контракт:

- ошибки внутри `ejecutar(...)` не всплывают в шину; их логирует `DefaultLatidoBus`;
- для `SINCRONO` обработчик видит тот же стек, что и `emitir`; для остальных — независимый стек;
- `MONITOR`-подписчик может быть async — это типичный кейс для телеметрии, где не критично порядок vs. скорость.

Подробно — [guides/latidos.md](../guides/latidos.md#latidos-profundos--ejecutor) и [modules/susurro.md](./susurro.md).

### `dev.vida.base.latidos.eventos`

Стандартные события Vida:

- `LatidoArranque` — клиент/сервер готовы.
- `LatidoPulso` — server tick (`tickActual()`, `duracionNanos()`).
- `LatidoParada` — shutdown.
- `LatidoConfiguracionRecargada` — перезагрузка конфига.

Точный список — в Javadoc `dev.vida.base.latidos.eventos`.

### `dev.vida.base.catalogo`

Типизированные реестры контента.

Ключевые типы:

- `CatalogoManejador` — «менеджер менеджеров»; через него мод получает конкретные реестры.
- `Catalogo<T>` — реестр одного типа (блоки, предметы, звуки).
- `CatalogoClave` — namespaced-ключ (эквивалент `Identifier` с валидацией).

Использование:

```java
Catalogo<Bloque> bloques = ctx.catalogos().obtener(Bloque.CATALOGO);
bloques.registrar(
    CatalogoClave.de("miaventura", "espada_sagrada"),
    new Bloque(Bloque.Propiedades.piedra())
);
```

Контракт:

- регистрации разрешены только в фазе `INICIADO` (до перехода в `ACTIVO`);
- после заморозки (перед первым тиком игры) `registrar(...)` бросает `IllegalStateException`;
- дубликаты отклоняются.

Подробно — [guides/catalogo.md](../guides/catalogo.md).

### `dev.vida.base.ajustes`

Типизированные настройки поверх `config`.

Ключевые типы:

- `Ajuste<T>` — схема одной настройки с дефолтом и валидаторами.
- `AjustesTipados` — фасад, scoped под конкретный мод.

Использование:

```java
public static final Ajuste<Integer> RENDER_DIST =
    Ajuste.entero("render.distance", 32).min(2).max(64);

int d = ctx.ajustes().valor(RENDER_DIST);
```

Подробно — [guides/ajustes.md](../guides/ajustes.md).

## Потокобезопасность

- `LatidoBus` — thread-safe на чтение и публикацию. По умолчанию обработчик вызывается в потоке `emitir(...)`; явное переопределение через `Ejecutor` переносит исполнение в main-thread / `Susurro` / serialized worker.
- `Catalogo` — thread-safe на чтение (после заморозки); регистрации — только из главного потока инициализации.
- `Ajustes` — иммутабельный снимок, thread-safe by construction.
- `Log` — thread-safe.

## Что ещё будет в `base`

- `AjustesSincronizados` — настройки с авто-синхронизацией клиент↔сервер.
- `DirectorioRecursos` — API для модами-добавляемых ресурсов (подготовка к `Fuente`).
- Интеграция с `vida-red` (`TejidoCanal`) на уровне `ModContext` для унифицированного доступа к сетевому каналу.
- Дальнейшее развитие `LatidoRegistrador` / `@OyenteDeTick` — см. [roadmap](../roadmap.md).

Точные сроки — в [roadmap](../roadmap.md) и [session-roadmap](../session-roadmap.md).

## Что читать дальше

- [guides/latidos.md](../guides/latidos.md) · [guides/catalogo.md](../guides/catalogo.md) · [guides/ajustes.md](../guides/ajustes.md)
- [modules/susurro.md](./susurro.md) · [modules/bloque.md](./bloque.md) · [modules/objeto.md](./objeto.md) — модули 0.3.0, построенные поверх `base`.
- [getting-started/first-mod.md](../getting-started/first-mod.md) — полный пример мода.
- [reference/api-stability.md](../reference/api-stability.md) — политика `@Stable` / `@Preview` для модулей Vida.
