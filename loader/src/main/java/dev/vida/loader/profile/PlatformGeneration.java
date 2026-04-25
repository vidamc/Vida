/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.profile;

import dev.vida.core.ApiStatus;
import java.util.Locale;

/** Logical platform generation label stored in {@code profile.json}. */
@ApiStatus.Preview("loader")
public enum PlatformGeneration {
    /** Semantic 1.21.x game drops (ProGuard client mappings under the launcher tree). */
    LEGACY_121,
    /** Calendar 26.x line — separate mapping rules when supported. */
    CALENDAR_26;

    /**
     * Parses the JSON enum token ({@code LEGACY_121}, …), case-sensitive as in the schema.
     */
    public static PlatformGeneration parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("generation must not be blank");
        }
        try {
            return PlatformGeneration.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown generation '" + raw + "'", ex);
        }
    }

    /** Folder name under {@code platform-profiles/generations/} for this generation. */
    public String folderName() {
        return switch (this) {
            case LEGACY_121 -> "legacy-121";
            case CALENDAR_26 -> "calendar-26";
        };
    }

    /** Lowercase slug for logs and diagnostics. */
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
