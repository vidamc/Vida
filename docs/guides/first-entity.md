# Первая сущность

Минимальный walkthrough по `vida-entidad` и `vida-mundo`: регистрируем свой тип сущности, подписываемся на world-латидосы и используем `@OyenteDeTick`, чтобы выполнять логику с контролируемой частотой.

## Что мы сделаем

В примере ниже мод добавляет сущность `luciernaga`:

- категория `AMBIENTAL`;
- маленький hitbox;
- базовые data-components (`Salud`, `Brillo`);
- подписчик на `MundoCargado`;
- tick-обработчик раз в секунду.

## 1. Подключите зависимости

```kotlin
dependencies {
    compileOnly("dev.vida:vida-base:0.5.0")
    compileOnly("dev.vida:vida-entidad:0.5.0")
    compileOnly("dev.vida:vida-mundo:0.5.0")
}
```

## 2. Опишите сущность

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
                    .grupoIa(PropiedadesEntidad.GrupoIa.VOLADOR)
                    .grupoIa(PropiedadesEntidad.GrupoIa.PASIVO)
                    .componente(ClaveComponenteEntidad.SALUD,
                            new ComponenteEntidad.Salud(4.0, 4.0))
                    .componente(ClaveComponenteEntidad.BRILLO,
                            new ComponenteEntidad.Brillo(true))
                    .construir());

    private EntidadesBosque() {}
}
```

## 3. Зарегистрируйте её в `iniciar`

```java
package bosque;

import dev.vida.base.ModContext;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.LatidoRegistrador;
import dev.vida.entidad.registro.RegistroEntidades;

public final class BosqueMod implements VidaMod {
    @Override
    public void iniciar(ModContext ctx) {
        RegistroEntidades entidades = RegistroEntidades.conectar(ctx.catalogos(), "bosque");
        entidades.registrarOExigir(EntidadesBosque.LUCIERNAGA);

        LatidoRegistrador.registrarEnObjeto(ctx.latidos(), this);
        ctx.log().info("Entidad registrada: {}", EntidadesBosque.LUCIERNAGA.id());
    }
}
```

Если ваш обработчик требует `Susurro` или `HiloPrincipal`, используйте перегрузку `registrarEnObjeto(bus, instance, susurro, hp)`.

## 4. Подпишитесь на мир и тики

```java
package bosque;

import dev.vida.base.latidos.OyenteDeTick;
import dev.vida.mundo.latidos.LatidosMundo;

public final class BosqueMod implements VidaMod {
    // ...

    @OyenteDeTick(tps = 1)
    public void limpiarCacheCadaSegundo(dev.vida.base.latidos.eventos.LatidoPulso ev) {
        // здесь удобно держать недорогую housekeeping-логику
    }

    @dev.vida.base.latidos.EjecutorLatido
    public void alCargarMundo(LatidosMundo.MundoCargado ev) {
        if (ev.mundo().dimension().natural()) {
            // например, подготовить spawn-таблицы только для natural-миров
        }
    }
}
```

Когда нужен world-context, выбирайте `LatidosMundo.Tick`. Когда нужен общий heartbeat без привязки к конкретному миру, достаточно `@OyenteDeTick` или `LatidoPulso`.

## 5. Держите модель декларативной

`vida-entidad` в `0.5.0` описывает **тип** сущности, а не конкретный runtime-instance. Хорошая практика:

- id, hitbox, масса, AI-группы и дефолтные components объявляются как константы;
- подписчики на world events живут рядом с модом или в отдельном listener-классе;
- любая тяжёлая логика уходит в `Susurro`, а в тике остаются только короткие решения.

## Частые ошибки

- `masa <= 0` или нулевой `hitbox` — будут отброшены валидацией `PropiedadesEntidad`.
- Регистрация после `congelar()` — вернёт `CatalogoError`.
- `@OyenteDeTick` на методе без параметра `LatidoPulso` — ошибка биндинга.
- Попытка описывать runtime-состояние сущности внутри `Entidad` — неправильный уровень абстракции; для этого нужен world/runtime bridge.

## Куда дальше

- [`modules/entidad.md`](../modules/entidad.md) — полный обзор entity API.
- [`modules/mundo.md`](../modules/mundo.md) — мир, координаты и world-латидосы.
- [`guides/latidos.md`](./latidos.md) — приоритеты, фазы и `Ejecutor`.
