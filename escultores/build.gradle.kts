/*
 * :escultores — низкоуровневые byte[]→byte[] трансформеры (public API + встроенные).
 */
plugins {
    id("vida.library-conventions")
}

description = "Escultores: low-level class transformers (byte[] → byte[])."

dependencies {
    api(project(":core"))
    implementation(libs.asm)
    implementation(libs.asm.tree)

    testRuntimeOnly(libs.logback.classic)
}
