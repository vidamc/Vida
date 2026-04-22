/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.curseforge;

import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Сканер CurseForge-инстансов.
 *
 * <p>CurseForge хранит инстансы в {@code <base>/Instances/<name>/} с файлом
 * {@code minecraftinstance.json}. Интересующие поля:
 * <ul>
 *   <li>{@code name} — display name;</li>
 *   <li>{@code gameVersion} — версия Minecraft;</li>
 *   <li>{@code baseModLoader.name} — {@code "forge-47.3.0"} / {@code "fabric-0.16.0"};</li>
 * </ul>
 */
final class CurseForgeInstanceScanner {

    private CurseForgeInstanceScanner() {}

    static List<InstanceRef> list(Path baseDir) throws IOException {
        Path instances = baseDir.resolve("Instances");
        if (!Files.isDirectory(instances)) return List.of();

        List<InstanceRef> refs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(instances, Files::isDirectory)) {
            for (Path d : ds) {
                Path js = d.resolve("minecraftinstance.json");
                if (!Files.isRegularFile(js)) continue;
                InstanceRef ref = readRef(d, js);
                if (ref != null) refs.add(ref);
            }
        }
        return refs;
    }

    @SuppressWarnings("unchecked")
    static InstanceRef readRef(Path instanceDir, Path instanceJson) {
        try {
            Object tree = JsonTree.parse(Files.readString(instanceJson, StandardCharsets.UTF_8));
            if (!(tree instanceof Map<?, ?> map)) return null;
            Map<String, Object> root = (Map<String, Object>) map;

            String id = instanceDir.getFileName().toString();
            String displayName = stringOrEmpty(root.get("name"));
            if (displayName.isBlank()) displayName = id;
            String mcVersion = stringOrEmpty(root.get("gameVersion"));

            Optional<String> loader = Optional.empty();
            Optional<String> loaderVer = Optional.empty();

            Object baseModLoader = root.get("baseModLoader");
            if (baseModLoader instanceof Map<?, ?> bml) {
                Map<String, Object> bmlMap = (Map<String, Object>) bml;
                Object name = bmlMap.get("name");
                if (name instanceof String s && !s.isBlank()) {
                    int dash = s.indexOf('-');
                    if (dash > 0) {
                        loader = Optional.of(s.substring(0, dash).toLowerCase(Locale.ROOT));
                        loaderVer = Optional.of(s.substring(dash + 1));
                    } else {
                        loader = Optional.of(s.toLowerCase(Locale.ROOT));
                    }
                }
            }

            return new InstanceRef(id, displayName, instanceDir, mcVersion, loader, loaderVer);
        } catch (IOException | RuntimeException ignore) {
            return null;
        }
    }

    private static String stringOrEmpty(Object o) {
        return o instanceof String s ? s : "";
    }
}
