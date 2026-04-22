/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LauncherProfilesPatcherTest {

    private static final Instant T = Instant.parse("2026-04-21T12:00:00Z");

    private static McLayout layout(Path mc) {
        return new McLayout(mc, "1.21.1", "0.5.0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void creates_fresh_file_when_missing(@TempDir Path dir) throws IOException {
        McLayout l = layout(dir);
        var r = LauncherProfilesPatcher.patch(l.launcherProfiles(), l, T, false);

        assertThat(r.fileExisted()).isFalse();
        assertThat(r.profileExisted()).isFalse();
        assertThat(r.backupCreated()).isFalse();

        String body = Files.readString(l.launcherProfiles(), StandardCharsets.UTF_8);
        Map<String, Object> tree = (Map<String, Object>) JsonTree.parse(body);
        assertThat(tree).containsKey("profiles").containsKey("settings").containsKey("version");
        Map<String, Object> profiles = (Map<String, Object>) tree.get("profiles");
        assertThat(profiles).containsKey("vida-1.21.1-0.5.0");
        Map<String, Object> p = (Map<String, Object>) profiles.get("vida-1.21.1-0.5.0");
        assertThat(p).containsEntry("lastVersionId", "vida-1.21.1-0.5.0");
        assertThat(p).containsEntry("name", "Vida 1.21.1 (0.5.0)");
        assertThat(p).containsEntry("type", "custom");
    }

    @Test
    @SuppressWarnings("unchecked")
    void preserves_other_profiles_when_patching_existing(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("launcher_profiles.json");
        String original = """
                {
                  "profiles": {
                    "vanilla-snapshot": { "name": "Snapshot", "type": "latest-snapshot" },
                    "vanilla-release":  { "name": "Latest",   "type": "latest-release",
                      "javaArgs": "-Xmx4G" }
                  },
                  "settings": { "keepLauncherOpen": true, "soundOn": true },
                  "version": 3,
                  "clientToken": "abc-123",
                  "launcherVersion": { "name": "2.7.77" }
                }
                """;
        Files.writeString(file, original, StandardCharsets.UTF_8);

        McLayout l = layout(dir);
        var r = LauncherProfilesPatcher.patch(file, l, T, false);
        assertThat(r.fileExisted()).isTrue();
        assertThat(r.profileExisted()).isFalse();

        Map<String, Object> tree = (Map<String, Object>) JsonTree.parse(Files.readString(file));
        Map<String, Object> profiles = (Map<String, Object>) tree.get("profiles");
        assertThat(profiles)
                .containsKey("vanilla-snapshot")
                .containsKey("vanilla-release")
                .containsKey("vida-1.21.1-0.5.0");

        Map<String, Object> vanillaSnap = (Map<String, Object>) profiles.get("vanilla-snapshot");
        assertThat(vanillaSnap).containsEntry("type", "latest-snapshot");
        Map<String, Object> vanillaRel = (Map<String, Object>) profiles.get("vanilla-release");
        assertThat(vanillaRel).containsEntry("javaArgs", "-Xmx4G");

        Map<String, Object> settings = (Map<String, Object>) tree.get("settings");
        assertThat(settings).containsEntry("keepLauncherOpen", true);

        // Неизвестные поля тоже сохраняются.
        assertThat(tree).containsEntry("clientToken", "abc-123");
        Map<String, Object> lv = (Map<String, Object>) tree.get("launcherVersion");
        assertThat(lv).containsEntry("name", "2.7.77");
    }

    @Test
    @SuppressWarnings("unchecked")
    void updates_existing_vida_profile_without_duplicating(@TempDir Path dir) throws IOException {
        McLayout l = layout(dir);
        // Первый запуск — создаёт.
        LauncherProfilesPatcher.patch(l.launcherProfiles(), l, T, false);
        // Второй запуск — обновляет.
        var r2 = LauncherProfilesPatcher.patch(l.launcherProfiles(), l, T.plusSeconds(60), false);
        assertThat(r2.fileExisted()).isTrue();
        assertThat(r2.profileExisted()).isTrue();

        Map<String, Object> tree = (Map<String, Object>) JsonTree.parse(Files.readString(l.launcherProfiles()));
        Map<String, Object> profiles = (Map<String, Object>) tree.get("profiles");
        // Ровно один профиль с Vida-id.
        long count = profiles.keySet().stream().filter(k -> k.equals("vida-1.21.1-0.5.0")).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void dry_run_does_not_touch_filesystem(@TempDir Path dir) throws IOException {
        McLayout l = layout(dir);
        var r = LauncherProfilesPatcher.patch(l.launcherProfiles(), l, T, true);
        assertThat(r.target()).isEqualTo(l.launcherProfiles());
        assertThat(l.launcherProfiles()).doesNotExist();
    }

    @Test
    @SuppressWarnings("unchecked")
    void corrupted_file_is_backed_up_and_replaced(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("launcher_profiles.json");
        Files.writeString(file, "this is not JSON at all {{{", StandardCharsets.UTF_8);

        McLayout l = layout(dir);
        var r = LauncherProfilesPatcher.patch(file, l, T, false);
        assertThat(r.fileExisted()).isTrue();
        assertThat(r.backupCreated()).isTrue();
        assertThat(r.backupPath()).isNotNull();
        assertThat(r.backupPath()).exists();

        Map<String, Object> tree = (Map<String, Object>) JsonTree.parse(Files.readString(file));
        Map<String, Object> profiles = (Map<String, Object>) tree.get("profiles");
        assertThat(profiles).containsKey("vida-1.21.1-0.5.0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejects_non_object_root(@TempDir Path dir) throws IOException {
        // Массив в корне — это JSON, но не легальный launcher_profiles.
        Path file = dir.resolve("launcher_profiles.json");
        Files.writeString(file, "[1,2,3]", StandardCharsets.UTF_8);

        McLayout l = layout(dir);
        var r = LauncherProfilesPatcher.patch(file, l, T, false);
        // Обрабатывается как «corrupted»: бэкап + fresh.
        assertThat(r.backupCreated()).isTrue();
        Map<String, Object> tree = (Map<String, Object>) JsonTree.parse(Files.readString(file));
        assertThat(LauncherProfilesPatcher.listProfileIds(tree))
                .contains("vida-1.21.1-0.5.0");
    }
}
