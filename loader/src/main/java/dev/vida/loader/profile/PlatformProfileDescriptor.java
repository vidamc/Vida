/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.profile;

import dev.vida.core.ApiStatus;
import java.util.List;
import java.util.Optional;

/**
 * Frozen view of {@code profile.json} for one Minecraft drop / generation pair.
 *
 * @param classpathMappingsResource required when strategy is {@link PlatformMappingsStrategy#CLASSPATH_PROGUARD}
 * @param morphBundle               when present, only these platform morph classes (FQCN) are registered from the
 *                                  loader jar; when absent, all built-in platform morphs are registered
 */
@ApiStatus.Preview("loader")
public record PlatformProfileDescriptor(
        String profileId,
        String gameVersion,
        PlatformGeneration generation,
        PlatformMappingsStrategy mappingsStrategy,
        Optional<String> classpathMappingsResource,
        Optional<String> platformBridgeClass,
        Optional<String> clientJarSha256Hex,
        Optional<List<String>> morphBundle,
        Optional<String> mappingMode,
        Optional<Integer> minimumJavaVersion,
        Optional<Integer> recommendedJavaVersion,
        Optional<Integer> dataPackFormat,
        Optional<Integer> resourcePackFormat) {

    public PlatformProfileDescriptor {
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("profileId required");
        }
        if (gameVersion == null || gameVersion.isBlank()) {
            throw new IllegalArgumentException("gameVersion required");
        }
        if (generation == null) {
            throw new IllegalArgumentException("generation required");
        }
        if (mappingsStrategy == null) {
            throw new IllegalArgumentException("mappings.strategy required");
        }
        classpathMappingsResource = classpathMappingsResource == null
                ? Optional.empty()
                : classpathMappingsResource.filter(s -> !s.isBlank());
        platformBridgeClass = platformBridgeClass == null
                ? Optional.empty()
                : platformBridgeClass.filter(s -> !s.isBlank());
        clientJarSha256Hex = clientJarSha256Hex == null
                ? Optional.empty()
                : clientJarSha256Hex.filter(s -> !s.isBlank());
        morphBundle = morphBundle == null
                ? Optional.empty()
                : morphBundle.map(List::copyOf);
        mappingMode = mappingMode == null ? Optional.empty() : mappingMode.filter(s -> !s.isBlank());
        minimumJavaVersion = minimumJavaVersion == null ? Optional.empty() : minimumJavaVersion;
        recommendedJavaVersion =
                recommendedJavaVersion == null ? Optional.empty() : recommendedJavaVersion;
        dataPackFormat = dataPackFormat == null ? Optional.empty() : dataPackFormat;
        resourcePackFormat = resourcePackFormat == null ? Optional.empty() : resourcePackFormat;
        if (mappingsStrategy == PlatformMappingsStrategy.CLASSPATH_PROGUARD
                && classpathMappingsResource.isEmpty()) {
            throw new IllegalArgumentException(
                    "mappings.classpathResource required for CLASSPATH_PROGUARD");
        }
        if (morphBundle.isPresent()) {
            List<String> mb = morphBundle.get();
            if (mb.isEmpty()) {
                throw new IllegalArgumentException(
                        "morphBundle, if present, must list at least one morph class");
            }
            for (String s : mb) {
                if (s == null || s.isBlank()) {
                    throw new IllegalArgumentException("morphBundle entries must not be blank");
                }
            }
        }
        minimumJavaVersion.ifPresent(v -> {
            if (v < 8 || v > 999) {
                throw new IllegalArgumentException("minimumJavaVersion out of range");
            }
        });
        recommendedJavaVersion.ifPresent(v -> {
            if (v < 8 || v > 999) {
                throw new IllegalArgumentException("recommendedJavaVersion out of range");
            }
        });
    }
}
