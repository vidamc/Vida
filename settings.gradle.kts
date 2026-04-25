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
        google()
        mavenCentral()
    }
}

// ----- Convention plugins (included build) ----------------------------------
includeBuild("build-logic")

// ----- BOM (aligned versions for consumers) ---------------------------------
include(":bom")

// ----- Core modules ---------------------------------------------------------
include(":core")
include(":manifest")
include(":fuente")
include(":config")
include(":cartografia")
include(":discovery")
include(":resolver")
include(":vifada")
include(":loader")
include(":base")
include(":gradle-plugin")
include(":installer")
include(":vida-doc-test")

// ----- API / runtime modules (0.3.x) ----------------------------------------
include(":bloque")
include(":objeto")
include(":susurro")
include(":puertas")
include(":escultores")

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
// Опциональные подпроекты: не ломаем конфигурацию, если каталог отсутствует (sparse checkout).
val modsSaciedad = file("mods/saciedad")
if (modsSaciedad.isDirectory) {
    include(":mods:saciedad")
    project(":mods:saciedad").projectDir = modsSaciedad
}

val modsSenda = file("mods/senda")
if (modsSenda.isDirectory) {
    include(":mods:senda")
    project(":mods:senda").projectDir = modsSenda
}

// ----- Skal Launcher (standalone Compose UI) ---------------------------------
val skalLauncherDir = file("Skal Launcher")
if (skalLauncherDir.isDirectory) {
    include(":skal-launcher")
    project(":skal-launcher").projectDir = skalLauncherDir
}
