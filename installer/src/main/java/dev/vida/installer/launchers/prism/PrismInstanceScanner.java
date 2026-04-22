/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сканер instance'ов в каталоге Prism/MultiMC.
 *
 * <p>Правило обнаружения (совместимо с launcher'ом): подкаталог {@code instances/}
 * считается instance'ом тогда и только тогда, когда содержит файл
 * {@code instance.cfg} — именно это поведение {@code QFileSystemWatcher} в
 * исходниках Prism.
 */
public final class PrismInstanceScanner {

    private PrismInstanceScanner() {}

    /**
     * Перечисляет все instance'ы в {@code <dataDir>/instances/}.
     *
     * @param dataDir корень data-dir лаунчера (Prism data-dir или root MultiMC)
     */
    public static List<InstanceRef> list(Path dataDir) throws IOException {
        Path instancesDir = dataDir.resolve("instances");
        if (!Files.isDirectory(instancesDir)) return List.of();

        List<InstanceRef> refs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(instancesDir, Files::isDirectory)) {
            for (Path d : ds) {
                Path cfg = d.resolve("instance.cfg");
                if (!Files.isRegularFile(cfg)) continue;

                String id = d.getFileName().toString();
                Map<String, String> ini = readIni(cfg);
                String name = ini.getOrDefault("name", id);

                McInfo mc = readMcPack(d.resolve("mmc-pack.json"));
                refs.add(new InstanceRef(
                        id, name, d,
                        mc.minecraftVersion,
                        mc.loader,
                        mc.loaderVersion));
            }
        }
        return refs;
    }

    // ------------------------------------------------------------------
    //  INI parser (лёгкий; Prism/MultiMC пишут плоский key=value)
    // ------------------------------------------------------------------

    static Map<String, String> readIni(Path file) throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")
                    || line.startsWith("[")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            m.put(key, value);
        }
        return m;
    }

    // ------------------------------------------------------------------
    //  mmc-pack.json parser — только то, что нужно нам
    // ------------------------------------------------------------------

    private record McInfo(String minecraftVersion,
                          Optional<String> loader,
                          Optional<String> loaderVersion) {}

    @SuppressWarnings("unchecked")
    private static McInfo readMcPack(Path packJson) {
        if (!Files.isReadable(packJson)) return new McInfo("", Optional.empty(), Optional.empty());
        try {
            Object tree = JsonTree.parse(Files.readString(packJson, StandardCharsets.UTF_8));
            if (!(tree instanceof Map<?, ?> m)) return empty();
            Object comps = ((Map<String, Object>) m).get("components");
            if (!(comps instanceof List<?> list)) return empty();

            String mc = "";
            Optional<String> loader = Optional.empty();
            Optional<String> loaderVer = Optional.empty();
            for (Object c : list) {
                if (!(c instanceof Map<?, ?> cm)) continue;
                Object uid = cm.get("uid");
                Object ver = cm.get("version");
                if (!(uid instanceof String uids) || !(ver instanceof String vers)) continue;
                switch (uids) {
                    case "net.minecraft" -> mc = vers;
                    case "net.fabricmc.fabric-loader" -> {
                        loader = Optional.of("fabric");
                        loaderVer = Optional.of(vers);
                    }
                    case "org.quiltmc.quilt-loader" -> {
                        loader = Optional.of("quilt");
                        loaderVer = Optional.of(vers);
                    }
                    case "net.minecraftforge" -> {
                        loader = Optional.of("forge");
                        loaderVer = Optional.of(vers);
                    }
                    case "net.neoforged" -> {
                        loader = Optional.of("neoforge");
                        loaderVer = Optional.of(vers);
                    }
                    default -> { /* ignore */ }
                }
            }
            return new McInfo(mc, loader, loaderVer);
        } catch (IOException | RuntimeException e) {
            return empty();
        }
    }

    private static McInfo empty() {
        return new McInfo("", Optional.empty(), Optional.empty());
    }
}
