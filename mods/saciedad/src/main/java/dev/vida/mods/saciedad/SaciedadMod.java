/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad;

import dev.vida.base.ModContext;
import dev.vida.base.VidaEntrypoint;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.Fase;
import dev.vida.base.latidos.LatidoRegistrador;
import dev.vida.base.latidos.OyenteDeTick;
import dev.vida.base.latidos.Prioridad;
import dev.vida.base.latidos.Suscripcion;
import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.core.ApiStatus;
import dev.vida.render.LatidoRenderHud;
import java.util.List;

/**
 * Entrypoint мода Saciedad — шкала насыщения.
 *
 * <h2>Жизненный цикл</h2>
 * <ol>
 *   <li>{@link #iniciar} — читает конфиг, создаёт рендерер, подписывается
 *       на {@link LatidoPulso} (для синхронизации состояния) и
 *       {@link LatidoRenderHud} (для отрисовки).</li>
 *   <li>{@link #detener} — отменяет все подписки.</li>
 * </ol>
 *
 * <h2>Производительность</h2>
 * Подписка на {@code LatidoPulso} работает с throttling {@code tps=20}, но не
 * содержит тяжёлой логики — значение насыщения приходит напрямую из
 * {@link SaciedadCache}, который заполняет инжектор {@code FoodDataSaciedadMorph}.
 */
@ApiStatus.Preview("saciedad")
@VidaEntrypoint
public final class SaciedadMod implements VidaMod {

    private SaciedadHudRenderizador renderizador;
    private List<Suscripcion> suscripcionesTick;
    private Suscripcion suscripcionFondo;
    private Suscripcion suscripcionBarra;

    @Override
    public void iniciar(ModContext ctx) {
        SaciedadConfig config = SaciedadConfig.desde(ctx.ajustes());
        renderizador = new SaciedadHudRenderizador(config);

        // Подписка @OyenteDeTick через LatidoRegistrador (reflection-биндер)
        suscripcionesTick = LatidoRegistrador.registrarEnObjeto(ctx.latidos(), this);

        // Два подписчика на LatidoRenderHud с Fase.DESPUES (фон + сама шкала)
        suscripcionFondo = ctx.latidos().suscribir(
                LatidoRenderHud.TIPO,
                Prioridad.NORMAL,
                Fase.DESPUES,
                renderizador::renderizarFondo);

        suscripcionBarra = ctx.latidos().suscribir(
                LatidoRenderHud.TIPO,
                Prioridad.NORMAL,
                Fase.DESPUES,
                renderizador::renderizarBarra);

        ctx.log().info("Saciedad iniciado (posicion={}, color=#{}, mostrarSiempre={})",
                config.posicion(), Integer.toHexString(config.color()).toUpperCase(),
                config.mostrarSiempre());
    }

    /**
     * Вызывается каждый тик (20 раз в секунду при стандартной частоте).
     *
     * <p>Синхронизирует состояние: {@link SaciedadCache} обновляется инжектором
     * {@code FoodDataSaciedadMorph}; здесь достаточно просто существовать как
     * «anchor» тика, чтобы система событий работала корректно. Метод
     * специально оставлен лёгким — никаких аллокаций.
     *
     * @param pulso событие тика
     */
    @OyenteDeTick(tps = 20)
    public void onTick(LatidoPulso pulso) {
        // Значение насыщения уже закэшировано FoodDataSaciedadMorph.
        // Здесь можно добавить дополнительную логику (напр., сброс при смерти).
    }

    @Override
    public void detener(ModContext ctx) {
        if (suscripcionFondo != null) suscripcionFondo.cancelar();
        if (suscripcionBarra != null) suscripcionBarra.cancelar();
        if (suscripcionesTick != null) suscripcionesTick.forEach(Suscripcion::cancelar);
    }
}
