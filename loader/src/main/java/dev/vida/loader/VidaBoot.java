/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.telemetry.TelemetriaV1;
import dev.vida.loader.internal.BootSequence;
import dev.vida.loader.internal.DesarrolloHotReloadServicio;
import java.lang.instrument.Instrumentation;
import java.util.Objects;

/**
 * Программный бутстрап Vida.
 *
 * <p>Это «безагентный» путь — подходит для тестов, девелоперской
 * рантайм-сборки и любых инструментов, которым не нужно (или нельзя)
 * запускать JVM с {@code -javaagent}. В продуктивном сценарии запуск
 * обычно идёт через {@link VidaPremain}, но он делегирует сюда же.
 *
 * <p>После успешного вызова окружение регистрируется в
 * {@link VidaRuntime#current()}. Повторный запуск в том же процессе не
 * поддерживается.
 */
@ApiStatus.Stable
public final class VidaBoot {

    private static final Log LOG = Log.of(VidaBoot.class);

    private VidaBoot() {}

    /** Основная точка входа. */
    public static BootReport boot(BootOptions options) {
        return boot(options, Thread.currentThread().getContextClassLoader(), null);
    }

    /**
     * Расширенная точка входа, принимает явный родительский ClassLoader и
     * опциональный {@link Instrumentation} (когда мы вызваны из
     * {@link VidaPremain#premain}/{@link VidaPremain#agentmain}).
     */
    public static BootReport boot(BootOptions options, ClassLoader parent, Instrumentation inst) {
        Objects.requireNonNull(options, "options");
        LOG.info("Vida boot starting (strict={}, skipDiscovery={}, modsDir={})",
                options.strict(), options.skipDiscovery(), options.modsDir());

        BootReport report = BootSequence.run(options, parent, inst);
        if (report.isOk()) {
            VidaRuntime.install(report.environment());
            TelemetriaV1.registrarArranqueFrioNanos(report.duration().toNanos());
            DesarrolloHotReloadServicio.maybeStart();
            LOG.info("Vida boot OK in {} ms: {} mods, {} morph-targets, {} morphs",
                    report.duration().toMillis(),
                    report.environment().resolvedMods().size(),
                    report.environment().morphs().targetCount(),
                    report.environment().morphs().totalMorphs());
        } else {
            LOG.error("Vida boot FAILED in {} ms with {} errors",
                    report.duration().toMillis(), report.errors().size());
            for (LoaderError e : report.errors()) {
                LOG.error("  - {}", e);
            }
        }
        return report;
    }
}
