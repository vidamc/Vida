/*
 * :config — типизированный конфиг-движок Vida (Ajustes) поверх TOML.
 */

plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Ajustes — typed configuration engine with profiles and deep merging."

dependencies {
    api(project(":core"))
    implementation(libs.tomlj)
    testRuntimeOnly(libs.logback.classic)
}
