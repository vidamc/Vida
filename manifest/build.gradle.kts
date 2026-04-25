/*
 * :manifest — разбор vida.mod.json + встроенный Vida-JSON парсер.
 */

plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Manifest schema (vida.mod.json) + Vida-JSON streaming parser."

dependencies {
    api(project(":core"))
    testRuntimeOnly(libs.logback.classic)
}
