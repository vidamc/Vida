/*
 * :mundo — публичный world API Vida.
 *
 * Даёт типы координат, измерений, биомов, выборку блоков по модели `bloque` и world-латидосы.
 * Vanilla-runtime не входит в compile classpath; интеграция — через `:loader` / мост платформы.
 */
plugins {
    id("vida.library-conventions")
}

description =
        "Vida world API: Mundo, Coordenada, ChunkCoordenada, RegionCoordenada, LimitesVerticales, Dimension, Bioma, LatidosMundo."

dependencies {
    api(project(":core"))
    api(project(":base"))
    api(project(":bloque"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
