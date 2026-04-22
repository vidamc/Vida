/*
 * :gradle-plugin — Gradle-плагин для авторов модов.
 *
 * id: dev.vida.mod
 *
 * Добавляет DSL `vida { ... }`, генерирует `vida.mod.json` из него,
 * валидирует манифест через :manifest, умеет ремаппить jar через
 * :cartografia и запускать игру с модом.
 *
 * Публикуется как самостоятельный плагин: consumers могут писать
 * `plugins { id("dev.vida.mod") version "..." }`.
 */
plugins {
    id("vida.java-conventions")
    id("vida.test-conventions")
    `java-gradle-plugin`
}

description = "Vida Gradle plugin for mod authors: manifest generation, remapping, launch."

dependencies {
    implementation(project(":core"))
    implementation(project(":manifest"))
    implementation(project(":cartografia"))
    implementation(project(":puertas"))
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)

    testImplementation(gradleTestKit())
    testImplementation(project(":core"))
    testImplementation(project(":manifest"))
    testImplementation(project(":cartografia"))
    testImplementation(project(":puertas"))
}

gradlePlugin {
    plugins {
        create("vidaMod") {
            id = "dev.vida.mod"
            implementationClass = "dev.vida.gradle.VidaPlugin"
            displayName = "Vida Mod Plugin"
            description = "Build, remap and run Vida mods for Minecraft."
            tags.set(listOf("vida", "minecraft", "modding"))
        }
    }
}

// Gradle Task-конструкторы традиционно вызывают setGroup/setDescription/...
// это формально trips javac's this-escape-warning, но для Gradle-тасков это
// канонический паттерн. Исключаем именно этот lint.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-this-escape")
}

// Гарантируем, что функциональные тесты видят актуальный плагин.
tasks.named<Test>("test") {
    dependsOn(tasks.named("pluginUnderTestMetadata"))
}
