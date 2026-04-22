/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import java.util.Optional;

/**
 * Глобальная точка доступа к текущему {@link VidaEnvironment}.
 *
 * <p>Устанавливается один раз в {@link VidaBoot#boot(BootOptions)}; после
 * этого {@link #current()} возвращает одно и то же значение до завершения
 * процесса. Повторная установка — программная ошибка ({@link IllegalStateException}).
 */
@ApiStatus.Preview("loader")
public final class VidaRuntime {

    private static volatile VidaEnvironment ENV;

    private VidaRuntime() {}

    /**
     * @return текущий environment, если бутстрап произошёл; пусто — иначе.
     */
    public static Optional<VidaEnvironment> maybeCurrent() {
        return Optional.ofNullable(ENV);
    }

    /** @throws IllegalStateException если бутстрап ещё не выполнен. */
    public static VidaEnvironment current() {
        VidaEnvironment e = ENV;
        if (e == null) {
            throw new IllegalStateException("Vida is not booted yet. Call VidaBoot.boot(...) first.");
        }
        return e;
    }

    static synchronized void install(VidaEnvironment env) {
        if (ENV != null) {
            throw new IllegalStateException("Vida runtime already installed");
        }
        ENV = env;
    }

    /** Только для тестов: сбрасывает рантайм. Не публикуется как API. */
    static synchronized void resetForTests() {
        ENV = null;
    }
}
