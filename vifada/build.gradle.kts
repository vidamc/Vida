/*
 * :vifada — байткод-трансформер Vida.
 *
 * Содержит публичные аннотации (@VifadaMorph/@VifadaInject/@VifadaOverwrite/
 * @VifadaShadow/@VifadaAt) и внутреннюю ASM-реализацию парсера и
 * single-pass аппликатора морфов.
 */

plugins {
    id("vida.library-conventions")
}

description = "Vifada: bytecode transformer with @VifadaMorph / @VifadaInject / @VifadaOverwrite."

dependencies {
    api(project(":core"))
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.asm.commons)
    implementation(libs.asm.analysis)

    testRuntimeOnly(libs.logback.classic)
}
