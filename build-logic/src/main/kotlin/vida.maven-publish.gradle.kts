/*
 * Публикация Java-библиотек в Maven Central (через io.github.gradle-nexus.publish-plugin на корне).
 * artifactId = имя Gradle-проекта (core, base, manifest, …) — см. docs/reference/platform-bom.md.
 */

import dev.vida.buildlogic.configureVidaCentralMetadata
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.SigningExtension

plugins {
    signing
    `maven-publish`
}

base {
    archivesName.set(project.name)
}

val javaComponent = components["java"]

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(javaComponent)
            suppressPomMetadataWarningsFor("runtimeElements")
            pom {
                configureVidaCentralMetadata(
                    project,
                    "Vida ${project.name}",
                    project.description ?: "Vida module ${project.name}",
                )
            }
        }
    }
}

extensions.configure<SigningExtension>("signing") {
    val signingKey = providers.environmentVariable("SIGNING_PRIVATE_KEY")
    val signingPassword = providers.environmentVariable("SIGNING_PASSWORD")
    if (signingKey.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        sign(publishing.publications["maven"])
    }
}

// Javadoc warnings must not fail publish (library modules use -Werror on compile; javadoc is separate).
tasks.withType<Javadoc>().configureEach { isFailOnError = false }
