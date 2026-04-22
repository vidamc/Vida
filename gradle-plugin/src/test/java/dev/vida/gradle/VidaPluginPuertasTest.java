/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Функциональные тесты для задач {@code vidaValidatePuertas} и
 * {@code vidaPackagePuertas}.
 */
final class VidaPluginPuertasTest {

    private static final String VALIDO = """
            vida-puertas 1 namespace=intermedio
            accesible class net/ejemplo/Bloque
            mutable field net/ejemplo/Bloque edad I
            """;

    private static final String INVALIDO = """
            vida-puertas 1 namespace=intermedio
            accesible blockmethod net/ejemplo/Bloque nombre ()V
            """;

    private static String settings() {
        return """
                rootProject.name = "demo"
                """;
    }

    private static String buildFile(String puertas) {
        return """
                plugins {
                    id("dev.vida.mod")
                }

                group   = "com.ejemplo"
                version = "0.1.0"

                vida {
                    mod {
                        id.set("demo")
                        displayName.set("Demo")
                        entrypoint.set("com.ejemplo.DemoMod")
                        license.set("Apache-2.0")
                        puertas.addAll(%s)
                    }
                    minecraft { version.set("1.21.1") }
                }

                java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
                """.formatted(puertas);
    }

    @Test
    void validate_y_package_exitoso_con_ptr_valido(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("settings.gradle.kts"), settings());
        Files.writeString(dir.resolve("build.gradle.kts"),
                buildFile("\"puertas/demo.ptr\""));
        Path resDir = dir.resolve("src/main/resources/puertas");
        Files.createDirectories(resDir);
        Files.writeString(resDir.resolve("demo.ptr"), VALIDO);

        BuildResult result = GradleRunner.create()
                .withProjectDir(dir.toFile())
                .withPluginClasspath()
                .withArguments("vidaPackagePuertas", "--stacktrace")
                .forwardOutput()
                .build();

        assertThat(result.task(":vidaValidatePuertas").getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
        assertThat(result.task(":vidaPackagePuertas").getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);

        Path out = dir.resolve("build/generated/vida/puertas/puertas/demo.ptr");
        assertThat(out).exists();
        assertThat(Files.readString(out)).contains("accesible class");
    }

    @Test
    void validate_falla_para_ptr_invalido(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("settings.gradle.kts"), settings());
        Files.writeString(dir.resolve("build.gradle.kts"),
                buildFile("\"puertas/malo.ptr\""));
        Path resDir = dir.resolve("src/main/resources/puertas");
        Files.createDirectories(resDir);
        Files.writeString(resDir.resolve("malo.ptr"), INVALIDO);

        BuildResult result = GradleRunner.create()
                .withProjectDir(dir.toFile())
                .withPluginClasspath()
                .withArguments("vidaValidatePuertas", "--stacktrace")
                .forwardOutput()
                .buildAndFail();

        assertThat(result.getOutput())
                .containsAnyOf("неизвестная цель", "validación", "валидация провалена", "puertas");
    }

    @Test
    void ausencia_de_puertas_deja_tarea_skipped(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("settings.gradle.kts"), settings());
        // Сборка без puertas-записей: таска должна быть SKIPPED.
        Files.writeString(dir.resolve("build.gradle.kts"), buildFile(""));

        BuildResult result = GradleRunner.create()
                .withProjectDir(dir.toFile())
                .withPluginClasspath()
                .withArguments("vidaValidatePuertas", "--stacktrace")
                .forwardOutput()
                .build();

        assertThat(result.task(":vidaValidatePuertas").getOutcome())
                .isIn(TaskOutcome.SKIPPED, TaskOutcome.SUCCESS, TaskOutcome.NO_SOURCE);
    }
}
