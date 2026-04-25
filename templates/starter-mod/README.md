# Starter mod (Plantilla)

Готовый скелет мода под Vida: JDK 21, плагин `dev.vida.mod`, BOM `dev.vida:vida-bom`, один класс `VidaMod`.

## Быстрый старт (сейчас — Gradle Plugin Portal)

Пока публикация **`dev.vida:*` в Maven Central** не настроена, платформенные JAR и BOM берите из **`mavenLocal()`** (сборка монорепо Vida) или из composite build (см. ниже). Плагин мода **`dev.vida.mod`** ориентируем на **[Gradle Plugin Portal](https://plugins.gradle.org/)** после первого `publishPlugins` из CI.

1. Скопируйте каталог `starter-mod` в свой репозиторий (или используйте его как подпроект).
2. Установите в `gradle.properties`:
   - `vida.plugin.version` — версия плагина на [Gradle Plugin Portal](https://plugins.gradle.org/).
   - `vida.platform.version` — та же линия релиза, что у артефактов `dev.vida:*` (**пока** из `mavenLocal` после `./gradlew :bom:publishToMavenLocal publishToMavenLocal` в клоне Vida, либо из вашего Nexus).
3. Пока без Central — опубликуйте BOM и библиотеки в локальный Maven из клона Vida:
   ```bash
   cd /path/to/Vida
   ./gradlew publishToMavenLocal
   ```
4. В проекте шаблона:
   ```bash
   ./gradlew vidaGenerateManifest build
   ```

## Разработка внутри монорепозитория Vida

Чтобы не зависеть от Portal, подключите корень Vida как composite build и используйте плагин из исходников.

Пример `settings.gradle.kts` (пути поправьте под расположение шаблона):

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // Корень репозитория Vida (родитель templates/)
    includeBuild("../..") {
        dependencySubstitution {
            substitute(module("dev.vida:vida-bom")).using(project(":bom"))
        }
    }
}

plugins {
    id("dev.vida.mod") version "unspecified"
}
```

Дальше в `build.gradle.kts` можно ссылаться на проекты через `compileOnly(project(":base"))` вместо координат Maven — это уже настройка монорепо; для автономного мода предпочтительны координаты + BOM.

## Переименование

| Что | Куда |
|-----|------|
| `plantilla_demo` | `vida { mod { id.set("tu_mod") } }` и совпадающие пакеты при желании |
| Пакет `dev.vida.template.starter` | ваш namespace |
| `rootProject.name` | имя Gradle-проекта |

## Документация

- [Platform BOM](../../docs/reference/platform-bom.md)
- [Полный набор моддера](../../docs/guides/modder-toolkit.md)
