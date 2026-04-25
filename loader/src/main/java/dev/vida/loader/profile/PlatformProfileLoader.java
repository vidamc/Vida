/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.profile;

import dev.vida.core.ApiStatus;
import dev.vida.loader.BootOptions;
import dev.vida.manifest.json.JsonReader;
import dev.vida.manifest.json.JsonToken;
import dev.vida.manifest.json.VidaJson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Loads {@code profile.json} from the loader classpath under
 * {@code META-INF/vida/platform-profiles/&lt;profileId&gt;/profile.json}.
 *
 * <p>Resolution order for the profile id matches the Ver Profile Adapter plan:
 * {@link BootOptions#platformProfileId()} first, then system property {@code vida.platformProfile}.
 */
@ApiStatus.Preview("loader")
public final class PlatformProfileLoader {

    static final String RESOURCE_ROOT = "META-INF/vida/platform-profiles/";
    static final String PROFILE_FILE = "profile.json";

    private PlatformProfileLoader() {}

    /**
     * Result of resolving an optional explicit profile request.
     *
     * @param descriptor       empty when no profile was requested or needed
     * @param failureMessage   present when a profile id was requested but could not be loaded
     */
    public record ResolveResult(Optional<PlatformProfileDescriptor> descriptor,
            Optional<String> failureMessage) {

        public static ResolveResult none() {
            return new ResolveResult(Optional.empty(), Optional.empty());
        }

        public static ResolveResult ok(PlatformProfileDescriptor d) {
            return new ResolveResult(Optional.of(d), Optional.empty());
        }

        public static ResolveResult failure(String message) {
            return new ResolveResult(Optional.empty(), Optional.of(message));
        }
    }

    /**
     * Resolves {@link BootOptions} + {@code vida.platformProfile} into a descriptor.
     */
    public static ResolveResult resolve(BootOptions options) {
        Optional<String> id = options.platformProfileId();
        if (id.isEmpty()) {
            String sys = System.getProperty("vida.platformProfile");
            if (sys != null && !sys.isBlank()) {
                id = Optional.of(sys.trim());
            }
        }
        if (id.isEmpty()) {
            return ResolveResult.none();
        }
        String profileId = id.get();
        try {
            String json = readProfileJson(profileId);
            PlatformProfileDescriptor d = parseProfileJson(json);
            if (!d.profileId().equals(profileId)) {
                return ResolveResult.failure("profile id mismatch: requested '" + profileId
                        + "' but profile.json declares '" + d.profileId() + "'");
            }
            return ResolveResult.ok(d);
        } catch (RuntimeException | IOException ex) {
            return ResolveResult.failure("failed to load platform profile '" + profileId + "': "
                    + ex.getMessage());
        }
    }

    static String readProfileJson(String profileId) throws IOException {
        String path = RESOURCE_ROOT + normalizeProfilePath(profileId) + "/" + PROFILE_FILE;
        ClassLoader cl = PlatformProfileLoader.class.getClassLoader();
        InputStream in = cl.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("classpath resource not found: " + path);
        }
        try (in; BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(java.util.stream.Collectors.joining("\n"));
        }
    }

    /** Normalizes profile id segments for resource paths ({@code ..} rejected). */
    static String normalizeProfilePath(String profileId) {
        String trimmed = profileId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("profile id must not be blank");
        }
        if (trimmed.contains("..")) {
            throw new IllegalArgumentException("invalid profile id");
        }
        String unix = trimmed.replace('\\', '/');
        while (unix.startsWith("/")) {
            unix = unix.substring(1);
        }
        return unix;
    }

    /** Parses profile JSON text (visible for tests). */
    public static PlatformProfileDescriptor parseProfileJson(String json) {
        try (JsonReader r = VidaJson.reader(json)) {
            return parseProfileObject(r);
        }
    }

    private static PlatformProfileDescriptor parseProfileObject(JsonReader r) {
        r.beginObject();
        String profileId = null;
        String gameVersion = null;
        PlatformGeneration generation = null;
        PlatformMappingsStrategy strategy = null;
        Optional<String> classpathResource = Optional.empty();
        Optional<String> platformBridge = Optional.empty();
        Optional<String> clientSha = Optional.empty();
        Optional<List<String>> morphBundle = Optional.empty();
        Optional<String> mappingMode = Optional.empty();
        Optional<Integer> minJava = Optional.empty();
        Optional<Integer> recJava = Optional.empty();
        Optional<Integer> dataPack = Optional.empty();
        Optional<Integer> resourcePack = Optional.empty();
        while (r.hasNext()) {
            String name = r.nextName();
            switch (name) {
                case "profileId" -> profileId = r.nextString();
                case "gameVersion" -> gameVersion = r.nextString();
                case "generation" -> generation = PlatformGeneration.parse(r.nextString());
                case "platformBridge" -> platformBridge = Optional.of(r.nextString());
                case "mappingMode" -> mappingMode = Optional.of(r.nextString());
                case "morphBundle" -> morphBundle = Optional.of(parseStringArray(r));
                case "mappings" -> {
                    ParsedMappings m = parseMappingsObject(r);
                    strategy = m.strategy();
                    classpathResource = m.classpathResource();
                }
                case "clientJar" -> clientSha = parseClientJarSha(r);
                case "minimumJavaVersion" -> minJava = Optional.of(readPositiveInt(r, name));
                case "recommendedJavaVersion" -> recJava = Optional.of(readPositiveInt(r, name));
                case "dataPackFormat" -> dataPack = Optional.of(readPositiveInt(r, name));
                case "resourcePackFormat" -> resourcePack = Optional.of(readPositiveInt(r, name));
                default -> r.skipValue();
            }
        }
        r.endObject();
        if (profileId == null || gameVersion == null || generation == null || strategy == null) {
            throw new IllegalArgumentException("profile.json missing required fields");
        }
        return new PlatformProfileDescriptor(
                profileId,
                gameVersion,
                generation,
                strategy,
                classpathResource,
                platformBridge,
                clientSha,
                morphBundle,
                mappingMode,
                minJava,
                recJava,
                dataPack,
                resourcePack);
    }

    private static int readPositiveInt(JsonReader r, String field) {
        return switch (r.peek()) {
            case NUMBER -> {
                int v = r.nextInt();
                if (v < 0) {
                    throw new IllegalArgumentException(field + " must be >= 0");
                }
                yield v;
            }
            case STRING -> {
                String s = r.nextString().trim();
                try {
                    yield Integer.parseInt(s);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(field + " must be an integer");
                }
            }
            default -> throw new IllegalArgumentException(field + " must be a number");
        };
    }

    private static List<String> parseStringArray(JsonReader r) {
        r.beginArray();
        List<String> out = new ArrayList<>();
        while (r.hasNext()) {
            out.add(r.nextString());
        }
        r.endArray();
        return List.copyOf(out);
    }

    private record ParsedMappings(PlatformMappingsStrategy strategy,
            Optional<String> classpathResource) {}

    private static ParsedMappings parseMappingsObject(JsonReader r) {
        r.beginObject();
        PlatformMappingsStrategy strategy = null;
        Optional<String> classpathResource = Optional.empty();
        while (r.hasNext()) {
            String name = r.nextName();
            switch (name) {
                case "strategy" -> strategy = parseStrategy(r.nextString());
                case "classpathResource" -> classpathResource = Optional.of(r.nextString());
                default -> r.skipValue();
            }
        }
        r.endObject();
        if (strategy == null) {
            throw new IllegalArgumentException("mappings.strategy missing");
        }
        return new ParsedMappings(strategy, classpathResource);
    }

    private static PlatformMappingsStrategy parseStrategy(String raw) {
        String u = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return PlatformMappingsStrategy.valueOf(u);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unknown mappings.strategy: " + raw);
        }
    }

    private static Optional<String> parseClientJarSha(JsonReader r) {
        if (r.peek() == JsonToken.NULL) {
            r.nextNull();
            return Optional.empty();
        }
        r.beginObject();
        Optional<String> sha = Optional.empty();
        while (r.hasNext()) {
            String name = r.nextName();
            if ("sha256".equals(name)) {
                if (r.peek() == JsonToken.NULL) {
                    r.nextNull();
                } else {
                    sha = Optional.of(r.nextString());
                }
            } else {
                r.skipValue();
            }
        }
        r.endObject();
        return sha;
    }
}
