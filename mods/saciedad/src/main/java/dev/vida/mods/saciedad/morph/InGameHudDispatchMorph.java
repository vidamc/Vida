/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad.morph;

import dev.vida.base.LatidoGlobal;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.ApiStatus;
import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;
import dev.vida.vifada.CallbackInfo;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.VifadaAt;
import dev.vida.vifada.VifadaInject;
import dev.vida.vifada.VifadaMorph;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Vifada-морф, инжектирующийся в {@code net.minecraft.client.gui.Gui.render()}
 * (Minecraft 1.20.5+) и публикующий {@link LatidoRenderHud} на глобальную шину
 * событий в начале каждого HUD-кадра.
 *
 * <h2>Требования к окружению</h2>
 * <ul>
 *   <li>Minecraft 1.20.5+ с Mojang-mapped JARs (или remapper-слоем, который
 *       раскрывает {@code net.minecraft.client.gui.Gui}).</li>
 *   <li>{@link LatidoGlobal} установлен загрузчиком ({@code BootSequence})
 *       до начала рендеринга.</li>
 * </ul>
 *
 * <h2>Graceful fallback</h2>
 * {@code requireTarget = false} — если целевой класс/метод не найдены
 * (обфусцированный Minecraft, другая версия), морф молча пропускается.
 * Никаких исключений, никаких ошибок загрузки.
 *
 * <h2>Размещение в рендер-цикле</h2>
 * Инжекция выполняется в самом начале {@code Gui.render(GuiGraphics, float)}
 * (точка {@link InjectionPoint#HEAD}), т. е. до отрисовки vanilla-элементов
 * HUD. Подписчики с {@code Fase.ANTES} получат событие раньше vanilla,
 * {@code Fase.DESPUES} — после (когда vanilla HUD уже отрисован, но кадр
 * ещё не отправлен).
 *
 * <p><b>Примечание:</b> для отрисовки «поверх» vanilla HUD подписывайтесь
 * на {@code Fase.DESPUES}; для «под» — на {@code Fase.ANTES}.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.gui.Gui")
public final class InGameHudDispatchMorph {

    /**
     * Вызывается в начале каждого HUD-кадра.
     *
     * <p>Получает ширину и высоту экрана через рефлексию (чтобы не тащить
     * зависимость от {@code Minecraft} в compile-classpath), создаёт лямбду
     * {@link PintorHud} поверх {@link GuiGraphics#fill} и публикует событие.
     *
     * @param guiGraphics контекст рисования кадра
     * @param partialTick дробная часть тика (0.0–1.0)
     * @param ci          callback-info (не используется — инжекция не отменяется)
     */
    @VifadaInject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    public void onVidaHudRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        LatidoBus bus = LatidoGlobal.maybeCurrent().orElse(null);
        if (bus == null) return;

        int screenW = resolveScreenWidth();
        int screenH = resolveScreenHeight();
        if (screenW <= 0 || screenH <= 0) return;

        PintorHud pintor =
                (x, y, ancho, alto, colorArgb) ->
                        guiGraphics.fill(x, y, x + ancho, y + alto, colorArgb);

        bus.emitir(LatidoRenderHud.TIPO,
                new LatidoRenderHud(screenW, screenH, partialTick, pintor));
    }

    // ---- helpers (reflection-based — no compile-time MC dependency) --------

    private static int resolveScreenWidth() {
        return resolveGuiDimension("getGuiScaledWidth");
    }

    private static int resolveScreenHeight() {
        return resolveGuiDimension("getGuiScaledHeight");
    }

    /**
     * Возвращает размер GUI-вьюпорта через {@code Minecraft.getInstance().getWindow()}.
     * При любой рефлексионной ошибке возвращает {@code -1}.
     */
    private static int resolveGuiDimension(String methodName) {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object window = mc.getClass().getMethod("getWindow").invoke(mc);
            return (int) window.getClass().getMethod(methodName).invoke(window);
        } catch (Exception ex) {
            return -1;
        }
    }
}
