# Первый мод за 10 минут

К концу руководства — рабочий мод, который:

- подключается к платформе по BOM;
- реализует `VidaMod` с подпиской на `LatidoPulso`;
- регистрирует пример блока через `RegistroBloques`;
- читает настройку из `Ajustes` типизированно.

Нужно: [dev-окружение](./dev-environment.md) (JDK 21, Gradle).

## 1. Проект

Опора — **[`templates/starter-mod`](../../templates/starter-mod/README.md)**: скопируйте каталог, задайте `vida.platform.version` / `vida.plugin.version` в `gradle.properties` (как в шаблоне) и при необходимости переименуйте `id` мода, пакет и класс entrypoint.

Альтернатива: свой Gradle-проект с плагином `id("dev.vida.mod")` — см. [dev-environment, вариант B](./dev-environment.md#вариант-b-разработка-мода) и [modder-toolkit](../guides/modder-toolkit.md).

## 2. Точка входа

`src/main/java/com/ejemplo/miaventura/MiAventura.java`:

```java
package com.ejemplo.miaventura;

import dev.vida.base.ModContext;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.Prioridad;
import dev.vida.base.latidos.eventos.LatidoPulso;

public final class MiAventura implements VidaMod {

    @Override
    public void iniciar(ModContext ctx) {
        ctx.log().info("¡Hola, {} v{}!", ctx.metadata().nombre(), ctx.version());

        ctx.latidos().suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, pulso -> {
            if (pulso.tickActual() % 20L == 0L) {
                ctx.log().debug("tick de un segundo: {}", pulso.tickActual());
            }
        });
    }
}
```

`VidaMod.iniciar` вызывается один раз после загрузки классов мода. Подписки на Latidos и регистрация в Catalogo — обычно здесь. См. [VidaMod](../modules/base.md#vidamod).

## 3. Манифест

Плагин `dev.vida.mod` генерирует `vida.mod.json` в ресурсы; при `injectDefaultVidaDependency` в зависимости подставляется `vida` (диапазон — см. [modder-toolkit](../guides/modder-toolkit.md)). Пример `vida { mod { ... } }` (идентично идее шаблона):

```kotlin
vida {
    injectDefaultVidaDependency.set(true)
    mod {
        id.set("miaventura")
        displayName.set("Mi Aventura")
        description.set("Ejemplo")
        authors.add("Ana")
        license.set("Apache-2.0")
        entrypoint.set("com.ejemplo.miaventura.MiAventura")
    }
}
```

```bash
./gradlew vidaGenerateManifest vidaValidateManifest build
```

Итоговый фрагмент (точные поля зависят от версии плагина — детали в [схеме `vida.mod.json`](../reference/manifest-schema.md)):

```json
{
  "schema": 1,
  "id": "miaventura",
  "version": "0.1.0",
  "name": "Mi Aventura",
  "authors": [{ "name": "Ana" }],
  "entrypoints": { "main": "com.ejemplo.miaventura.MiAventura" },
  "dependencies": {
    "required": { "vida": "^1.0.0" }
  },
  "vifada": { "morphs": [] }
}
```

## 4. Блок (`vida-bloque`)

Реестр блоков — [RegistroBloques](../modules/bloque.md#registrobloques) поверх [Catalogo](../guides/catalogo.md), не сырой `Catalogo` без обёртки.

```java
import dev.vida.bloque.Bloque;
import dev.vida.bloque.MaterialBloque;
import dev.vida.bloque.PropiedadesBloque;
import dev.vida.bloque.registro.RegistroBloques;
import dev.vida.core.Identifier;

// dentro de iniciar:
RegistroBloques registro = RegistroBloques.conectar(ctx.catalogos(), "miaventura");
Bloque roca = new Bloque(
    Identifier.of("miaventura", "piedra_santa"),
    PropiedadesBloque.con(MaterialBloque.PIEDRA).dureza(2.0f).construir());
registro.registrarOExigir(roca);
```

В `build.gradle.kts` к зависимостям добавьте (через тот же BOM, что и `base`):

```kotlin
compileOnly("dev.vida:bloque")
```

## 5. Настройка (`Ajustes`)

```java
import dev.vida.base.ajustes.Ajuste;

int distancia = ctx.ajustes().valor(
    Ajuste.entero("render.distance", 32).min(2).max(64)
);
```

`run/config/miaventura.toml` (путь к профилю может отличаться в зависимости от лаунчера):

```toml
[render]
distance = 48
```

Подробнее: [guides/ajustes.md](../guides/ajustes.md).

## 6. Запуск

```bash
./gradlew vidaRun
```

`vidaRun` — задача плагина: клиент/агент и `mods` настраиваются в DSL (см. [gradle-plugin](../../gradle-plugin/src/main/java/dev/vida/gradle/package-info.java)). В логе должна появиться строка инициализации мода.

## 7. Дальше

- [Latidos](../guides/latidos.md) — приоритеты, `Ejecutor`, аннотации.
- [Catalogo](../guides/catalogo.md) — `CatalogoClave`, freeze.
- [Ajustes](../guides/ajustes.md) — профили, синхронизация.
- [Vifada](../guides/vifada.md) — Vifada-морфы.
- [API stability](../reference/api-stability.md) — `@Stable` / `@Preview` / `@Internal`.

Шаблон с исходниками: [`templates/starter-mod`](../../templates/starter-mod).
