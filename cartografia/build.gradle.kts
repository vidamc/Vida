/*
 * :cartografia — система мэппингов имён Vida.
 *
 * Содержит:
 *   * Namespace / MappingTree / Class/Field/MethodMapping — иммутабельная модель.
 *   * io/ProguardReader — парсер Mojang-овских proguard-мэппингов.
 *   * io/CtgReader + io/CtgWriter — собственный компактный бинарный формат .ctg.
 *   * asm/CartografiaRemapper — адаптер org.objectweb.asm.commons.Remapper.
 */

plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Cartografía — name mapping model, Proguard reader, .ctg format, ASM Remapper adapter."

dependencies {
    api(project(":core"))
    implementation(libs.asm)
    implementation(libs.asm.commons)
    testRuntimeOnly(libs.logback.classic)
}
