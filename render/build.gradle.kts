/*
 * :render — публичный render API Vida.
 *
 * Pipeline-абстракция поверх моделей блоков/сущностей, texture-atlas и shader hooks.
 * Модуль не содержит vanilla-клиентских зависимостей и пригоден для unit-тестов.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida render API: ModeloBloque, ModeloEntidad, TextureAtlas, RenderPipeline."

dependencies {
    api(project(":core"))
    api(project(":base"))
    api(project(":bloque"))
    api(project(":entidad"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
