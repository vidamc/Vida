/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.installer.McDirDetector;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Проверяет, что CLI-флаг {@code --validate-puertas} разбирается и что
 * {@link CliInstaller#run(CliArgs, String)} правильно обрабатывает три
 * сценария: валидный файл, невалидный файл, директория.
 */
final class CliPuertasTest {

    private static final McDirDetector DETECTOR =
            new McDirDetector("Linux", "/home/ana", null);

    private static final String VALIDO = """
            vida-puertas 1 namespace=intermedio
            accesible class net/ejemplo/Bloque
            mutable field net/ejemplo/Bloque edad I
            """;

    private static final String INVALIDO = """
            vida-puertas 1 namespace=intermedio
            accesible blockmethod net/ejemplo/Bloque nombre ()V
            """;

    @Test
    void flag_se_parsea_y_action_correcto(@TempDir Path dir) throws IOException {
        Path ptr = Files.writeString(dir.resolve("x.ptr"), VALIDO);
        var args = CliArgs.parse(
                new String[] { "--validate-puertas", ptr.toString() },
                DETECTOR, "x");
        assertThat(args.action()).isEqualTo(CliArgs.Action.VALIDATE_PUERTAS);
        assertThat(args.validatePuertasPath()).isEqualTo(ptr);
    }

    @Test
    void archivo_valido_retorna_cero(@TempDir Path dir) throws IOException {
        Path ptr = Files.writeString(dir.resolve("ok.ptr"), VALIDO);
        int code = runCli("--validate-puertas", ptr.toString());
        assertThat(code).isZero();
    }

    @Test
    void archivo_invalido_retorna_dos(@TempDir Path dir) throws IOException {
        Path ptr = Files.writeString(dir.resolve("bad.ptr"), INVALIDO);
        int code = runCli("--validate-puertas", ptr.toString());
        assertThat(code).isEqualTo(2);
    }

    @Test
    void directorio_recursivo_con_mixto(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("sub"));
        Files.writeString(dir.resolve("ok.ptr"), VALIDO);
        Files.writeString(dir.resolve("sub/bad.ptr"), INVALIDO);

        int code = runCli("--validate-puertas", dir.toString());
        assertThat(code).isEqualTo(2);
    }

    @Test
    void path_inexistente_retorna_uno() {
        int code = runCli("--validate-puertas", "/camino/que/no/existe-12345.ptr");
        assertThat(code).isEqualTo(1);
    }

    @Test
    void helpText_incluye_nueva_opcion() {
        assertThat(CliArgs.helpText()).contains("--validate-puertas");
    }

    // -----------------------------------------------------------------

    private static int runCli(String... argv) {
        var args = CliArgs.parse(argv, DETECTOR, "test");
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream capOut = new ByteArrayOutputStream();
        ByteArrayOutputStream capErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capOut, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(capErr, true, StandardCharsets.UTF_8));
        try {
            return CliInstaller.run(args, "test");
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }
}
