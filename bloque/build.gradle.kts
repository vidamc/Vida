/*
 * :bloque — публичный API Vida для блоков Minecraft.
 *
 * Модуль содержит декларативную модель блока: свойства, материал, форму
 * коллизии и обёртку BlockEntity. Само взаимодействие с vanilla-миром
 * пойдёт через :vida-render и :vida-mundo (когда они появятся); сейчас
 * модуль намеренно сделан side-agnostic — можно использовать и в unit-тестах,
 * и на сервере, и на клиенте.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida public block API: Bloque, PropiedadesBloque, FormaColision, BloqueEntidad."

dependencies {
    api(project(":core"))
    api(project(":base"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
