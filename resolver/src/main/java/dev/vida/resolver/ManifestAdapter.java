/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import dev.vida.core.VersionRange;
import dev.vida.manifest.ModDependencies;
import dev.vida.manifest.ModManifest;
import java.util.Map;
import java.util.Objects;

/**
 * Мостик между {@link ModManifest} и моделью резолвера. Отдельным модулем
 * выделен, чтобы домен резолвера оставался независимым от манифеста —
 * модуль {@code :manifest} используется только здесь.
 */
@ApiStatus.Stable
public final class ManifestAdapter {

    private ManifestAdapter() {}

    /**
     * Собирает {@link Provider} из манифеста. Опциональный {@code attachment}
     * сохраняется в {@link Provider#attachment()}, позволяя пользователям
     * связать провайдер с исходным {@code ModCandidate} или чем-то ещё.
     *
     * <p>Поля {@code dependencies.required}, {@code dependencies.optional} и
     * {@code dependencies.incompatibilities} разворачиваются в
     * {@link Dependency} соответствующих видов.
     */
    public static Provider toProvider(ModManifest manifest, Object attachment) {
        Objects.requireNonNull(manifest, "manifest");

        Provider.Builder b = Provider.builder(manifest.id(), manifest.version());
        if (attachment != null) b.attachment(attachment);

        ModDependencies deps = manifest.dependencies();
        for (Map.Entry<String, VersionRange> e : deps.required().entrySet()) {
            b.dependency(Dependency.required(e.getKey(), e.getValue()));
        }
        for (Map.Entry<String, VersionRange> e : deps.optional().entrySet()) {
            b.dependency(Dependency.optional(e.getKey(), e.getValue()));
        }
        for (Map.Entry<String, VersionRange> e : deps.incompatibilities().entrySet()) {
            b.dependency(Dependency.incompatible(e.getKey(), e.getValue()));
        }
        return b.build();
    }

    /** Shortcut без attachment. */
    public static Provider toProvider(ModManifest manifest) {
        return toProvider(manifest, null);
    }
}
