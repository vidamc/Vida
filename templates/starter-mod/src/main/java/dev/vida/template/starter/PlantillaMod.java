/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.template.starter;

import dev.vida.base.ModContext;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.Prioridad;
import dev.vida.base.latidos.eventos.LatidoPulso;

/** Минимальный entrypoint для копируемого шаблона (см. templates/starter-mod/README.md). */
public final class PlantillaMod implements VidaMod {

    @Override
    public void iniciar(ModContext ctx) {
        ctx.log().info("Plantilla iniciada: {}", ctx.metadata().nombre());

        ctx.latidos().suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, pulso -> {
            if (pulso.tickActual() % 100 == 0) {
                ctx.log().debug("tick {}", pulso.tickActual());
            }
        });
    }
}
