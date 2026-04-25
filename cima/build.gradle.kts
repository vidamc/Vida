/*
 * :cima — "верх" доступа: обёртка над ваниллой для кастомных модов, которым
 * мало портABLE `vida-mundo`. Реализация поставляется :loader, интерфейсы
 * публикуются тут, без `net.minecraft.*` в public API.
 */
plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description =
        "Vida cima: optional ultimate runtime surface (Minecraft Level handle + Mundo) for custom mods."

dependencies {
    api(project(":core"))
    api(project(":base"))
    api(project(":mundo"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
