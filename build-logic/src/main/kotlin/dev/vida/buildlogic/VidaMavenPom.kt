package dev.vida.buildlogic

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom

/** Shared Maven Central POM metadata for dev.vida artifacts. */
fun MavenPom.configureVidaCentralMetadata(
    project: Project,
    artifactTitle: String,
    artifactDescription: String,
) {
    name.set(artifactTitle)
    description.set(artifactDescription)
    url.set("https://github.com/vidamc/Vida")
    licenses {
        license {
            name.set("Apache License 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
    }
    scm {
        connection.set("scm:git:https://github.com/vidamc/Vida.git")
        developerConnection.set("scm:git:ssh://git@github.com/vidamc/Vida.git")
        url.set("https://github.com/vidamc/Vida")
    }
    developers {
        developer {
            id.set("vida")
            name.set("The Vida Project Authors")
            url.set("https://github.com/vidamc/Vida")
        }
    }
}
