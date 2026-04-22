/*
 * :mods:senda — Senda (navigation convenience mod)
 *
 * Демонстрирует Catalogo + Ajustes + Latidos в минималистичном моде-путеводителе.
 * Самостоятельный мод; выпускается независимо от основного монорепо.
 */
plugins {
    id("vida.library-conventions")
}

group = "dev.vida.mods"
description = "Senda — navigation waypoint mod for Vida/Minecraft 1.21.1."

dependencies {
    api(project(":base"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
