/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class VigiaSesionTest {

    @TempDir Path tmp;

    @Test
    void start_stop_produces_jfr_and_resumen() throws IOException {
        VigiaSesion sesion = VigiaSesion.iniciar();
        assertThat(sesion.activa()).isTrue();

        busyWork();

        Path jfr = tmp.resolve("test.jfr");
        Resumen resumen = sesion.detener(jfr);

        assertThat(sesion.activa()).isFalse();
        assertThat(Files.exists(jfr)).isTrue();
        assertThat(Files.size(jfr)).isGreaterThan(0);
        assertThat(resumen.duracion()).isNotNull();
        assertThat(resumen.muestras()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void instantanea_does_not_stop_session() {
        VigiaSesion sesion = VigiaSesion.iniciar();
        try {
            busyWork();

            Resumen snap = sesion.instantanea();
            assertThat(sesion.activa()).isTrue();
            assertThat(snap).isNotNull();
            assertThat(snap.duracion()).isNotNull();
        } finally {
            sesion.cancelar();
        }
    }

    @Test
    void double_stop_throws() throws IOException {
        VigiaSesion sesion = VigiaSesion.iniciar();
        sesion.detener(tmp.resolve("one.jfr"));

        assertThatThrownBy(() -> sesion.detener(tmp.resolve("two.jfr")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("detenida");
    }

    @Test
    void cancelar_stops_gracefully() {
        VigiaSesion sesion = VigiaSesion.iniciar();
        assertThat(sesion.activa()).isTrue();
        sesion.cancelar();
        assertThat(sesion.activa()).isFalse();
    }

    @Test
    void resumen_top_metodos_format() throws IOException {
        VigiaSesion sesion = VigiaSesion.iniciar();
        busyWork();

        Resumen resumen = sesion.detener(tmp.resolve("format.jfr"));

        for (var m : resumen.topMetodos()) {
            assertThat(m.metodo()).contains("#");
            assertThat(m.muestras()).isPositive();
            assertThat(m.porcentaje()).isBetween(0.0, 100.0);
        }
    }

    @Test
    void con_susurro_captures_estadisticas() throws IOException {
        try (var s = dev.vida.susurro.Susurro.iniciar()) {
            VigiaSesion sesion = VigiaSesion.iniciar();
            sesion.conSusurro(s);

            Resumen resumen = sesion.detener(tmp.resolve("sus.jfr"));
            assertThat(resumen.susurroActivos()).isGreaterThanOrEqualTo(0);
        }
    }

    private static void busyWork() {
        double sum = 0;
        for (int i = 0; i < 500_000; i++) {
            sum += Math.sqrt(i);
        }
        if (sum < 0) throw new AssertionError("unreachable");
    }
}
