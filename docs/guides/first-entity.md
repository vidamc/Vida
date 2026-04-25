# Первая сущность

Кратко: `vida-entidad` (тип `Entidad` + [RegistroEntidades](../../entidad/src/main/java/dev/vida/entidad/registro/RegistroEntidades.java)) и `vida-mundo` (world-латидосы в [LatidosMundo](../../mundo/src/main/java/dev/vida/mundo/latidos/LatidosMundo.java)). Пример: сущность «luciernaga», подписка на `MundoCargado`, троттлинг раз в секунду через `@OyenteDeTick`.

Версии артефактов `dev.vida:*` задайте **одной** линией BOM — например `vida.platform.version` в `gradle.properties` (тот же номер, что у опубликованного [vida-bom](../reference/platform-bom.md)).

## 1. Зависимости

```kotlin
val vidaPlatformVersion = providers.gradleProperty("vida.platform.version").get()

dependencies {
    compileOnly(platform("dev.vida:vida-bom:$vidaPlatformVersion"))
    compileOnly("dev.vida:base")
    compileOnly("dev.vida:entidad")
    compileOnly("dev.vida:mundo")
}
```

(Имена артефактов — `entidad` / `mundo`, не префикс `vida-` в coordinate.)

## 2. Тип сущности

`Entidad` хранит `Identifier`, [TipoEntidad](../../entidad/src/main/java/dev/vida/entidad/TipoEntidad.java) и [PropiedadesEntidad](../../entidad/src/main/java/dev/vida/entidad/PropiedadesEntidad.java) (масса, hitbox, группы IA, data-компоненты).

```java
package bosque;

import dev.vida.core.Identifier;
import dev.vida.entidad.Entidad;
import dev.vida.entidad.PropiedadesEntidad;
import dev.vida.entidad.TipoEntidad;
import dev.vida.entidad.componentes.ClaveComponenteEntidad;
import dev.vida.entidad.componentes.ComponenteEntidad;

final class EntidadesBosque {
    static final Entidad LUCIERNAGA = new Entidad(
            Identifier.of("bosque", "luciernaga"),
            TipoEntidad.AMBIENTAL,
            PropiedadesEntidad.con()
                    .masa(0.2)
                    .hitbox(0.25, 0.20, 0.25)
                    .gruposIa(
                            PropiedadesEntidad.GrupoIa.VOLADOR,
                            PropiedadesEntidad.GrupoIa.PASIVO)
                    .componente(ClaveComponenteEntidad.SALUD, new ComponenteEntidad.Salud(4.0, 4.0))
                    .componente(ClaveComponenteEntidad.BRILLO, new ComponenteEntidad.Brillo(true))
                    .construir());

    private EntidadesBosque() {}
}
```

`masa` и габариты hitbox’а должны быть **> 0** — иначе валидация `PropiedadesEntidad` бросит до регистрации.

## 3. Регистрация, мир и тики

`RegistroEntidades.conectar` принимает `CatalogoManejador` из `ctx.catalogos()` и namespace по умолчанию.

`LatidoRegistrador.registrarEnObjeto(bus, this)` сканирует `@EjecutorLatido` и `@OyenteDeTick`. Для сценариев с Susurro / Hilo principal — перегрузка с `Susurro` и `HiloPrincipal` (см. javadoc [LatidoRegistrador](../../base/src/main/java/dev/vida/base/latidos/LatidoRegistrador.java)).

- `@OyenteDeTick` — к событию [LatidoPulso](../../base/src/main/java/dev/vida/base/latidos/eventos/LatidoPulso.java) (параметр метода — `LatidoPulso`) с нужным `tps`.
- `@EjecutorLatido` — у типа аргумента `E` должен быть `static final Latido<E> TIPO` (как у [LatidosMundo.MundoCargado](../../mundo/src/main/java/dev/vida/mundo/latidos/LatidosMundo.java)).

```java
package bosque;

import dev.vida.base.ModContext;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.EjecutorLatido;
import dev.vida.base.latidos.LatidoRegistrador;
import dev.vida.base.latidos.OyenteDeTick;
import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.entidad.registro.RegistroEntidades;
import dev.vida.mundo.latidos.LatidosMundo;

public final class BosqueMod implements VidaMod {
    @Override
    public void iniciar(ModContext ctx) {
        RegistroEntidades entidades = RegistroEntidades.conectar(ctx.catalogos(), "bosque");
        entidades.registrarOExigir(EntidadesBosque.LUCIERNAGA);
        LatidoRegistrador.registrarEnObjeto(ctx.latidos(), this);
        ctx.log().info("Entidad: {}", EntidadesBosque.LUCIERNAGA.id());
    }

    @OyenteDeTick(tps = 1)
    public void limpiarCacheCadaSegundo(LatidoPulso ev) {
        // housekeeping; тяжёлое — в Susurro
    }

    @EjecutorLatido
    public void alCargarMundo(LatidosMundo.MundoCargado ev) {
        if (ev.mundo().dimension().natural()) {
            // логика только для «естественных» измерений
        }
    }
}
```

Для тика **конкретного** мира без опоры на глобальный [LatidoPulso](../../base/src/main/java/dev/vida/base/latidos/eventos/LatidoPulso.java) смотрите [LatidosMundo.Tick](../../mundo/src/main/java/dev/vida/mundo/latidos/LatidosMundo.java) — отдельный `Latido` и подписка на `ctx.latidos()`.

## 4. Модель

`Entidad` описывает **тип** в [Catalogo](../catalogo.md), а не сущности в save. Состояние в мире — зона поставщика моста / плоскости данных; в API это отражено в [package-info `entidad`](../../entidad/src/main/java/dev/vida/entidad/package-info.java).

## 5. Частые ошибки

- `masa` / hitbox: нулевые или невалидные значения — [IllegalArgumentException](../../entidad/src/main/java/dev/vida/entidad/PropiedadesEntidad.java) в билдере.
- Регистрация в каталоге после [congelar()](../../base/src/main/java/dev/vida/base/catalogo/CatalogoMutable.java) — [CatalogoError](../../base/src/main/java/dev/vida/base/catalogo/CatalogoError.java).
- `@OyenteDeTick` с сигнатурой, отличной от [ожидаемой](../../base/src/main/java/dev/vida/base/latidos/OyenteDeTick.java) (параметр `LatidoPulso`) — сбой при [разборе](../../base/src/main/java/dev/vida/base/latidos/LatidoRegistrador.java).
- [@EjecutorLatido](../../base/src/main/java/dev/vida/base/latidos/EjecutorLatido.java) — у класса `E` нет `static final Latido<E> TIPO` → [LatidoRegistradorError](../../base/src/main/java/dev/vida/base/latidos/LatidoRegistradorError.java).

## 6. Ссылки

- [modules/entidad.md](../modules/entidad.md) — entidad API.
- [modules/mundo.md](../modules/mundo.md) — `Mundo`, `Coordenada`, [LimitesVerticales](../../mundo/src/main/java/dev/vida/mundo/LimitesVerticales.java), [LatidosMundo](../../mundo/src/main/java/dev/vida/mundo/latidos/LatidosMundo.java).
- [latidos.md](./latidos.md) — фазы, приоритеты, [Ejecutor](../../base/src/main/java/dev/vida/base/latidos/Ejecutor.java).
