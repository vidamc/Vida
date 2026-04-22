/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import static org.assertj.core.api.Assertions.*;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

final class CatalogoTest {

    record Bloque(String nombre) {}

    private static final String REG_ID = "ejemplo:block";

    private DefaultCatalogo<Bloque> nuevo() {
        return DefaultCatalogo.de(REG_ID, Bloque.class);
    }

    @Test
    void register_and_lookup_by_key_and_numeric() {
        DefaultCatalogo<Bloque> c = nuevo();
        var k1 = CatalogoClave.<Bloque>de(REG_ID, "ejemplo:uno");
        var k2 = CatalogoClave.<Bloque>de(REG_ID, "ejemplo:dos");

        Inscripcion<Bloque> i1 = c.registrar(k1, new Bloque("uno")).unwrap();
        Inscripcion<Bloque> i2 = c.registrar(k2, new Bloque("dos")).unwrap();

        assertThat(i1.numerico()).isEqualTo(0);
        assertThat(i2.numerico()).isEqualTo(1);

        assertThat(c.contiene(k1)).isTrue();
        assertThat(c.obtener(k2)).map(Bloque::nombre).contains("dos");
        assertThat(c.obtener(0)).map(Bloque::nombre).contains("uno");
        assertThat(c.numericoDe(k2)).contains(1);
    }

    @Test
    void duplicate_registration_returns_err() {
        DefaultCatalogo<Bloque> c = nuevo();
        var k = CatalogoClave.<Bloque>de(REG_ID, "ejemplo:x");
        c.registrar(k, new Bloque("a"));
        var res = c.registrar(k, new Bloque("b"));
        assertThat(res.isErr()).isTrue();
        assertThat(res.unwrapErr()).isInstanceOf(CatalogoError.ClaveDuplicada.class);
    }

    @Test
    void foreign_key_refused() {
        DefaultCatalogo<Bloque> c = nuevo();
        var k = CatalogoClave.<Bloque>de("otro:block", "ejemplo:x");
        var res = c.registrar(k, new Bloque("x"));
        assertThat(res.isErr()).isTrue();
        assertThat(res.unwrapErr()).isInstanceOf(CatalogoError.ClaveAjena.class);
    }

    @Test
    void congelar_blocks_further_registration() {
        DefaultCatalogo<Bloque> c = nuevo();
        var k = CatalogoClave.<Bloque>de(REG_ID, "ejemplo:x");
        c.registrar(k, new Bloque("x"));
        c.congelar();
        assertThat(c.congelado()).isTrue();

        var k2 = CatalogoClave.<Bloque>de(REG_ID, "ejemplo:y");
        var res = c.registrar(k2, new Bloque("y"));
        assertThat(res.isErr()).isTrue();
        assertThat(res.unwrapErr()).isInstanceOf(CatalogoError.CatalogoCerrado.class);
    }

    @Test
    void registrarOExigir_throws_on_dup() {
        DefaultCatalogo<Bloque> c = nuevo();
        var k = CatalogoClave.<Bloque>de(REG_ID, "ejemplo:x");
        c.registrarOExigir(k, new Bloque("x"));
        assertThatThrownBy(() -> c.registrarOExigir(k, new Bloque("y")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void iteration_order_stable() {
        DefaultCatalogo<Bloque> c = nuevo();
        for (int i = 0; i < 5; i++) {
            c.registrar(CatalogoClave.de(REG_ID, "ejemplo:b" + i), new Bloque("b" + i));
        }
        int k = 0;
        for (Inscripcion<Bloque> i : c.inscripciones()) {
            assertThat(i.numerico()).isEqualTo(k++);
        }
        assertThat(c.valores()).hasSize(5);
    }

    @Test
    void snapshot_is_immutable_and_independent() {
        DefaultCatalogo<Bloque> c = nuevo();
        var k = CatalogoClave.<Bloque>de(REG_ID, "ejemplo:x");
        c.registrar(k, new Bloque("x"));
        Catalogo<Bloque> snap = c.snapshot();

        c.registrar(CatalogoClave.de(REG_ID, "ejemplo:y"), new Bloque("y"));
        assertThat(snap.tamanio()).isEqualTo(1);
        assertThat(c.tamanio()).isEqualTo(2);
        assertThat(snap.congelado()).isTrue();
    }

    @Test
    void manager_opens_and_reuses() {
        CatalogoManejador m = new CatalogoManejador();
        CatalogoMutable<Bloque> a = m.abrir(Identifier.parse(REG_ID), Bloque.class);
        CatalogoMutable<Bloque> b = m.abrir(REG_ID, Bloque.class);
        assertThat(a).isSameAs(b);
        assertThat(m.cantidad()).isEqualTo(1);
    }

    @Test
    void manager_prevents_type_mismatch() {
        CatalogoManejador m = new CatalogoManejador();
        m.abrir(REG_ID, Bloque.class);
        assertThatThrownBy(() -> m.abrir(REG_ID, String.class))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void manager_freezes_all() {
        CatalogoManejador m = new CatalogoManejador();
        CatalogoMutable<Bloque> a = m.abrir("a:one", Bloque.class);
        CatalogoMutable<Bloque> b = m.abrir("b:two", Bloque.class);
        m.congelarTodo();
        assertThat(a.congelado()).isTrue();
        assertThat(b.congelado()).isTrue();
    }

    @Test
    void numeric_out_of_range_is_empty() {
        DefaultCatalogo<Bloque> c = nuevo();
        assertThat(c.obtener(-1)).isEmpty();
        assertThat(c.obtener(999)).isEmpty();
    }
}
