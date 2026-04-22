/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.core.ApiStatus;

/**
 * Главная точка входа мода.
 *
 * <p>Контракт жизненного цикла:
 * <ol>
 *   <li>Vida находит класс-entrypoint и создаёт его экземпляр через
 *       no-args-конструктор;</li>
 *   <li>один раз вызывает {@link #iniciar(ModContext) iniciar(ModContext)};</li>
 *   <li>мод переходит в {@link EstadoMod#INICIADO}, затем в
 *       {@link EstadoMod#ACTIVO} после старта игры;</li>
 *   <li>при завершении — вызывается {@link #detener(ModContext)
 *       detener(ModContext)}, после чего экземпляр мода должен остановить
 *       все свои потоки/таймеры и освободить ресурсы.</li>
 * </ol>
 *
 * <p>Ни один из методов не должен блокироваться надолго: инициализация
 * мода идёт синхронно, так что slow-startup одного мода тормозит весь
 * процесс. Тяжёлые задачи (IO, генерация таблиц) выносите в фоновые
 * пулы или отложите до {@code LatidoArranque}.
 *
 * <h2>Минимальный пример</h2>
 *
 * <pre>{@code
 * public final class MiMod implements VidaMod {
 *     @Override
 *     public void iniciar(ModContext ctx) {
 *         ctx.log().info("¡Hola, soy {}!", ctx.metadata().nombre());
 *     }
 * }
 * }</pre>
 */
@ApiStatus.Preview("base")
public interface VidaMod {

    /**
     * Инициализация мода. Вызывается один раз сразу после загрузки классов
     * мода. Здесь типично происходит регистрация контента в
     * {@link dev.vida.base.catalogo.Catalogo} и подписка на
     * {@link dev.vida.base.latidos.LatidoBus}.
     */
    void iniciar(ModContext ctx);

    /**
     * Выгрузка мода. Вызывается при корректном shutdown Vida либо при
     * «горячей» выгрузке мода в dev-окружении. Дефолтная реализация —
     * no-op: если моду нечего освобождать, метод можно не переопределять.
     */
    default void detener(ModContext ctx) {
        // no-op
    }
}
