/*
 * :fuente — декларативные данные (datapack JSON в zip), парсинг Fuente / dataDriven.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida Fuente: data-driven block/item/recipe parsing from mod packs."

dependencies {
    api(project(":base"))
    api(project(":core"))
    api(project(":manifest"))
    api(project(":discovery"))
    api(project(":bloque"))
    api(project(":objeto"))

    testImplementation(project(":discovery"))
    testRuntimeOnly(libs.logback.classic)
}
