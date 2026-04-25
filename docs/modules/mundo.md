# mundo

Публичный **world API** без ссылок на классы Minecraft: снимок `Mundo`, координаты блока/чанка/региона, измерение, биом, **идентификатор блока в клетке и теги блоков** (`EtiquetaBloque` из `vida-bloque`), world-латидосы. JAR ваниллы **не** в compile classpath этого модуля.

Рантайм, который **строит** `Mundo` из существующего vanilla-`Level` и **прикручивает** [`LatidosMundo.Tick`](../../mundo/src/main/java/dev/vida/mundo/latidos/LatidosMundo.java) к шине, — в пакете `dev.vida.platform` модуля `:loader` (например [`MundoNivelVanilla`](../../loader/src/main/java/dev/vida/platform/MundoNivelVanilla.java), вместе с [PlatformBridge / `VanillaBridge`](./loader.md#платформенные-морфы)). Это **внутренняя проводка** между игрой и шиной Latidos, а не публичное расширяемое «супер-API мира» — см. [ниже](#абстракция-и-прямой-доступ-к-minecraft).

- Пакет: `dev.vida.mundo`
- Артефакт: `dev.vida:mundo` (версия через [BOM `dev.vida:vida-bom`](../reference/platform-bom.md), та же, что и у `dev.vida:base` и остальных модулей)
- Стабильность: **`@ApiStatus.Stable`** с 2.0 «Масштаб» (SemVer для публичных типов; см. [api-stability.md](../reference/api-stability.md))

```kotlin
dependencies {
    compileOnly(platform("dev.vida:vida-bom:…"))  // ваша `vida.platform.version`
    compileOnly("dev.vida:mundo")
}
```

`compileOnly` — на рантайме API находится в загрузчике / общем classpath с другими модами.

## Абстракция и прямой доступ к Minecraft

**Что `vida-mundo` *не* даёт (и намеренно):**

- нет пути к `ServerLevel`, `LevelChunkManager`, `ChunkMap`, `NoiseSettings`, `DensityFunction`, генераторам сидов и прочему **движку мира** — только абстракции (`Coordenada`, `ChunkCoordenada` как целочисленные индексы, снимок [`Bioma`](../../mundo/src/main/java/dev/vida/mundo/Bioma.java), [LatidosMundo](../../mundo/src/main/java/dev/vida/mundo/latidos/LatidosMundo.java) на шине);
- [`Mundo`](../../mundo/src/main/java/dev/vida/mundo/Mundo.java) — это **контракт снимка** (день/ночь, биом, «чанк известен», `bloqueRegistradoEn` и т.д.), а не копия surface/chunk-данных 1:1;
- **не** подразумевается, что сюда добавят «витрину» весь noise/chunk-pipeline: это сразу привязало бы публичный API к внутренностям Mojang.

Так сделано, чтобы моды, завязанные **только** на `vida-mundo` / `vida-bloque` / [Latidos](../guides/latidos.md) / [Catalogo](../guides/catalogo.md), жили в одном SemVer, а порт к новой мажорной линии Minecraft — оставался в Vifada-морфах и внешних артефактах, а не в каждом вызове `Mundo`.

**Когда нужен настоящий «прямой» доступ к ванилле (ультимативные моды):**

0. **Публичный слой [cima](cima.md) (`CimaJuego` / `CimaJuegoGlobal#cimaJuego`)** — **Preview**: тот же <strong>Object = Level</strong> и <strong><code>Mundo</code></strong> по тому же пути, что <strong>MundoNivelVanilla</strong> + <strong>LatidosMundo.Tick</strong> в <strong>:loader</strong>; без <strong>import net.minecraft.*</strong> в <strong>API</strong> модуля <strong>cima</strong> (логика на <strong>классах MC</strong> — у вас в JAR, см. cima.md).
1. **Классы `net.minecraft.*` / `com.mojang.*` на рантайме** поднимаются через **Juego** — см. [classloading.md](../architecture/classloading.md#правила-делегирования). Подключаете Minecraft (или [Cartografía](../modules/cartografia.md)-сопоставимые **stubs/реальный JAR**) в Gradle как `compileOnly` + рантайм, и пишете логику против `Level`, `ChunkMap` и т.д. *вне* `vida-mundo` (ваш пакет мода, отдельный модуль).
2. [Vifada](../guides/vifada.md) — [инъекции](../modules/vifada.md) в нужные методы (тик мира, генерация, сеть), где Latidos **не** даёт события.
3. [Puertas](../guides/puertas.md) — расширение доступа к `private`/`protected` без копий рефлекса, если Vifada избыточна.
4. [PlatformBridge / `VanillaBridge`](./loader.md#платформенные-морфы) — **внутренняя** диспатч-точка для <strong>LatidoPulso</strong> / <strong>LatidosMundo.Tick</strong>; <strong>не</strong> подмешивайте свои куски в bridge. Для своей логики: **cima (0)**, при необходимости **(2–3)**, JAR/стабы <strong>(1)</strong>.

Коротко: **абстрактный мир = `vida-mundo`**. **Снимок+сырой Level на том же пути, что и bridge = [cima](cima.md)**. **Свой** глубинный и Game = classpath + cima (по вкусу) + Vifada/Puertas.

## Контракт стабильности

- Публичные типы помечены `@ApiStatus.Stable`; расширение идёт через **новые** `default`-методы в `Mundo` или новые типы в пакете, без ломки существующих `record`-компонентов.
- Фабрики высот `LimitesVerticales.*Vanilla121()` отражают **типичный** профиль Minecraft Java **1.21.x**; кастомные измерения и datapack-измерения могут отличаться — уточнение через реализацию `Mundo` / платформенный мост.
- Упаковка `ChunkCoordenada.empaquetar` совместима с привычным представлением пары `(chunkX, chunkZ)` в одном `long`.

## `Mundo`

Интерфейс снимка мира: идентификатор, измерение, биом в точке, признак загрузки чанка для координаты, цикл дня.

Обязательные методы:

```java
public interface Mundo {
    Identifier id();
    Dimension dimension();
    Bioma biomaEn(Coordenada coordenada);
    boolean estaCargado(Coordenada coordenada);
    long tiempoDelDia();
}
```

Дополнительные **`default`**-методы (совместимое расширение без поломки реализаций):

| Метод | Назначение |
|-------|------------|
| `limitesVerticales()` | Допустимый диапазон `y`; по умолчанию из `dimension().limitesVerticalesPredeterminados()` |
| `enRangoDeAltura(Coordenada)` | Проверка `y` против `limitesVerticales()` |
| `esDeDia()` / `esDeNoche()` | Полутоновый день по `tiempoDelDia()` mod 24000 |
| `bloqueRegistradoEn(Coordenada)` | `Optional<Identifier>` блока в реестре игры; `empty` если чанк не загружен или нет данных |
| `bloqueTieneEtiqueta(Coordenada, EtiquetaBloque)` | принадлежность блока тегу (datapack / синхронизация реестра) |

Зависимость **`vida-bloque`** нужна только для типа тега `EtiquetaBloque`; сами блоки описываются там же (`Bloque`, `RegistroBloques`).

Референсная минимальная реализация для мостов и тестов — `MundoEstatico` (биом / «загруженность» / опционально явные `LimitesVerticales` и фиксированный `bloqueRegistradoEn`).

## Координаты

### `Coordenada`

Блок `(x, y, z)` с `desplazar`, делением на чанк, квадратом расстояния, а также:

- `chunk()` → `ChunkCoordenada`
- `region()` → `RegionCoordenada`

### `ChunkCoordenada`

Горизонталь `(chunkX, chunkZ)`; `empaquetar` / `desempaquetar` / `clave`; `desde(Coordenada)`; `region()` для перехода к региону `.mca`.

### `RegionCoordenada`

Индекс файла региона (**32×32** чанка): `desde(ChunkCoordenada)` и `desde(Coordenada)`, `clave()` для карт и кэшей.

### `LimitesVerticales`

Диапазон **включительно** по `y`: `contiene(int)`, `contiene(Coordenada)`, `alturaSpan()`, фабрики `overworldVanilla121()`, `netherVanilla121()`, `endVanilla121()`, произвольный `de(min, max)`.

## `Dimension`

Поля: `id`, `natural`, `permiteCama`, `techoFijo`; константы `OVERWORLD`, `NETHER`, `END`; фабрики `Dimension.de(...)`.

Метод **`limitesVerticalesPredeterminados()`** возвращает профиль высот Vanilla 1.21.x для известных id; для пользовательских измерений — безопасный fallback как у Overworld (пока мост не задаёт точнее через `Mundo.limitesVerticales()`).

## `Bioma`

`id`, конечная `temperatura`, `humedad` ∈ [0..1], `precipitacion`; помощники `esFrio()`, `tienePrecipitacion()`.

## World-латидосы (`dev.vida.mundo.latidos`)

Класс `LatidosMundo` объединяет типизированные события:

| Запись | Latido id | Поля |
|--------|-----------|------|
| `MundoCargado` | `vida:mundo_cargado` | `mundo`, `recienCreado` |
| `ChunkCargado` | `vida:chunk_cargado` | `mundo`, `chunkX`, `chunkZ`, `completo` |
| `ChunkDescargado` | `vida:chunk_descargado` | `mundo`, `chunkX`, `chunkZ` |
| `Tick` | `vida:mundo_tick` | `mundo`, `tickActual`, `tiempoDelDia` |
| `NocheAmanece` | `vida:noche_amanece` | `mundo`, `tiempoAnterior`, `tiempoActual`, `transicion` |

`Tick` дополняет общий `LatidoPulso` из `vida-base`, когда нужен именно контекст мира. Подписка — через `LatidoBus` и `@EjecutorLatido` (см. [base-ejecutor](./base-ejecutor.md)).

### Пример подписчика

```java
import dev.vida.base.latidos.EjecutorLatido;
import dev.vida.mundo.Mundo;
import dev.vida.mundo.latidos.LatidosMundo;

public final class OyentesMundoEjemplo {

    @EjecutorLatido
    public void alCargar(LatidosMundo.MundoCargado ev) {
        Mundo mundo = ev.mundo();
        if (mundo.dimension().natural()) {
            // ...
        }
    }
}
```

## `@OyenteDeTick`

Shortcut из `vida-base` для троттлинга частоты относительно `LatidoPulso` (см. [guides/latidos](../guides/latidos.md)).

## Потокобезопасность

Все value-types и payload-записи `LatidosMundo` иммутабельны. Поток выполнения обработчика задаётся `Ejecutor` / `@EjecutorLatido`. Конкретная реализация `Mundo` может оборачивать игровой уровень — согласованность полей определяет рантайм.

## Проверка док ↔ API

Ниже — самодостаточный compilation unit для задачи `./gradlew vidaDocTest`:

```java
package dev.vida.docs.mundo;

import dev.vida.core.Identifier;
import dev.vida.mundo.Bioma;
import dev.vida.mundo.ChunkCoordenada;
import dev.vida.mundo.Coordenada;
import dev.vida.mundo.Dimension;
import dev.vida.mundo.LimitesVerticales;
import dev.vida.mundo.Mundo;
import dev.vida.mundo.MundoEstatico;
import dev.vida.mundo.RegionCoordenada;

/** Сводка контракта mundo для vidaDocTest. */
public final class MundoApiStableSnapshot {
    private MundoApiStableSnapshot() {}

    static void ejemplo() {
        Coordenada p = new Coordenada(8, 70, -16);
        ChunkCoordenada ch = p.chunk();
        RegionCoordenada reg = ch.region();
        LimitesVerticales lv = Dimension.OVERWORLD.limitesVerticalesPredeterminados();
        boolean alturaOk = lv.contiene(p);

        Mundo mundo = new MundoEstatico(
                Identifier.of("docs", "stub"),
                Dimension.OVERWORLD,
                18000L,
                new Bioma(
                        Identifier.of("minecraft", "forest"),
                        0.7f,
                        0.5f,
                        Bioma.Precipitacion.LLUVIA),
                false,
                null);

        boolean enAltura = mundo.enRangoDeAltura(p);
        long rk = reg.clave();
        if (alturaOk && enAltura && rk != 0L) {
            // stub
        }
    }
}
```

## Что читать дальше

- [Абстракция и прямой доступ к Minecraft](#абстракция-и-прямой-доступ-к-minecraft) — в этом же файле: зачем нет движка в `vida-mundo` и куда идти за ваниллой.
- [`entidad`](./entidad.md) — сущности и связь с миром.
- [`guides/first-entity.md`](../guides/first-entity.md) — пошаговый пример с `LatidosMundo`.
- [`base-ejecutor`](./base-ejecutor.md) — регистрация обработчиков.
- [`loader`](./loader.md#платформенные-морфы) — `PlatformBridge`, эмиссия `LatidoPulso` / `LatidosMundo.Tick` из тика.
