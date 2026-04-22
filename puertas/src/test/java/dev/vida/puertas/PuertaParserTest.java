/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PuertaParserTest {

    @Test
    void cabecera_correcta() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                """);
        assertThat(r.esExitoso()).isTrue();
        assertThat(r.archivo().version()).isEqualTo(1);
        assertThat(r.archivo().namespace()).isEqualTo(Namespace.INTERMEDIO);
        assertThat(r.archivo().directivas()).isEmpty();
    }

    @Test
    void cabecera_faltante_version_lanza_error() {
        var r = PuertaParser.parsear("t.ptr", "vida-puertas namespace=intermedio\n");
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0))
                .isInstanceOf(PuertaError.CabeceraInvalida.class);
    }

    @Test
    void cabecera_namespace_desconocido() {
        var r = PuertaParser.parsear("t.ptr", "vida-puertas 1 namespace=xyz\n");
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0))
                .isInstanceOf(PuertaError.NamespaceDesconocido.class);
    }

    @Test
    void version_no_soportada_reportada() {
        var r = PuertaParser.parsear("t.ptr", "vida-puertas 99 namespace=crudo\n");
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0)).isInstanceOf(PuertaError.VersionNoSoportada.class);
    }

    @Test
    void accesible_class() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible class net/example/Foo
                """);
        assertThat(r.esExitoso()).isTrue();
        assertThat(r.archivo().directivas()).hasSize(1);
        PuertaDirectiva d = r.archivo().directivas().get(0);
        assertThat(d.accion()).isEqualTo(Accion.ACCESIBLE);
        assertThat(d.objetivo()).isEqualTo(Objetivo.CLASE);
        assertThat(d.claseInternal()).isEqualTo("net/example/Foo");
    }

    @Test
    void normaliza_puntos_a_barras() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=exterior
                accesible class net.example.Foo
                """);
        assertThat(r.archivo().directivas().get(0).claseInternal()).isEqualTo("net/example/Foo");
    }

    @Test
    void comentarios_y_vacias_ignoradas() {
        var r = PuertaParser.parsear("t.ptr", """
                # comentario
                vida-puertas 1 namespace=intermedio
                
                # otra
                accesible class A
                """);
        assertThat(r.esExitoso()).isTrue();
        assertThat(r.archivo().directivas()).hasSize(1);
    }

    @Test
    void metodo_descriptor_valido() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible method net/X m (ILjava/lang/String;)V
                """);
        assertThat(r.esExitoso()).isTrue();
        PuertaDirectiva d = r.archivo().directivas().get(0);
        assertThat(d.objetivo()).isEqualTo(Objetivo.METODO);
        assertThat(d.nombreMiembro()).contains("m");
        assertThat(d.descriptor()).contains("(ILjava/lang/String;)V");
    }

    @Test
    void campo_descriptor_valido() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible field net/X count I
                mutable   field net/X counter Ljava/util/concurrent/atomic/AtomicLong;
                """);
        assertThat(r.esExitoso()).isTrue();
        assertThat(r.archivo().directivas()).hasSize(2);
    }

    @Test
    void descriptor_malformado() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible field net/X count XX
                """);
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0)).isInstanceOf(PuertaError.DescriptorMalformado.class);
    }

    @Test
    void directiva_truncada() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible method net/X
                """);
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0)).isInstanceOf(PuertaError.DirectivaTruncada.class);
    }

    @Test
    void mutable_clase_rechazado() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                mutable class net/X
                """);
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0)).isInstanceOf(PuertaError.MutableNoAplicable.class);
    }

    @Test
    void mutable_metodo_rechazado() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                mutable method net/X m ()V
                """);
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0)).isInstanceOf(PuertaError.MutableNoAplicable.class);
    }

    @Test
    void directiva_invalida() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                broma class net/X
                """);
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0)).isInstanceOf(PuertaError.DirectivaInvalida.class);
    }

    @Test
    void indice_por_clase() {
        var r = PuertaParser.parsear("t.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible class net/A
                accesible class net/B
                accesible field net/A f I
                """);
        assertThat(r.archivo().paraClase("net/A")).hasSize(2);
        assertThat(r.archivo().paraClase("net/B")).hasSize(1);
        assertThat(r.archivo().paraClase("net/Z")).isEmpty();
        assertThat(r.archivo().clases()).containsExactlyInAnyOrder("net/A", "net/B");
    }

    @Test
    void archivo_vacio_sin_header() {
        var r = PuertaParser.parsear("t.ptr", "\n\n# comentario\n");
        assertThat(r.esExitoso()).isFalse();
        assertThat(r.errores().get(0)).isInstanceOf(PuertaError.CabeceraInvalida.class);
    }

    @Test
    void combinar_fusiona_directivas() {
        var a = PuertaParser.parsear("a.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible class net/A
                """).archivo();
        var b = PuertaParser.parsear("b.ptr", """
                vida-puertas 1 namespace=intermedio
                accesible class net/B
                """).archivo();
        var comb = PuertaArchivo.combinar("combined", java.util.List.of(a, b));
        assertThat(comb.directivas()).hasSize(2);
        assertThat(comb.clases()).contains("net/A", "net/B");
    }

    @Test
    void combinar_namespace_diferente_lanza() {
        var a = PuertaParser.parsear("a.ptr", """
                vida-puertas 1 namespace=intermedio
                """).archivo();
        var b = PuertaParser.parsear("b.ptr", """
                vida-puertas 1 namespace=exterior
                """).archivo();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> PuertaArchivo.combinar("x", java.util.List.of(a, b)));
    }
}
