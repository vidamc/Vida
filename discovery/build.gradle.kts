/*
 * :discovery — сканер директории `mods/`, поддержка вложенных JAR (jar-in-jar)
 * и бинарный кэш `mods.idx` для быстрого повторного старта.
 */

plugins {
    id("vida.library-conventions")
}

description = "Mod discovery: ZIP readers, ModScanner, nested JARs, mods.idx cache."

dependencies {
    api(project(":core"))
    api(project(":manifest"))
    testRuntimeOnly(libs.logback.classic)
}
