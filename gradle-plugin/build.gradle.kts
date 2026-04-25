/*
 * :gradle-plugin — Gradle-плагин для авторов модов.
 *
 * id: dev.vida.mod
 *
 * Публикуется в Gradle Plugin Portal (`publishPlugins`) и как Maven-публикации
 * (pluginMaven + marker) для Sonatype / локальной проверки POM.
 */
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

plugins {
    id("vida.java-conventions")
    id("vida.test-conventions")
    `java-gradle-plugin`
    signing
    alias(libs.plugins.gradle.plugin.publish)
}

description = "Vida Gradle plugin for mod authors: manifest generation, remapping, launch."

dependencies {
    implementation(project(":core"))
    implementation(project(":manifest"))
    implementation(project(":fuente"))
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
    // Обязательно для com.gradle.plugin-publish (иначе «Website URL not set»).
    website.set("https://github.com/vidamc/Vida")
    vcsUrl.set("https://github.com/vidamc/Vida.git")
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

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Vida Gradle Plugin (dev.vida.mod)")
            description.set(project.description)
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
    }
}

extensions.configure<SigningExtension>("signing") {
    val signingKey = providers.environmentVariable("SIGNING_PRIVATE_KEY")
    val signingPassword = providers.environmentVariable("SIGNING_PASSWORD")
    setRequired(signingKey.map { it.isNotBlank() }.orElse(false))
    if (signingKey.isPresent && signingKey.get().isNotBlank()) {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
        val pubs = project.extensions.getByType<org.gradle.api.publish.PublishingExtension>().publications
        sign(pubs)
    }
}

// com.gradle.plugin-publish может создавать Sign-задачи — без ключа не выполнять.
tasks.withType<Sign>().configureEach {
    onlyIf("SIGNING_PRIVATE_KEY present") {
        providers.environmentVariable("SIGNING_PRIVATE_KEY").orNull?.isNotBlank() == true
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
