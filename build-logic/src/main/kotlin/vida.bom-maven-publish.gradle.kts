/*
 * Дополняет :bom (java-platform + maven-publish): signing, SCM/developers в POM.
 * Подключать через apply(plugin = "vida.bom-maven-publish") после блока publishing { }.
 */

import dev.vida.buildlogic.configureVidaCentralMetadata
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

plugins {
    signing
}

afterEvaluate {
    val publishing = extensions.getByType<PublishingExtension>()
    publishing.publications.named<MavenPublication>("maven") {
        pom {
            configureVidaCentralMetadata(
                project,
                "Vida BOM (dev.vida:vida-bom)",
                project.description ?: "Bill of Materials for Vida loader and mod API artifacts.",
            )
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
}
