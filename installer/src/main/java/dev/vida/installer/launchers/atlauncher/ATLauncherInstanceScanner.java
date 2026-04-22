/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.atlauncher;

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
 * Сканер ATLauncher instance'ов.
 *
 * <p>ATLauncher хранит каждый instance как {@code <BASE_DIR>/instances/<SafeName>/},
 * внутри — {@code instance.json} (Gson-сериализованный блок).
 *
 * <p>Интересующие поля (см. research report):
 * <ul>
 *   <li>{@code id} — версия Minecraft (в поле {@code MinecraftVersion.id}).</li>
 *   <li>{@code launcher.name} — display name.</li>
 *   <li>{@code launcher.loaderVersion.type} — {@code "Fabric"}/{@code "Forge"}/... (case-sensitive).</li>
 *   <li>{@code launcher.loaderVersion.version} — версия модлоадера.</li>
 * </ul>
 */
public final class ATLauncherInstanceScanner {

    private ATLauncherInstanceScanner() {}

    public static List<InstanceRef> list(Path baseDir) throws IOException {
        Path instances = baseDir.resolve("instances");
        if (!Files.isDirectory(instances)) return List.of();

        List<InstanceRef> refs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(instances, Files::isDirectory)) {
            for (Path d : ds) {
                Path js = d.resolve("instance.json");
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
            String mcVersion = stringOrEmpty(root.get("id"));
            String displayName = id;
            Optional<String> loader = Optional.empty();
            Optional<String> loaderVer = Optional.empty();

            Object launcher = root.get("launcher");
            if (launcher instanceof Map<?, ?> lm) {
                Map<String, Object> l = (Map<String, Object>) lm;
                Object nm = l.get("name");
                if (nm instanceof String s && !s.isBlank()) displayName = s;

                Object lv = l.get("loaderVersion");
                if (lv instanceof Map<?, ?> lvm) {
                    Map<String, Object> lvMap = (Map<String, Object>) lvm;
                    Object type = lvMap.get("type");
                    Object ver = lvMap.get("version");
                    if (type instanceof String t) {
                        loader = Optional.of(t.toLowerCase(Locale.ROOT));
                    }
                    if (ver instanceof String v) {
                        loaderVer = Optional.of(v);
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
