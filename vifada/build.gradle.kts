/*
 * :vifada — байткод-трансформер Vida.
 *
 * Содержит публичные аннотации (@VifadaMorph/@VifadaInject/@VifadaOverwrite/
 * @VifadaShadow/@VifadaAt) и внутреннюю ASM-реализацию парсера и
 * single-pass аппликатора морфов.
 */

plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Vifada: bytecode transformer with @VifadaMorph / @VifadaInject / @VifadaOverwrite."

val jmh by sourceSets.creating {
    java.srcDir("src/jmh/java")
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].output + sourceSets["main"].runtimeClasspath
}

dependencies {
    api(project(":core"))
    implementation(libs.asm)
    implementation(libs.asm.tree)
    implementation(libs.asm.commons)
    implementation(libs.asm.analysis)

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
