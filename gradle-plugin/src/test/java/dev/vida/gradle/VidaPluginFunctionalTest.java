/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Запускает реальный Gradle-процесс через TestKit: проверяем, что
 * {@code vidaGenerateManifest} и {@code vidaValidateManifest} отрабатывают
 * end-to-end в изолированном проекте.
 */
final class VidaPluginFunctionalTest {

    @Test
    void generate_and_validate_manifest_end_to_end(@TempDir Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                rootProject.name = "demo"
                """);

        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("dev.vida.mod")
                }

                group   = "com.ejemplo"
                version = "0.4.2"

                vida {
                    mod {
                        id.set("miaventura")
                        displayName.set("Mi Aventura")
                        description.set("Una prueba de Vida")
                        authors.addAll("Ana", "Bob")
                        entrypoint.set("com.ejemplo.MiAventura")
                        license.set("Apache-2.0")
                        dependency("vida", "^0.1")
                    }
                    minecraft {
                        version.set("1.21.1")
                    }
                }

                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(21))
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("vidaValidateManifest", "--stacktrace", "--info")
                .forwardOutput()
                .build();

        assertThat(result.task(":vidaGenerateManifest").getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.task(":vidaValidateManifest").getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);

        Path manifest = projectDir.resolve("build/generated/vida/resources/vida.mod.json");
        assertThat(manifest).exists();
        String json = Files.readString(manifest);
        assertThat(json)
                .contains("\"miaventura\"")
                .contains("\"Mi Aventura\"")
                .contains("\"com.ejemplo.MiAventura\"");
    }

    @Test
    void validate_fails_on_bad_manifest_dsl(@TempDir Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), """
                rootProject.name = "demo"
                """);

        // id содержит запрещённые символы → ManifestParser должен вернуть ошибку.
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("dev.vida.mod")
                }

                version = "1.0.0"

                vida {
                    mod {
                        id.set("BAD ID!")
                    }
                }

                java {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(21))
                    }
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("vidaValidateManifest", "--stacktrace")
                .forwardOutput()
                .buildAndFail();

        assertThat(result.getOutput())
                .containsAnyOf("vida.mod.json", "invalid", "ManifestError", "BAD ID");
    }
}
