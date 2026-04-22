/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.ApiStatus;
import java.util.Optional;

/**
 * Глобальная точка доступа к единственной рантайм {@link LatidoBus}.
 *
 * <p>Устанавливается один раз загрузчиком ({@code BootSequence}) в самом
 * начале бутстрапа — до вызова любого entrypoint'а. После установки
 * {@link #actual()} возвращает один и тот же экземпляр на протяжении всей
 * жизни процесса.
 *
 * <p>Главный смысл существования этого класса — дать Vifada-морфам (которые
 * зависят только от {@code :base}, а не от {@code :loader}) доступ к шине
 * событий без циклической зависимости:
 *
 * <pre>{@code
 * // В Vifada-морфе:
 * LatidoGlobal.maybeCurrent().ifPresent(bus ->
 *         bus.emitir(LatidoRenderHud.TIPO, evento));
 * }</pre>
 *
 * <h2>Безопасность</h2>
 * {@code instalar} помечен {@link dev.vida.core.ApiStatus.Internal} — вызывать его вне
 * загрузчика незаконно. Повторная установка бросает
 * {@link IllegalStateException}.
 */
@ApiStatus.Preview("base")
public final class LatidoGlobal {

    private static volatile LatidoBus INSTANCE;

    private LatidoGlobal() {}

    /**
     * Возвращает глобальную шину, если бутстрап уже выполнен.
     *
     * @return шина, или {@link Optional#empty()} до вызова {@link #instalar}
     */
    public static Optional<LatidoBus> maybeCurrent() {
        return Optional.ofNullable(INSTANCE);
    }

    /**
     * Возвращает глобальную шину.
     *
     * @throws IllegalStateException если загрузчик ещё не установил шину
     */
    public static LatidoBus actual() {
        LatidoBus b = INSTANCE;
        if (b == null) {
            throw new IllegalStateException(
                    "LatidoGlobal: bus not installed yet — call BootSequence first.");
        }
        return b;
    }

    /**
     * Устанавливает глобальную шину. Вызывается ровно один раз из
     * {@code BootSequence}; повторный вызов бросает {@link IllegalStateException}.
     *
     * @param bus шина событий, не {@code null}
     */
    @ApiStatus.Internal
    public static synchronized void instalar(LatidoBus bus) {
        if (bus == null) throw new NullPointerException("bus");
        if (INSTANCE != null) throw new IllegalStateException("LatidoGlobal already installed");
        INSTANCE = bus;
    }

    /** Только для тестов — сбрасывает глобальное состояние. */
    @ApiStatus.Internal
    public static synchronized void resetForTests() {
        INSTANCE = null;
    }
}
