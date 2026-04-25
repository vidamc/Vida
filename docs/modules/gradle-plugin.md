# gradle-plugin

Gradle-плагин `dev.vida.mod`. Инструмент сборки мода Vida: генерация `vida.mod.json`, ремаппинг JAR, запуск dev-клиента.

- Пакет: `dev.vida.gradle`
- Plugin ID: `dev.vida.mod`
- Стабильность: **`@ApiStatus.Stable`** (публичные пакеты `dev.vida.gradle`; см. [api-stability.md](../reference/api-stability.md))

## Установка

```kotlin
plugins {
    id("dev.vida.mod") version "0.1.0"
}
```

Плагин автоматически применяет `java-library` и настраивает Java 21.

## DSL

```kotlin
vida {
    mod {
        id.set("miaventura")
        displayName.set("Mi Aventura")
        description.set("Пример мода на Vida")
        authors.add("Ana")
        license.set("MIT")
        entrypoint.set("com.ejemplo.MiAventura")
    }

    minecraft {
        version.set("1.21.1")
        mappings {
            proguard.set(file("mappings/mojang_1_21_1.txt"))
        }
    }

    // Puertas (access wideners) — опционально. 0.3.0+
    puertas {
        files.from(file("src/main/resources/miaventura.ptr"))
        namespace.set("intermedio") // crudo | intermedio | exterior
        strict.set(true)            // упасть на первой ошибке
    }

    run {
        mainClass.set("net.minecraft.client.Main")
        args.addAll("--accessToken", "dev")
        jvmArgs.addAll("-Xmx4G")
    }
}
```

Все поля — `Property<T>` / `ListProperty<T>` (Gradle lazy API). Это даёт точное кеширование и корректную работу с configuration cache.

## Задачи

### `vidaGenerateManifest`

Рендерит `vida.mod.json` из DSL в `build/generated/vida/resources/vida.mod.json`. Подкладывает эту папку в `sourceSets.main.resources`, так что файл попадает в JAR автоматически.

```bash
./gradlew vidaGenerateManifest
```

### `vidaValidateManifest`

Прогоняет сгенерированный (или написанный руками) `vida.mod.json` через `ManifestParser`. При ошибке — `BUILD FAILED` с человекочитаемой диагностикой.

Если в манифесте включён **`custom["vida:dataDriven"].enabled = true`**, задача дополнительно проверяет, что каталог **`datapackRoot`** (или дефолт `data/<mod-id>/vida`) **существует** под корнем ресурсов модуля (обычно `src/main/resources`), через [`FuenteManifestoDatapackValidador`](./fuente.md). Иначе сборка падает с сообщением `vida:dataDriven validation failed: …`.

Зависимость: `jar` задача зависит от `vidaValidateManifest`, так что валидный манифест — прекондиция любой сборки.

### `vidaRemapJar`

Применяет `CartografiaRemapper` к JAR мода. Обычно — ремап intermediary → mojang (для распространения) или обратно (для dev).

```bash
./gradlew vidaRemapJar
# build/libs/miaventura-<ver>-remapped.jar
```

### `vidaValidatePuertas` *(0.3.0+)*

Парсит все `.ptr`-файлы, указанные в `vida.puertas.files`, и падает с подробной диагностикой, если хоть одна директива некорректна: неизвестный namespace, нераспознанный дескриптор, дубликаты, конфликтующие действия.

```bash
./gradlew vidaValidatePuertas
```

Задача — `@CacheableTask`, up-to-date, пока не изменился ни один входной файл. Подцепляется к `check`.

### `vidaPackagePuertas` *(0.3.0+)*

Копирует и нормализует `.ptr`-файлы в `build/generated/vida/resources/META-INF/puertas/`. Этот каталог автоматически добавляется в `sourceSets.main.resources`, поэтому файлы попадают в JAR и становятся видны лоадеру при запуске. Задача зависит от `vidaValidatePuertas` — в JAR никогда не попадёт невалидный `.ptr`.

Формат `.ptr` и все директивы — см. [modules/puertas.md](./puertas.md) и практический [guides/puertas.md](../guides/puertas.md).

### `vidaRun`

Запускает Vida с указанной игрой через `JavaExec`. Параметры берутся из `vida.run { ... }`:

```bash
./gradlew vidaRun
```

Под капотом:

1. Строит classpath: Minecraft + все зависимости + агент.
2. Формирует JVM-аргументы (`-javaagent:.../vida-loader.jar`).
3. Запускает как форкнутый процесс.

Для подключения отладчика добавьте в `run`:

```kotlin
jvmArgs.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
```

#### Hot reload (dev, 2.0)

В блоке `vida.run { }` включите **`hotReload.set(true)`**. Задача передаст JVM `-Dvida.dev.hotReload=true` и `-Dvida.dev.hotReload.watch=…` (каталог скомпилированных классов мода); загрузчик может сбросить каталоги через `CatalogoManejador` при изменении файлов. Только для разработки — см. [guides/hot-reload.md](../guides/hot-reload.md).

## Интеграция с сборкой

Плагин автоматически:

- применяет `java-library`;
- ставит `JavaLanguageVersion.of(21)` по умолчанию (можно переопределить);
- добавляет сгенерированный `vida.mod.json` в `resources`;
- делает `jar` зависимой от `vidaGenerateManifest`, `vidaValidateManifest` и (при непустом `vida.puertas.files`) `vidaPackagePuertas`;
- подцепляет `vidaValidateManifest` и `vidaValidatePuertas` к `check`;
- регистрирует группу задач `vida` для удобной навигации в IDE.

## Composite builds

Плагин распространяется как отдельный артефакт в Gradle Plugin Portal, но в репозитории Vida вы подключаете его через `includeBuild`:

```kotlin
// settings.gradle.kts вашего тестового проекта
includeBuild("../Vida") {
    dependencySubstitution {
        substitute(module("dev.vida:vida-gradle-plugin"))
            .using(project(":gradle-plugin"))
    }
}
```

Это позволяет разрабатывать плагин и мод одновременно, без релиза.

## Совместимость

- Gradle **9.x** (основная поддержка). 8.x и ранее — не поддерживаются из-за изменений в Property API.
- Kotlin DSL — основной. Groovy DSL работает, но примеры в документации — только Kotlin.
- Configuration cache — поддерживается.

## Валидация

Плагин самотестируется через Gradle Testkit: `gradle-plugin/src/functionalTest`. Тесты проверяют:

- идентичность сгенерированного `vida.mod.json` эталону;
- корректность валидации (невалидные манифесты → `BUILD FAILED`);
- настройку classpath для `vidaRun`;
- incremental behavior: изменение одного поля DSL не инвалидирует лишние задачи.

## Планы

- `vidaTestRun` — лёгкий test-runner для мода без полного запуска Minecraft.
- Корневая задача **`./gradlew vidaDocTest`** — компиляция fenced-примеров из `docs/` ([session-roadmap.md](../session-roadmap.md)).
- Publishing DSL — удобная публикация в Modrinth / CurseForge / собственный maven.

## Что читать дальше

- [getting-started/dev-environment.md](../getting-started/dev-environment.md) — полный setup.
- [getting-started/first-mod.md](../getting-started/first-mod.md) — первый мод за 10 минут.
- [modules/cartografia.md](./cartografia.md) — что именно делает ремаппер.
- [modules/puertas.md](./puertas.md) · [guides/puertas.md](../guides/puertas.md) — формат `.ptr` и практические рецепты.
- [reference/manifest-schema.md](../reference/manifest-schema.md) — формат `vida.mod.json`, которым управляет DSL.
