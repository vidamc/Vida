/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.manifest.ModManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Проверка структуры datapack при сборке (Gradle): корень из {@code custom["vida:dataDriven"]}
 * должен существовать под {@code src/main/resources}.
 */
@ApiStatus.Stable
public final class FuenteManifestoDatapackValidador {

    private static final String CONFIG_KEY = "vida:dataDriven";

    private FuenteManifestoDatapackValidador() {}

    /**
     * @param resourcesRoot корень ресурсов модуля (обычно {@code src/main/resources})
     * @return сообщение об ошибке или пусто, если проверка не применима / успешна
     */
    public static Optional<String> validarRecursos(ModManifest manifest, Path resourcesRoot) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(resourcesRoot, "resourcesRoot");
        Config cfg = parseConfig(manifest);
        if (!cfg.enabled()) {
            return Optional.empty();
        }
        Path base = resourcesRoot;
        for (String seg : cfg.datapackRoot().split("/")) {
            if (!seg.isEmpty()) {
                base = base.resolve(seg);
            }
        }
        if (!Files.isDirectory(base)) {
            return Optional.of("vida:dataDriven enabled but datapack root missing: " + base);
        }
        return Optional.empty();
    }

    private static Config parseConfig(ModManifest manifest) {
        Object raw = manifest.custom().get(CONFIG_KEY);
        if (raw == null) {
            return new Config(false, "");
        }
        if (!(raw instanceof Map<?, ?> m)) {
            return new Config(false, "");
        }
        Object en = m.get("enabled");
        boolean enabled = en instanceof Boolean b && b;
        if (!enabled) {
            return new Config(false, "");
        }
        Object rootObj = m.get("datapackRoot");
        String root = rootObj instanceof String s && !s.isBlank()
                ? s
                : "data/" + manifest.id() + "/vida";
        return new Config(true, root);
    }

    private record Config(boolean enabled, String datapackRoot) {}
}
