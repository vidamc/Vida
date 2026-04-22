# Dev-окружение

Для разработки собственного мода под Vida или контрибьюции в загрузчик.

## Требования

- **JDK 21 LTS** (или новее до 26 GA). Рекомендуется Temurin / Liberica / Corretto.
- **Gradle 9.x** — используйте встроенный wrapper (`./gradlew`), локальный Gradle не нужен.
- **Git 2.40+**.
- IDE с поддержкой Java 21: IntelliJ IDEA 2024.2+, Eclipse 2024-12+, VS Code с расширением Red Hat Java.

## Проверка

```bash
java -version   # должно быть 21+
git --version
./gradlew --version
```

## Вариант A. Разработка самого Vida

```bash
git clone https://github.com/vidamc/Vida.git
cd Vida
./gradlew build         # полная сборка всех модулей
./gradlew check         # тесты + lint + spotless
./gradlew :loader:agentJar  # shaded JAR агента
```

Структура репозитория описана в [architecture/overview.md](../architecture/overview.md). Правила коммитов и стиля — в `[CONTRIBUTING.md](../../CONTRIBUTING.md)`.

## Вариант B. Разработка мода

Vida-плагин для Gradle публикуется в Gradle Plugin Portal. Минимальный `build.gradle.kts`:

```kotlin
plugins {
    id("dev.vida.mod") version "0.1.0"
}

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
            // Proguard-маппинги Mojang 1.21.1
            proguard.set(file("mappings/mojang_1_21_1.txt"))
        }
    }
    run {
        mainClass.set("net.minecraft.client.Main")
        args.addAll("--accessToken", "dev")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("dev.vida:vida-base:0.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
```

Получить маппинги Mojang:

```bash
mkdir -p mappings
curl -L https://piston-data.mojang.com/v1/objects/.../client.txt -o mappings/mojang_1_21_1.txt
```

(Точный URL проще всего взять из `version.json` соответствующей версии Minecraft.)

## Генерация шаблона

```bash
./gradlew vidaGenerateManifest
# создаст build/generated/vida/resources/vida.mod.json
```

Соберите JAR:

```bash
./gradlew build
# build/libs/miaventura-<ver>.jar
```

Запустите dev-клиент:

```bash
./gradlew vidaRun
```

Подробный список задач плагина — [modules/gradle-plugin.md](../modules/gradle-plugin.md).

## IDE-подсказки

### IntelliJ IDEA

1. `File → New → Project from Existing Sources` → выберите `build.gradle.kts`.
2. В `Project Structure → SDK` поставьте Java 21.
3. После первой синхронизации запустите `./gradlew vidaRemapJar` или `./gradlew genSources` — IDE увидит vanilla-классы в intermediary-маппингах.
4. Для запуска/отладки используйте run-конфигурацию `vidaRun` (Gradle) либо ручную Java Application с `-javaagent:.../vida-loader.jar`.

### VS Code

1. Установите расширение Red Hat Java.
2. `settings.json`:
  ```json
   {
     "java.configuration.runtimes": [
       { "name": "JavaSE-21", "path": "/path/to/jdk-21", "default": true }
     ],
     "java.import.gradle.wrapper.enabled": true
   }
  ```
3. Запустите `./gradlew build`; после этого VS Code проиндексирует классы.

## Отладка

### Подключение debugger'а

Добавьте в запуск:

```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

Порядок с `-javaagent:vida-loader.jar` неважен — они работают параллельно.

### Дамп патченных классов

```bash
java -Dvida.debug.dumpClasses=true -javaagent:vida-loader.jar ...
```

Файлы окажутся в `run/vida/dumps/<package>/<class>.class`. Откройте через `javap -p -v` или как `.class` в IntelliJ.

### Подробные логи

```bash
java -Dvida.log.level=DEBUG ...
```

Переопределяет уровень всех логгеров Vida. Логгеры модов — `vida.mod.<id>`; конфигурируются отдельно через `logback.xml` / SLF4J provider.

## Что дальше

- Напишите свой первый мод — [first-mod.md](./first-mod.md).
- Поняли архитектуру — [architecture/overview.md](../architecture/overview.md).
- Готовы контрибьютить — `[CONTRIBUTING.md](../../CONTRIBUTING.md)`.

