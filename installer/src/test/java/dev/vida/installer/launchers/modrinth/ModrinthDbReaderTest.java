/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.modrinth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.installer.launchers.InstanceRef;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ModrinthDbReaderTest {

    @TempDir Path tmp;

    private void createDb(String... profileInserts) throws SQLException {
        Path db = tmp.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE profiles (
                        path TEXT PRIMARY KEY,
                        name TEXT,
                        game_version TEXT,
                        loader TEXT,
                        loader_version TEXT,
                        java_args TEXT
                    )
                    """);
            for (String insert : profileInserts) {
                st.execute(insert);
            }
        }
    }

    @Test
    void reads_profiles_from_db() throws Exception {
        createDb(
                "INSERT INTO profiles VALUES('pack-1','My Pack','1.21.1','fabric','0.16.0','')",
                "INSERT INTO profiles VALUES('vanilla','Vanilla MC','1.21.1','vanilla',NULL,NULL)"
        );

        List<InstanceRef> refs = ModrinthDbReader.listProfiles(tmp);

        assertThat(refs).hasSize(2);
        InstanceRef first = refs.getFirst();
        assertThat(first.displayName()).isEqualTo("My Pack");
        assertThat(first.minecraftVersion()).isEqualTo("1.21.1");
        assertThat(first.loader()).hasValue("fabric");
        assertThat(first.loaderVersion()).hasValue("0.16.0");
    }

    @Test
    void returns_empty_when_no_db() throws IOException {
        assertThat(ModrinthDbReader.listProfiles(tmp)).isEmpty();
    }

    @Test
    void patches_java_args_appends() throws Exception {
        createDb(
                "INSERT INTO profiles VALUES('pack-1','My Pack','1.21.1','fabric','0.16.0','-Xmx4G')"
        );

        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "pack-1", "-javaagent:/path/loader.jar",
                "1.21.1", "0.5.0");
        assertThat(ok).isTrue();

        List<InstanceRef> refs = ModrinthDbReader.listProfiles(tmp);
        assertThat(refs).hasSize(1);

        Path db = tmp.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             var rs = conn.createStatement()
                     .executeQuery("SELECT java_args FROM profiles WHERE path='pack-1'")) {
            assertThat(rs.next()).isTrue();
            String args = rs.getString("java_args");
            assertThat(args).contains("-Xmx4G");
            assertThat(args).contains("-javaagent:/path/loader.jar");
            assertThat(args).contains("-Dvida.minecraftVersion=1.21.1");
        }
    }

    @Test
    void patches_java_args_replaces_existing_agent() throws Exception {
        createDb(
                "INSERT INTO profiles VALUES('pack-1','P','1.21.1','fabric','0.16.0',"
                        + "'-Xmx4G -javaagent:/old/agent.jar -Dfoo=bar')"
        );

        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "pack-1", "-javaagent:/new/loader.jar",
                "1.21.1", "0.5.0");
        assertThat(ok).isTrue();

        Path db = tmp.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             var rs = conn.createStatement()
                     .executeQuery("SELECT java_args FROM profiles WHERE path='pack-1'")) {
            assertThat(rs.next()).isTrue();
            String args = rs.getString("java_args");
            assertThat(args).contains("-javaagent:/new/loader.jar");
            assertThat(args).doesNotContain("/old/agent.jar");
            assertThat(args).contains("-Xmx4G").contains("-Dfoo=bar");
        }
    }

    @Test
    void patches_java_args_sets_when_null() throws Exception {
        createDb(
                "INSERT INTO profiles VALUES('pack-1','P','1.21.1','fabric','0.16.0',NULL)"
        );

        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "pack-1", "-javaagent:/path/loader.jar",
                "1.21.1", "0.5.0");
        assertThat(ok).isTrue();

        Path db = tmp.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             var rs = conn.createStatement()
                     .executeQuery("SELECT java_args FROM profiles WHERE path='pack-1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("java_args")).contains("-javaagent:/path/loader.jar")
                    .contains("-Dvida.minecraftVersion=1.21.1");
        }
    }

    @Test
    void patch_returns_false_for_missing_profile() throws Exception {
        createDb();
        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "nonexistent", "-javaagent:/x.jar",
                "1.21.1", "0.5.0");
        assertThat(ok).isFalse();
    }

    @Test
    void instance_path_resolved_under_profiles_dir() throws Exception {
        createDb(
                "INSERT INTO profiles VALUES('my-instance','Test','1.21.1','fabric','0.16.0',NULL)"
        );
        Files.createDirectories(tmp.resolve("profiles").resolve("my-instance"));

        List<InstanceRef> refs = ModrinthDbReader.listProfiles(tmp);
        assertThat(refs).hasSize(1);
        assertThat(refs.getFirst().instancePath().toString())
                .contains("profiles")
                .contains("my-instance");
    }

    /**
     * Схема как у Theseus ({@code modrinth/code}): {@code override_extra_launch_args}
     * — JSON-массив строк, а не {@code java_args}.
     */
    @Test
    void patches_override_extra_launch_args_json_array() throws Exception {
        Path db = tmp.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE profiles (
                      path TEXT PRIMARY KEY,
                      install_stage TEXT NOT NULL DEFAULT 'installed',
                      name TEXT NOT NULL,
                      icon_path TEXT,
                      game_version TEXT NOT NULL,
                      mod_loader TEXT NOT NULL DEFAULT 'fabric',
                      mod_loader_version TEXT,
                      groups TEXT NOT NULL DEFAULT '[]',
                      linked_project_id TEXT,
                      linked_version_id TEXT,
                      locked INTEGER,
                      created INTEGER NOT NULL DEFAULT 0,
                      modified INTEGER NOT NULL DEFAULT 0,
                      last_played INTEGER,
                      submitted_time_played INTEGER NOT NULL DEFAULT 0,
                      recent_time_played INTEGER NOT NULL DEFAULT 0,
                      override_java_path TEXT,
                      override_extra_launch_args TEXT NOT NULL DEFAULT '[]',
                      override_custom_env_vars TEXT NOT NULL DEFAULT '[]',
                      override_mc_memory_max INTEGER,
                      override_mc_force_fullscreen INTEGER,
                      override_mc_game_resolution_x INTEGER,
                      override_mc_game_resolution_y INTEGER,
                      override_hook_pre_launch TEXT,
                      override_hook_wrapper TEXT,
                      override_hook_post_exit TEXT
                    )
                    """);
            st.execute(
                    "INSERT INTO profiles(path,name,game_version,mod_loader,mod_loader_version,"
                            + "override_extra_launch_args) "
                            + "VALUES('pack-1','P','1.21.1','fabric','0.16.0','[\"-Xmx4G\"]')");
        }

        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "pack-1",
                "-javaagent:C:/data/profiles/pack-1/vida/vida-loader-1.jar",
                "1.21.1", "1.0.0");
        assertThat(ok).isTrue();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + db.toAbsolutePath());
             var rs = conn.createStatement().executeQuery(
                     "SELECT override_extra_launch_args FROM profiles WHERE path='pack-1'")) {
            assertThat(rs.next()).isTrue();
            String json = rs.getString(1);
            assertThat(json).contains("-javaagent:C:/data/profiles/pack-1/vida/vida-loader-1.jar");
            assertThat(json).contains("-Xmx4G");
            assertThat(json).contains("-Dvida.minecraftVersion=1.21.1");
        }
    }
}
