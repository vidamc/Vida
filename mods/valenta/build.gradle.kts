/*
 * :mods:valenta — Valenta (Sodium-class rendering optimization mod)
 *
 * Самостоятельный мод для Vida; выпускается независимо от основного монорепо.
 * VBO-батчер + glMultiDrawIndirect, compact vertex-format, chunk meshing
 * через Susurro task-graph, occlusion culling, QoL features.
 *
 * mc-stubs/ — compile-only заглушки MC-классов (не попадают в JAR).
 * src/jmh/ — JMH-бенчмарки (отдельный sourceSet).
 */
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    id("vida.library-conventions")
}

group = "dev.vida.mods"
description = "Valenta — Sodium-class rendering optimization mod for Vida/Minecraft 1.21.1."

// ---- MC stubs: compile-only, never shipped ----
val mcStubs by sourceSets.creating {
    java.srcDir("mc-stubs")
}

// ---- JMH benchmarks ----
val jmh by sourceSets.creating {
    java.srcDir("src/jmh/java")
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].output + sourceSets["main"].runtimeClasspath
}

dependencies {
    api(project(":base"))
    api(project(":render"))
    api(project(":susurro"))
    compileOnly(project(":vifada"))
    compileOnly(mcStubs.output)

    implementation(libs.slf4j.api)

    testImplementation(libs.jqwik)
    testRuntimeOnly(libs.logback.classic)

    "jmhImplementation"(libs.jmh.core)
    "jmhAnnotationProcessor"(libs.jmh.annprocess)
}

val jmhJar by tasks.registering(Jar::class) {
    archiveClassifier.set("jmh")
    from(jmh.output)
    from(sourceSets["main"].output)
    manifest {
        attributes("Main-Class" to "org.openjdk.jmh.Main")
    }
}

/*
 * Javadoc без внешнего `links` на Oracle: иначе doclet скачивает package-list
 * по сети — на Windows/медленном DNS задача «зависает» на десятки минут.
 * Детальная схема — в docs/mods/valenta/architecture.md, не в class Javadoc.
 */
tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).links = emptyList()
    maxMemory = "512m"
    isFailOnError = true
}
