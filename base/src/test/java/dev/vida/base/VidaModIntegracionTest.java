/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import static org.assertj.core.api.Assertions.*;

import dev.vida.base.ajustes.Ajuste;
import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.base.catalogo.CatalogoClave;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.catalogo.CatalogoMutable;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.base.latidos.eventos.LatidoArranque;
import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.config.Ajustes;
import dev.vida.core.Log;
import dev.vida.core.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Проверяет, что весь публичный API можно собрать и использовать
 * <b>снаружи</b> без :loader — как это делается в unit-тестах мода.
 */
final class VidaModIntegracionTest {

    record Bloque(String nombre) {}

    static final class EjemploMod implements VidaMod {
        final List<Long> ticks = new ArrayList<>();
        final List<LatidoArranque> arranques = new ArrayList<>();
        CatalogoMutable<Bloque> bloques;

        @Override
        public void iniciar(ModContext ctx) {
            ctx.log().info("mod {} iniciando", ctx.id());

            bloques = ctx.catalogos().abrir("demo:block", Bloque.class);
            bloques.registrarOExigir(CatalogoClave.de("demo:block", "demo:cobble"),
                    new Bloque("cobble"));
            bloques.registrarOExigir(CatalogoClave.de("demo:block", "demo:wood"),
                    new Bloque("wood"));

            ctx.latidos().suscribir(LatidoPulso.TIPO, e -> ticks.add(e.tickActual()));
            ctx.latidos().suscribir(LatidoArranque.TIPO, arranques::add);

            int dist = ctx.ajustes().valor(
                    Ajuste.entero("render.distance", 32).min(2).max(64).build());
            ctx.log().info("using render distance {}", dist);
        }
    }

    @Test
    void mod_full_lifecycle(@TempDir Path dir) throws Exception {
        // Раскладываем тестовую инфраструктуру вручную — как делает :loader.
        LatidoBus bus = LatidoBus.enMemoria();
        CatalogoManejador catalogos = new CatalogoManejador();
        AjustesTipados ajustes = AjustesTipados.sobre(Ajustes.empty());
        Path datos = Files.createDirectories(dir.resolve("data"));

        ModMetadata meta = ModMetadata.con("demo", Version.parse("0.1.0"), "Demo Mod");
        ModContext ctx = new DefaultModContext(
                meta, bus, catalogos, ajustes, Log.of("vida.mod.demo"), datos);

        EjemploMod mod = new EjemploMod();
        mod.iniciar(ctx);

        // Регистрация сработала:
        assertThat(mod.bloques.tamanio()).isEqualTo(2);
        assertThat(catalogos.obtener(mod.bloques.reestroId(), Bloque.class))
                .isPresent();

        // Тикаем и публикуем «arranque»:
        AtomicInteger monitorTicks = new AtomicInteger();
        bus.suscribir(LatidoPulso.TIPO, dev.vida.base.latidos.Prioridad.MONITOR,
                e -> monitorTicks.incrementAndGet());
        for (int i = 0; i < 5; i++) bus.emitir(LatidoPulso.TIPO, LatidoPulso.raiz(i));

        bus.emitir(LatidoArranque.TIPO, new LatidoArranque(Instant.now(), 1));

        assertThat(mod.ticks).containsExactly(0L, 1L, 2L, 3L, 4L);
        assertThat(monitorTicks).hasValue(5);
        assertThat(mod.arranques).hasSize(1);

        // После freeze регистрация запрещена:
        catalogos.congelarTodo();
        var res = mod.bloques.registrar(
                CatalogoClave.de("demo:block", "demo:new"), new Bloque("new"));
        assertThat(res.isErr()).isTrue();
    }
}
