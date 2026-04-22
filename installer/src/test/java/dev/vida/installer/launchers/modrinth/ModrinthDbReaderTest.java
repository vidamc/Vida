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

        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "pack-1", "-javaagent:/path/loader.jar");
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
        }
    }

    @Test
    void patches_java_args_replaces_existing_agent() throws Exception {
        createDb(
                "INSERT INTO profiles VALUES('pack-1','P','1.21.1','fabric','0.16.0',"
                        + "'-Xmx4G -javaagent:/old/agent.jar -Dfoo=bar')"
        );

        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "pack-1", "-javaagent:/new/loader.jar");
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

        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "pack-1", "-javaagent:/path/loader.jar");
        assertThat(ok).isTrue();

        Path db = tmp.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             var rs = conn.createStatement()
                     .executeQuery("SELECT java_args FROM profiles WHERE path='pack-1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("java_args")).isEqualTo("-javaagent:/path/loader.jar");
        }
    }

    @Test
    void patch_returns_false_for_missing_profile() throws Exception {
        createDb();
        boolean ok = ModrinthDbReader.patchJavaArgs(tmp, "nonexistent", "-javaagent:/x.jar");
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
}
