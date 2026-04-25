# Starter mod (Plantilla)

Готовый скелет мода под Vida: JDK 21, плагин `dev.vida.mod`, BOM `dev.vida:vida-bom`, один класс `VidaMod`.

## Быстрый старт (опубликованные артефакты)

1. Скопируйте каталог `starter-mod` в свой репозиторий (или используйте его как подпроект).
2. Установите в `gradle.properties` одну и ту же линию релиза Vida:
   - `vida.platform.version` — версия BOM и jar’ов `dev.vida:*` на Maven Central / вашем Nexus.
   - `vida.plugin.version` — версия плагина на [Gradle Plugin Portal](https://plugins.gradle.org/).
3. Опубликуйте BOM локально для проверки без центрального репозитория:
   ```bash
   cd /path/to/Vida
   ./gradlew :bom:publishToMavenLocal
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
