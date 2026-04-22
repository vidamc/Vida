/*
 * Базовые Java-конвенции Vida.
 *
 * Применяются ко всем JVM-модулям: выставляют Java 21 toolchain, UTF-8,
 * строгие компиляторные опции и унифицированный output layout.
 */

plugins {
    `java-library`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion))
    }
    withSourcesJar()
    // javadoc-jar добавляется отдельной конвенцией `vida.library-conventions`
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(libs.findVersion("java").get().requiredVersion.toInt())
    options.compilerArgs.addAll(listOf(
        "-Xlint:all",
        "-Xlint:-processing",
        "-Xlint:-serial",
        "-Werror",
        "-parameters",
    ))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        // Несовместимость с -Werror javadoc в CI решаем через -Xdoclint
        links("https://docs.oracle.com/en/java/javase/21/docs/api/")
    }
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title"   to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor"  to "The Vida Project Authors",
        )
    }
}
