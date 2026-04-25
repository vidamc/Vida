/*
 * :core — базовые типы Vida (Identifier, Version, Log, Result, …).
 *
 * Не должен иметь тяжёлых зависимостей — только SLF4J API и JDK.
 */

plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Vida core primitives: Identifier, Version, SemVer, Log, Result, Either."

dependencies {
    api(libs.slf4j.api)
    // Реальный binding у приложения; в тестах используем logback.
    testRuntimeOnly(libs.logback.classic)
}
