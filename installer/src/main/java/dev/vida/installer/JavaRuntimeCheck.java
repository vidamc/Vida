/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import java.util.Objects;

/**
 * Минимальный {@link Runtime.Version}-чекер.
 *
 * <p>Используется для «светофора» на старте: если текущая JVM &lt; 21 —
 * инсталлятор всё равно может установить файлы, но предупреждает, что
 * Vida не запустится на этой машине.
 */
public final class JavaRuntimeCheck {

    public static final int REQUIRED_MAJOR = 21;

    private JavaRuntimeCheck() {}

    /** Результат проверки. */
    public record Result(int major, String version, boolean ok, String message) {
        public Result {
            Objects.requireNonNull(version, "version");
            Objects.requireNonNull(message, "message");
        }
    }

    /** Проверяет текущую JVM. */
    public static Result checkCurrent() {
        return check(Runtime.version());
    }

    public static Result check(Runtime.Version v) {
        int major = v.feature();
        boolean ok = major >= REQUIRED_MAJOR;
        String msg = ok
                ? "Java " + major + " is supported."
                : "Java " + major + " detected, but Vida requires Java "
                        + REQUIRED_MAJOR + " or newer.";
        return new Result(major, v.toString(), ok, msg);
    }
}
