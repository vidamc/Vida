/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.senda;

import dev.vida.base.ModContext;
import dev.vida.base.VidaEntrypoint;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.EjecutorLatido;
import dev.vida.base.latidos.LatidoRegistrador;
import dev.vida.base.latidos.Suscripcion;
import dev.vida.core.ApiStatus;
import java.util.List;

/**
 * Entrypoint мода Senda — навигационный путеводитель.
 *
 * <h2>Демонстрируемые API</h2>
 * <ul>
 *   <li><b>{@link dev.vida.base.catalogo.CatalogoManejador}</b> — официальный реестр путевых
 *       точек через {@link SendaCatalogo}.</li>
 *   <li><b>{@link dev.vida.base.ajustes.AjustesTipados}</b> — читает {@code senda.toml}.</li>
 *   <li><b>{@link dev.vida.base.latidos.LatidoBus}</b> — события добавления/удаления
 *       точек ({@link SendaLatidos}).</li>
 * </ul>
 *
 * <h2>Жизненный цикл</h2>
 * <ol>
 *   <li>{@link #iniciar} — читает конфиг, создаёт {@link SendaCatalogo},
 *       подписывается на событие добавления точки для логирования.</li>
 *   <li>{@link #detener} — отменяет подписки, освобождает ресурсы.</li>
 * </ol>
 */
@ApiStatus.Preview("senda")
@VidaEntrypoint
public final class SendaMod implements VidaMod {

    private SendaCatalogo catalogo;
    private List<Suscripcion> suscripciones;

    @Override
    public void iniciar(ModContext ctx) {
        SendaConfig config = SendaConfig.desde(ctx.ajustes());
        catalogo = new SendaCatalogo(ctx.catalogos(), ctx.latidos(), config);

        // Подписка через LatidoRegistrador
        suscripciones = LatidoRegistrador.registrarEnObjeto(ctx.latidos(), this);

        ctx.log().info("Senda iniciado (maxPuntos={}, dimensionInicial={})",
                config.maxPuntosPorDimension(), config.dimensionInicial());
    }

    /**
     * Логирует появление новой путевой точки.
     *
     * @param evento событие добавления точки
     */
    @EjecutorLatido
    public void onPuntoAgregado(SendaLatidos.PuntoAgregado evento) {
        // Мод-логика: любой другой мод тоже может подписаться на это событие
    }

    /**
     * Возвращает каталог путевых точек. Доступно другим модам через Catalogo-API.
     *
     * @return каталог Senda; {@code null} если мод не инициирован
     */
    public SendaCatalogo catalogo() {
        return catalogo;
    }

    @Override
    public void detener(ModContext ctx) {
        if (suscripciones != null) {
            suscripciones.forEach(Suscripcion::cancelar);
        }
    }
}
