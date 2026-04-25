/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.base.LatidoGlobal;
import dev.vida.base.ModulosInstaladosGlobal;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.base.latidos.eventos.FaseCicloMod;
import dev.vida.base.latidos.eventos.LatidoArranque;
import dev.vida.base.latidos.eventos.LatidoFaseCiclo;
import dev.vida.platform.VanillaBridge;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BootLifecyclePhasesTest {

    @BeforeEach
    void reset() {
        VidaRuntime.resetForTests();
        LatidoGlobal.resetForTests();
        ModulosInstaladosGlobal.resetForTests();
        VanillaBridge.resetForTests();
    }

    @AfterEach
    void cleanup() {
        VidaRuntime.resetForTests();
        LatidoGlobal.resetForTests();
        ModulosInstaladosGlobal.resetForTests();
        VanillaBridge.resetForTests();
    }

    @Test
    void fases_y_arranque_en_orden(@TempDir Path root) throws Exception {
        LatidoBus bus = LatidoBus.enMemoria();
        LatidoGlobal.instalar(bus);
        List<FaseCicloMod> fases = new ArrayList<>();
        bus.suscribir(LatidoFaseCiclo.TIPO, ev -> fases.add(ev.fase()));
        List<Integer> arr = new ArrayList<>();
        bus.suscribir(LatidoArranque.TIPO, ev -> arr.add(ev.modsCargados()));

        Path mods = root.resolve("mods");
        Files.createDirectories(mods);
        TestSupport.buildModJar(mods.resolve("solo.jar"), "solo", "1.0.0", null, null);

        BootReport report = VidaBoot.boot(BootOptions.builder().modsDir(mods).build());
        assertThat(report.errors()).isEmpty();
        assertThat(fases)
                .containsExactly(
                        FaseCicloMod.PREPARACION,
                        FaseCicloMod.INICIALIZACION,
                        FaseCicloMod.POST_INICIALIZACION);
        assertThat(arr).containsExactly(1);
    }
}
