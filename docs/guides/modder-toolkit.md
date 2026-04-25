# Полный набор моддера Vida

Одна страница со всем, что нужно для типичного мода: Gradle DSL, задачи, JVM-свойства, зависимости и отладка. Детали по подсистемам — в ссылках.

## Gradle-плагин `dev.vida.mod`

Подключение:

```kotlin
plugins {
    id("dev.vida.mod") version "<версия из Plugin Portal>"
}
```

### Блок `vida { }`

| Блок | Назначение |
|------|------------|
| `mod { }` | `vida.mod.json`: id, версия, entrypoints, зависимости, Vifada, Puertas, Escultores, modules |
| `minecraft { }` | Клиентский JAR, маппинги для `vidaRemapJar` |
| `run { }` | `vidaRun`: агент, рабочая папка, JVM/программные аргументы, **политика доступа** |

### Автозависимость на платформу

По умолчанию при генерации манифеста добавляется `dependencies.required.vida`, если вы **сами** не указали зависимость на `vida`:

```kotlin
vida {
    injectDefaultVidaDependency.set(true)        // по умолчанию
    defaultVidaDependencyRange.set("^0.1.0")    // диапазон SemVer
}
```

Отключение (например мета-мод или тестовый проект без рантайма Vida):

```kotlin
vida { injectDefaultVidaDependency.set(false) }
```

В Maven/Gradle зависимости всё равно нужно объявить на артефакт `dev.vida:vida-base` (и др. API-модули) — см. [dev-environment](../getting-started/dev-environment.md).

### Задачи

| Задача | Действие |
|--------|----------|
| `vidaGenerateManifest` | Генерирует `build/generated/vida/resources/vida.mod.json` |
| `vidaValidateManifest` | Проверка против парсера манифеста |
| `vidaValidatePuertas` / `vidaPackagePuertas` | `.puertas`-файлы доступа |
| `vidaRemapJar` | Ремаппинг с Cartografía |
| `vidaRun` | Запуск клиента с загрузчиком |

### Запуск клиента и политика модов

Запретить выбор модов резолвером при **локальном** запуске:

```kotlin
vida {
    run {
        accessDeniedIds.addAll("undesired_mod", "another")
    }
}
```

В JVM уходит `-Dvida.accessDenied=id1,id2` (несколько id — только через свойство; в строке `-javaagent=...=accessDenied=x` уместен **один** id из‑за разбора пар по запятой).

## Загрузчик и JVM

Общие свойства (агент и программный старт):

| Свойство | Смысл |
|----------|--------|
| `vida.modsDir` | Каталог `mods/` |
| `vida.cacheDir` | Кеш индексов и трансформов |
| `vida.strict` | Ошибки бутстрапа прерывают процесс |
| `vida.accessDenied` | Запрещённые id модов для резолвера |
| `vida.minecraftVersion` | Версия MC для синтетического провайдера |

Строка аргумента агента: `key=value` через запятую; список для `accessDenied` в агенте — один id или используйте `-Dvida.accessDenied=a,b`.

## BOM и шаблон проекта

- [Platform BOM (`dev.vida:vida-bom`)](../reference/platform-bom.md) — одна версия для всех `dev.vida:*`.
- Скелет из репозитория: [`templates/starter-mod`](../../templates/starter-mod/README.md).

## Карта документации

- [Три дорожки](modding-paths.md) — API / Vifada / Escultores  
- [Latidos](latidos.md) · [Catalogo](catalogo.md) · [Ajustes](ajustes.md)  
- [Vifada](vifada.md) · [Puertas](puertas.md) · [Escultores](escultores.md)  
- [Политики резолвера](resolver-policies.md)  
- [Модуль gradle-plugin](../modules/gradle-plugin.md) · [манифест](../reference/manifest-schema.md)

## IDE и отладка

Кратко: toolchain Java 21, `./gradlew build`, для запуска — задача `vidaRun` или `-javaagent` с fat `loader` JAR. Debugger: `-agentlib:jdwp=...`. Подробнее — [dev-environment](../getting-started/dev-environment.md#ide-подсказки).
