/*
 * :entidad — публичный API Vida для сущностей.
 *
 * Описывает entity-type декларативно: тип, hitbox, масса, AI-группы и
 * data-components. Реальный bridge к Minecraft runtime живёт в :mundo.
 */
plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Vida public entity API: Entidad, PropiedadesEntidad, entity data-components."

dependencies {
    api(project(":core"))
    api(project(":base"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
