/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.manifest.ModManifest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * When a {@linkplain dev.vida.loader.profile.PlatformProfileDescriptor platform profile} is active,
 * mods may declare which profiles their Vifada morphs are compatible with via
 * {@code custom.vida.platformProfileIds} (JSON array of strings). If declared and the active
 * profile id is not listed, morph scanning for that mod jar is skipped.
 *
 * <p>If the active profile is empty or the mod omits the declaration, all morphs are accepted
 * (backward compatible).
 */
@ApiStatus.Internal
public final class ModMorphGate {

    private ModMorphGate() {}

    static boolean allowMorphsFromMod(ModManifest manifest, Optional<String> activeProfileId) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(activeProfileId, "activeProfileId");
        if (activeProfileId.isEmpty()) {
            return true;
        }
        Optional<Set<String>> declared = readDeclaredProfileIds(manifest);
        if (declared.isEmpty()) {
            return true;
        }
        return declared.get().contains(activeProfileId.get());
    }

    @SuppressWarnings("unchecked")
    private static Optional<Set<String>> readDeclaredProfileIds(ModManifest manifest) {
        Object vida = manifest.custom().get("vida");
        if (!(vida instanceof Map<?, ?> vm)) {
            return Optional.empty();
        }
        Object raw = vm.get("platformProfileIds");
        if (raw == null) {
            return Optional.empty();
        }
        if (raw instanceof List<?> list) {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = o.toString().trim();
                if (!s.isEmpty()) {
                    ids.add(s);
                }
            }
            return Optional.of(ids);
        }
        return Optional.empty();
    }
}
