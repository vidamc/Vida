# Первый мод за 10 минут

К концу этого руководства у вас будет рабочий мод на Vida, который:

- регистрируется как `miaventura:v0.1.0`,
- подписывается на `LatidoPulso` (тик сервера),
- регистрирует один блок в `Catalogo`,
- читает настройку из `Ajustes`.

Предполагается, что [dev-окружение](./dev-environment.md) уже настроено.

## 1. Создаём проект

```bash
mkdir miaventura && cd miaventura
gradle init --type java-application --java-version 21 \
  --dsl kotlin --package com.ejemplo --project-name miaventura --no-split-project
```

Замените `build.gradle.kts` на шаблон из [dev-environment.md](./dev-environment.md#вариант-b-разработка-мода).

## 2. Главный класс

`src/main/java/com/ejemplo/MiAventura.java`:

```java
package com.ejemplo;

import dev.vida.base.VidaMod;
import dev.vida.base.ModContext;
import dev.vida.base.latidos.Prioridad;
import dev.vida.base.latidos.eventos.LatidoPulso;

public final class MiAventura implements VidaMod {

    @Override
    public void iniciar(ModContext ctx) {
        ctx.log().info("¡Hola, soy {} v{}!", ctx.id(), ctx.version());

        ctx.latidos().suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, pulso -> {
            if (pulso.tickActual() % 20 == 0) {
                ctx.log().debug("secondly tick {}", pulso.tickActual());
            }
        });
    }
}
```

`VidaMod.iniciar` вызывается один раз сразу после загрузки классов мода. Всё, что связано с регистрацией контента и подпиской на события, идёт сюда. Контракт — `[VidaMod](../modules/base.md#vidamod)`.

## 3. Манифест

Плагин генерирует `vida.mod.json` из DSL — вам ничего писать руками не нужно. Убедитесь, что в `build.gradle.kts` прописано:

```kotlin
vida {
    mod {
        id.set("miaventura")
        displayName.set("Mi Aventura")
        authors.add("Ana")
        entrypoint.set("com.ejemplo.MiAventura")
    }
}
```

Собрать:

```bash
./gradlew vidaGenerateManifest vidaValidateManifest build
```

В `build/generated/vida/resources/vida.mod.json` появится:

```json
{
  "schema": 1,
  "id": "miaventura",
  "version": "0.1.0",
  "name": "Mi Aventura",
  "authors": [{"name": "Ana"}],
  "entrypoints": { "main": "com.ejemplo.MiAventura" },
  "dependencies": {},
  "vifada": {},
  "puertas": [],
  "modules": [],
  "incompatibilities": [],
  "custom": {}
}
```

Полная [схема манифеста](../reference/manifest-schema.md).

## 4. Добавляем блок

```java
import dev.vida.base.catalogo.Catalogo;
import dev.vida.base.catalogo.CatalogoClave;
// import Bloque из будущего vida-bloque; пока — заглушка

@Override
public void iniciar(ModContext ctx) {
    Catalogo<Bloque> bloques = ctx.catalogos().obtener(Bloque.CATALOGO);
    bloques.registrar(
        CatalogoClave.de("miaventura", "espada_sagrada"),
        new Bloque(Bloque.Propiedades.piedra())
    );
}
```

Модуль `vida-bloque` пока в разработке — реальный пример появится вместе с ним. Пока используйте `Catalogo` для кастомных, не-vanilla реестров (например, ваших внутренних типов).

## 5. Читаем настройку

Моды получают типизированный `Ajustes` через `ctx.ajustes()`:

```java
import dev.vida.base.ajustes.Ajuste;

int distancia = ctx.ajustes().valor(
    Ajuste.entero("render.distance", 32).min(2).max(64)
);
```

Файл `run/config/miaventura.toml`:

```toml
[render]
distance = 48
```

Значение валидируется по диапазону при первом чтении. Подробнее — [guides/ajustes.md](../guides/ajustes.md).

## 6. Запускаем

```bash
./gradlew vidaRun
```

Плагин запустит dev-клиент с `-javaagent` и вашим JAR в `mods/`. В логе увидите:

```
[INFO ] vida.mod.miaventura — ¡Hola, soy miaventura v0.1.0!
```

## 7. Что дальше

- [Latidos](../guides/latidos.md) — подписки, приоритеты, кастомные события.
- [Catalogo](../guides/catalogo.md) — регистрация контента, `CatalogoClave`, заморозка.
- [Ajustes](../guides/ajustes.md) — профили, синхронизация клиент↔сервер.
- [Vifada](../guides/vifada.md) — модификация байткода Minecraft.
- [API stability](../reference/api-stability.md) — какие аннотации `@Stable` / `@Preview` ставить на свои публичные классы.

Шаблон выше — в репозитории `[vida-examples/miaventura](https://github.com/vidamc/Vida)` (в работе). Он же используется в integration-тестах `gradle-plugin`.