/*
 * :mundo — публичный world API Vida.
 *
 * Даёт типы координат, измерений, биомов и world-латидосы. Модуль не тянет
 * vanilla runtime в compile API и остаётся пригодным для unit-тестов модов.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida world API: Mundo, Coordenada, Dimension, Bioma, LatidosMundo."

dependencies {
    api(project(":core"))
    api(project(":base"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
