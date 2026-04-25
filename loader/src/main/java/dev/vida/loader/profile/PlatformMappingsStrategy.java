/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.profile;

import dev.vida.core.ApiStatus;

/** Where Cartografía loads ProGuard client mappings from for this profile. */
@ApiStatus.Preview("loader")
public enum PlatformMappingsStrategy {
    /**
     * Resolve {@code client_mappings.txt} (and fallbacks) under
     * {@code .minecraft/versions/<minecraftVersion>/} — legacy behaviour.
     */
    GAME_DIR_PROGUARD,
    /** Load mapping text from a classpath resource ({@link PlatformProfileDescriptor#classpathMappingsResource}). */
    CLASSPATH_PROGUARD
}
