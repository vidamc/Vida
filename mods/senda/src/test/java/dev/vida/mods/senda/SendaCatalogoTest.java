/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.senda;

import static org.assertj.core.api.Assertions.*;

import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.base.latidos.Suscripcion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class SendaCatalogoTest {

    private static final String OVERWORLD = "overworld";
    private static final String NETHER    = "nether";

    private CatalogoManejador catalogosMgr;
    private LatidoBus bus;
    private SendaCatalogo catalogo;

    @BeforeEach
    void setUp() {
        catalogosMgr = new CatalogoManejador();
        bus = LatidoBus.enMemoria();
        SendaConfig config = SendaConfig.defecto();
        catalogo = new SendaCatalogo(catalogosMgr, bus, config);
    }

    // ---------------------------------------------------------------- registrar

    @Test
    void registrar_y_buscar() {
        PuntoRuta casa = new PuntoRuta("casa", OVERWORLD, 100.0, 64.0, -200.0);
        catalogo.registrar(casa);

        Optional<PuntoRuta> resultado = catalogo.buscar("casa", OVERWORLD);
        assertThat(resultado).isPresent().contains(casa);
    }

    @Test
    void registrar_emite_evento_PuntoAgregado() {
        List<SendaLatidos.PuntoAgregado> eventos = new ArrayList<>();
        try (Suscripcion s = bus.suscribir(SendaLatidos.PuntoAgregado.TIPO, eventos::add)) {
            assertThat(s.activa()).isTrue();
            PuntoRuta punto = new PuntoRuta("base", OVERWORLD, 0.0, 60.0, 0.0);
            catalogo.registrar(punto);

            assertThat(eventos).hasSize(1);
            assertThat(eventos.getFirst().punto()).isEqualTo(punto);
        }
    }

    @Test
    void registrar_duplicado_lanza_excepcion() {
        PuntoRuta p = new PuntoRuta("x", OVERWORLD, 0, 0, 0);
        catalogo.registrar(p);

        assertThatThrownBy(() -> catalogo.registrar(p))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x");
    }

    @Test
    void registrar_limiteAlcanzado_lanza_excepcion() {
        SendaConfig limitado = new SendaConfig(2, "overworld");
        SendaCatalogo cat = new SendaCatalogo(new CatalogoManejador(), bus, limitado);

        cat.registrar(new PuntoRuta("a", OVERWORLD, 0, 0, 0));
        cat.registrar(new PuntoRuta("b", OVERWORLD, 1, 0, 0));

        assertThatThrownBy(() -> cat.registrar(new PuntoRuta("c", OVERWORLD, 2, 0, 0)))
                .isInstanceOf(SendaCatalogo.LimiteAlcanzadoException.class)
                .satisfies(e -> {
                    SendaCatalogo.LimiteAlcanzadoException le =
                            (SendaCatalogo.LimiteAlcanzadoException) e;
                    assertThat(le.limite()).isEqualTo(2);
                    assertThat(le.dimension()).isEqualTo(OVERWORLD);
                });
    }

    // ---------------------------------------------------------------- eliminar

    @Test
    void eliminar_punto_existente() {
        PuntoRuta p = new PuntoRuta("temp", OVERWORLD, 10, 64, 10);
        catalogo.registrar(p);

        Optional<PuntoRuta> eliminado = catalogo.eliminar("temp", OVERWORLD);

        assertThat(eliminado).isPresent().contains(p);
        assertThat(catalogo.buscar("temp", OVERWORLD)).isEmpty();
    }

    @Test
    void eliminar_emite_evento_PuntoEliminado() {
        catalogo.registrar(new PuntoRuta("r", OVERWORLD, 0, 0, 0));

        List<SendaLatidos.PuntoEliminado> eventos = new ArrayList<>();
        try (Suscripcion s = bus.suscribir(SendaLatidos.PuntoEliminado.TIPO, eventos::add)) {
            assertThat(s.activa()).isTrue();
            catalogo.eliminar("r", OVERWORLD);

            assertThat(eventos).hasSize(1);
            assertThat(eventos.getFirst().punto().nombre()).isEqualTo("r");
        }
    }

    @Test
    void eliminar_inexistente_devuelve_empty() {
        Optional<PuntoRuta> resultado = catalogo.eliminar("noExiste", OVERWORLD);
        assertThat(resultado).isEmpty();
    }

    // ---------------------------------------------------------------- puntosDe / cantidad

    @Test
    void puntosDe_devuelve_todos_puntos_activos() {
        catalogo.registrar(new PuntoRuta("a", OVERWORLD, 0, 0, 0));
        catalogo.registrar(new PuntoRuta("b", OVERWORLD, 1, 0, 0));
        catalogo.registrar(new PuntoRuta("n", NETHER, 0, 0, 0));

        assertThat(catalogo.puntosDe(OVERWORLD)).hasSize(2);
        assertThat(catalogo.puntosDe(NETHER)).hasSize(1);
    }

    @Test
    void cantidad_solo_activos_no_incluye_eliminados() {
        catalogo.registrar(new PuntoRuta("q", OVERWORLD, 0, 0, 0));
        catalogo.registrar(new PuntoRuta("w", OVERWORLD, 1, 0, 0));
        catalogo.eliminar("q", OVERWORLD);

        assertThat(catalogo.cantidad(OVERWORLD)).isEqualTo(1);
    }

    @Test
    void tamanioRegistro_incluye_eliminados() {
        catalogo.registrar(new PuntoRuta("z", OVERWORLD, 0, 0, 0));
        catalogo.eliminar("z", OVERWORLD);

        // Catalogo es append-only: el registro permanece
        assertThat(catalogo.tamanioRegistro(OVERWORLD)).isEqualTo(1);
        // Pero activos es 0
        assertThat(catalogo.cantidad(OVERWORLD)).isEqualTo(0);
    }

    // ---------------------------------------------------------------- limpiarDimension

    @Test
    void limpiarDimension_elimina_todos_activos() {
        catalogo.registrar(new PuntoRuta("m", OVERWORLD, 0, 0, 0));
        catalogo.registrar(new PuntoRuta("n", OVERWORLD, 1, 0, 0));

        catalogo.limpiarDimension(OVERWORLD);

        assertThat(catalogo.cantidad(OVERWORLD)).isZero();
        assertThat(catalogo.puntosDe(OVERWORLD)).isEmpty();
    }

    @Test
    void limpiarDimension_emite_evento() {
        List<SendaLatidos.DimensionLimpiada> eventos = new ArrayList<>();
        try (Suscripcion s = bus.suscribir(SendaLatidos.DimensionLimpiada.TIPO, eventos::add)) {
            assertThat(s.activa()).isTrue();
            catalogo.limpiarDimension(OVERWORLD);

            assertThat(eventos).hasSize(1);
            assertThat(eventos.getFirst().dimension()).isEqualTo(OVERWORLD);
        }
    }

    // ---------------------------------------------------------------- PuntoRuta helpers

    @Test
    void puntoRuta_distanciaHorizontal() {
        PuntoRuta a = new PuntoRuta("a", OVERWORLD, 0, 0, 0);
        PuntoRuta b = new PuntoRuta("b", OVERWORLD, 3, 99, 4);

        assertThat(a.distanciaHorizontal(b)).isCloseTo(5.0, within(0.0001));
    }

    @Test
    void puntoRuta_distancia3d() {
        PuntoRuta a = new PuntoRuta("a", OVERWORLD, 0, 0, 0);
        PuntoRuta b = new PuntoRuta("b", OVERWORLD, 1, 1, 1);

        assertThat(a.distancia3d(b)).isCloseTo(Math.sqrt(3), within(0.0001));
    }

    @Test
    void puntoRuta_distancia_dimensiones_distintas() {
        PuntoRuta a = new PuntoRuta("a", OVERWORLD, 0, 0, 0);
        PuntoRuta b = new PuntoRuta("b", NETHER, 0, 0, 0);

        assertThat(a.distanciaHorizontal(b)).isNaN();
        assertThat(a.distancia3d(b)).isNaN();
    }

    @Test
    void puntoRuta_validacion_nombreVacio() {
        assertThatThrownBy(() -> new PuntoRuta("", OVERWORLD, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void puntoRuta_validacion_dimensionVacia() {
        assertThatThrownBy(() -> new PuntoRuta("p", "", 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
