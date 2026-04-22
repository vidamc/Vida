/*
 * :mods:saciedad — Saciedad (Saturation-bar mod)
 *
 * Самостоятельный мод для Vida; выпускается независимо от основного монорепо.
 * Показывает скрытую шкалу насыщения поверх шкалы голода.
 *
 * mc-stubs/ — источник компиляционных заглушек для MC-классов.
 * Они НЕ включаются в итоговый JAR (отдельный sourceSet, не main).
 * На рантайме реальные классы предоставляет Minecraft.
 */
plugins {
    id("vida.library-conventions")
}

group = "dev.vida.mods"
description = "Saciedad — saturation-bar overlay mod for Vida/Minecraft 1.21.1."

// ---- MC stubs: compile-only, never shipped ----
val mcStubs by sourceSets.creating {
    java.srcDir("mc-stubs")
}

dependencies {
    api(project(":base"))
    api(project(":render"))
    compileOnly(project(":vifada"))
    // MC stubs — types are provided by the Minecraft runtime; only needed for javac.
    compileOnly(mcStubs.output)

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
