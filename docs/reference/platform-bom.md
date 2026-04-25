# Platform BOM (`dev.vida:vida-bom`)

Bill of Materials фиксирует **одну версию** для всех опубликованных библиотек `dev.vida:*` из данного релиза Vida. Подключайте платформу один раз — версии транзитивных зависимостей от модулей Vida совпадают друг с другом.

- Артефакт: **`dev.vida:vida-bom`** (модуль Gradle `:bom`)
- Версия BOM = **версия релиза Vida** (корневой `version.txt` / тег).

## Gradle (Kotlin DSL)

```kotlin
dependencies {
    val vidaRelease = "1.0.0" // та же версия, что у артефактов Vida в Maven
    compileOnly(platform("dev.vida:vida-bom:$vidaRelease"))
    compileOnly("dev.vida:base")
    compileOnly("dev.vida:core") // при необходимости
}
```

Замените `compileOnly` на `api` / `implementation` по смыслу вашего мода.

## Maven

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>dev.vida</groupId>
      <artifactId>vida-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>dev.vida</groupId>
    <artifactId>base</artifactId>
  </dependency>
</dependencies>
```

## Состав BOM

В платформу входят ограничения версий для модулей: `core`, `manifest`, `fuente`, `config`, `cartografia`, `discovery`, `resolver`, `vifada`, `loader`, `base`, `bloque`, `objeto`, `susurro`, `puertas`, `escultores`, `entidad`, `mundo`, `render`, `red`, `vigia`. Список живёт в репозитории в `bom/build.gradle.kts`.

## Локальная проверка

После `./gradlew :bom:publishToMavenLocal` BOM доступен из `mavenLocal()` для разработки шаблонов и интеграционных тестов.

## См. также

- [Шаблон проекта](../../templates/starter-mod/README.md) (в репозитории Vida)
- [Модуль bom](../modules/bom.md)
