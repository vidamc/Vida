/*
 * :puertas — access-wideners Vida.
 *
 * Модуль парсит .ptr-файлы (простой текстовый формат) и применяет директивы
 * изменения доступности членов целевого класса на лету через ASM.
 *
 * Зависимости: core (ApiStatus/Result) + ASM (tree-API).  На manifest
 * намеренно НЕ зависим — manifest уже знает про puertas-поле и передаёт
 * пути до файлов в loader/gradle-plugin.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida access-wideners: .ptr parser and ASM applier."

dependencies {
    api(project(":core"))
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.slf4j.api)

    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testRuntimeOnly(libs.logback.classic)
}
