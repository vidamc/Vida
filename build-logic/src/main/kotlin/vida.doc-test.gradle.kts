/*
 * Генерирует источники из fenced {@code ```java} блоков с {@code package dev.vida.*}
 * (полные compilation units) для компиляции в :vida-doc-test.
 */

import dev.vida.buildlogic.VidaDocTestGenerateTask
import org.gradle.language.base.plugins.LifecycleBasePlugin

plugins {
    java
}

val generateVidaDocTestSources = tasks.register<VidaDocTestGenerateTask>("generateVidaDocTestSources") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Extracts ```java snippets from docs/ into generated test sources."
    docsDir.set(rootProject.layout.projectDirectory.dir("docs"))
    outputDir.set(layout.buildDirectory.dir("generated/vida-doc-test/src/test/java"))
}

sourceSets.named("test") {
    java.srcDir(layout.buildDirectory.dir("generated/vida-doc-test/src/test/java"))
}

tasks.named<JavaCompile>("compileTestJava") {
    dependsOn(generateVidaDocTestSources)
}

tasks.named<Test>("test") {
    dependsOn(generateVidaDocTestSources)
    // Gradle 9: при пустом наборе сгенерированных JUnit-тестов задача не должна падать —
    // проверка — это compileTestJava по сниппетам из docs/.
    failOnNoDiscoveredTests.set(false)
}
