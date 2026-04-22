/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Безопасный патчер {@code launcher_profiles.json}.
 *
 * <p>Правила:
 * <ol>
 *   <li>Если файл существует и валиден — парсится в полное tree-model
 *       через {@link JsonTree}, добавляется/обновляется профиль Vida,
 *       остальные поля/профили/настройки сохраняются <em>как были</em>.</li>
 *   <li>Если файл повреждён или отсутствует — бэкапится (при повреждённости)
 *       и создаётся новый минимальный файл с одним профилем Vida.</li>
 *   <li>Запись идёт через временный файл + атомарный move, чтобы не оставить
 *       лаунчер в «полурабочем» состоянии при сбое ФС.</li>
 * </ol>
 */
public final class LauncherProfilesPatcher {

    private static final Logger LOG = LoggerFactory.getLogger(LauncherProfilesPatcher.class);

    /** Текущая версия схемы {@code launcher_profiles.json}, которую пишет Mojang. */
    public static final long SCHEMA_VERSION = 3L;

    private LauncherProfilesPatcher() {}

    /** Результат патча. */
    public record Result(
            boolean fileExisted,
            boolean profileExisted,
            boolean backupCreated,
            Path backupPath,
            Path target) {
        public Result {
            Objects.requireNonNull(target, "target");
        }
    }

    /**
     * Применяет патч: добавляет (или обновляет) профиль Vida.
     *
     * @param launcherProfilesFile {@code <mc>/launcher_profiles.json}
     * @param layout               описание установки
     * @param now                  время для полей {@code created}/{@code lastUsed}
     * @param dryRun               если {@code true} — ничего не пишет, только считает изменения
     */
    public static Result patch(Path launcherProfilesFile,
                               McLayout layout,
                               Instant now,
                               boolean dryRun) throws IOException {
        Objects.requireNonNull(launcherProfilesFile, "launcherProfilesFile");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(now, "now");

        boolean existed = Files.isRegularFile(launcherProfilesFile);
        boolean backupCreated = false;
        Path backupPath = null;

        Map<String, Object> root;
        if (existed) {
            try {
                String body = Files.readString(launcherProfilesFile, StandardCharsets.UTF_8);
                Object parsed = JsonTree.parse(new StringReader(body));
                if (!(parsed instanceof Map<?, ?> m)) {
                    throw new IOException("launcher_profiles.json root is not a JSON object");
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> asMap = (Map<String, Object>) m;
                root = asMap;
            } catch (Exception parseError) {
                LOG.warn("launcher_profiles.json is unreadable, creating fresh (backing up old)", parseError);
                if (!dryRun) {
                    backupPath = launcherProfilesFile.resolveSibling(
                            "launcher_profiles.json.vida-backup-" + System.currentTimeMillis());
                    Files.copy(launcherProfilesFile, backupPath);
                    backupCreated = true;
                }
                root = freshRoot();
            }
        } else {
            root = freshRoot();
        }

        Map<String, Object> profiles = JsonTree.getOrCreateObject(root, "profiles");
        boolean profileExisted = profiles.containsKey(layout.profileId());
        profiles.put(layout.profileId(), buildProfile(layout, now));

        // Гарантируем version-ключ: некоторые сторонние лаунчеры на него смотрят.
        if (!root.containsKey("version")) root.put("version", SCHEMA_VERSION);

        if (!dryRun) {
            writeAtomically(launcherProfilesFile, JsonTree.write(root));
        }
        return new Result(existed, profileExisted, backupCreated, backupPath, launcherProfilesFile);
    }

    // ================================================================ helpers

    private static Map<String, Object> freshRoot() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("profiles", new LinkedHashMap<String, Object>());
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("crashAssistance", true);
        settings.put("enableAdvanced", false);
        settings.put("enableAnalytics", false);
        settings.put("enableHistorical", false);
        settings.put("enableReleases", true);
        settings.put("enableSnapshots", false);
        settings.put("keepLauncherOpen", false);
        settings.put("profileSorting", "ByLastPlayed");
        settings.put("showGameLog", false);
        settings.put("showMenu", false);
        settings.put("soundOn", false);
        root.put("settings", settings);
        root.put("version", SCHEMA_VERSION);
        return root;
    }

    private static Map<String, Object> buildProfile(McLayout layout, Instant now) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("created", now.toString());
        p.put("icon", "Grass");
        p.put("lastUsed", now.toString());
        p.put("lastVersionId", layout.profileId());
        p.put("name", layout.displayName());
        p.put("type", "custom");
        return p;
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Поиск профиля по id. Если список пуст — {@code null}. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> findProfile(Map<String, Object> root, String id) {
        Object profiles = root.get("profiles");
        if (!(profiles instanceof Map<?, ?> map)) return null;
        Object v = map.get(id);
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }

    /** Для тестов и диагностики: перечень id-шников профилей. */
    @SuppressWarnings("unchecked")
    static List<String> listProfileIds(Map<String, Object> root) {
        Object profiles = root.get("profiles");
        if (!(profiles instanceof Map<?, ?> map)) return List.of();
        return List.copyOf((java.util.Set<String>) map.keySet());
    }
}
