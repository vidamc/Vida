/*
 * Vida — root settings.
 *
 * Монорепо использует composite build: convention-плагины живут в отдельном
 * included build `build-logic`, что позволяет переиспользовать Java/тест/лимит
 * конвенции из любого модуля без копипаста.
 */

rootProject.name = "vida"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Единый источник версий — gradle/libs.versions.toml (подхватывается автоматически).
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

// ----- Convention plugins (included build) ----------------------------------
includeBuild("build-logic")

// ----- Core modules ---------------------------------------------------------
include(":core")
include(":manifest")
include(":config")
include(":cartografia")
include(":discovery")
include(":resolver")
include(":vifada")
include(":loader")
include(":base")
include(":gradle-plugin")
include(":installer")

// ----- API / runtime modules (0.3.x) ----------------------------------------
include(":bloque")
include(":objeto")
include(":susurro")
include(":puertas")

// ----- Observability (0.4.x) -------------------------------------------
include(":vigia")

// По мере реализации будут добавляться:
include(":entidad")
include(":mundo")
include(":render")
include(":red")
//   include(":escultores")
//   include(":bench")

// ----- Mods (0.7.x) ---------------------------------------------------------
include(":mods:saciedad")
project(":mods:saciedad").projectDir = file("mods/saciedad")

include(":mods:senda")
project(":mods:senda").projectDir = file("mods/senda")

include(":mods:valenta")
project(":mods:valenta").projectDir = file("mods/valenta")
