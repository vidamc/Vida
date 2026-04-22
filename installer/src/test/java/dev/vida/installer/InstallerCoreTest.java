/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Интеграционный тест ядра инсталлятора.
 *
 * <p>Работает на реальном «вшитом» loader.jar, который попадает в classpath
 * теста через main-resources из {@code :embedLoader}.
 */
final class InstallerCoreTest {

    @Test
    void embedded_loader_resource_is_present() {
        var stream = InstallerCore.class.getResourceAsStream(
                InstallerCore.EMBEDDED_LOADER_RESOURCE);
        assertThat(stream)
                .as("embedded loader must be on test classpath (check :embedLoader wiring)")
                .isNotNull();
    }

    @Test
    void full_install_produces_minecraft_profile_layout(@TempDir Path dir) throws IOException {
        InstallOptions opt = InstallOptions.builder()
                .installDir(dir)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();

        List<String> log = new ArrayList<>();
        InstallReport rep = new InstallerCore(log::add).install(opt);
        assertThat(rep.isOk()).as("errors: %s", rep.errors()).isTrue();

        // 1) libraries/ в Maven-раскладке.
        Path libJar = dir.resolve("libraries/dev/vida/vida-loader/0.1.0/vida-loader-0.1.0.jar");
        assertThat(libJar).exists();
        assertThat(Files.size(libJar)).isEqualTo(rep.loaderBytes());

        // 2) versions/<id>/<id>.json и пустой <id>.jar.
        Path verDir = dir.resolve("versions/vida-1.21.1-0.1.0");
        Path verJson = verDir.resolve("vida-1.21.1-0.1.0.json");
        Path verJar  = verDir.resolve("vida-1.21.1-0.1.0.jar");
        assertThat(verJson).exists();
        assertThat(verJar).exists();

        // 3) launcher_profiles.json создан с нашим профилем.
        Path launcher = dir.resolve("launcher_profiles.json");
        assertThat(launcher).exists();

        // 4) mods/ и служебные каталоги.
        assertThat(dir.resolve("mods")).isDirectory();
        assertThat(dir.resolve("vida")).isDirectory();
        assertThat(dir.resolve("vida/config")).isDirectory();
        assertThat(dir.resolve("vida/logs")).isDirectory();
        assertThat(dir.resolve("vida/install.json")).exists();

        // 5) Standalone-скрипт находится в vida/ (не засоряем корень mc-dir).
        Path script = InstallerCore.isWindows()
                ? dir.resolve("vida/vida.bat")
                : dir.resolve("vida/vida.sh");
        assertThat(script).exists();
        assertThat(Files.readString(script))
                .contains("-javaagent").contains("1.21.1").contains("0.1.0");

        assertThat(log).anyMatch(m -> m.contains("Extracting loader"));
        assertThat(log).anyMatch(m -> m.contains("version JSON"));
        assertThat(log).anyMatch(m -> m.contains("launcher_profiles"));
        assertThat(log).anyMatch(m -> m.contains("Install complete"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void version_json_has_expected_shape(@TempDir Path dir) throws IOException {
        new InstallerCore().install(InstallOptions.builder()
                .installDir(dir).loaderVersion("0.2.0").minecraftVersion("1.21.1").build());

        String body = Files.readString(dir.resolve("versions/vida-1.21.1-0.2.0/vida-1.21.1-0.2.0.json"));
        Map<String, Object> tree = (Map<String, Object>) JsonTree.parse(body);

        assertThat(tree).containsEntry("id", "vida-1.21.1-0.2.0");
        assertThat(tree).containsEntry("inheritsFrom", "1.21.1");
        assertThat(tree).containsEntry("type", "release");

        Map<String, Object> arguments = (Map<String, Object>) tree.get("arguments");
        List<Object> jvm = (List<Object>) arguments.get("jvm");
        assertThat(jvm).anyMatch(s -> String.valueOf(s)
                .contains("-javaagent:${library_directory}/dev/vida/vida-loader/0.2.0/"));
        assertThat(jvm).anyMatch(s -> String.valueOf(s).startsWith("-Dvida.mods="));

        List<Object> libs = (List<Object>) tree.get("libraries");
        assertThat(libs).hasSize(1);
        Map<String, Object> lib = (Map<String, Object>) libs.get(0);
        assertThat(lib).containsEntry("name", "dev.vida:vida-loader:0.2.0");
        Map<String, Object> dl = (Map<String, Object>) lib.get("downloads");
        Map<String, Object> art = (Map<String, Object>) dl.get("artifact");
        assertThat(art).containsEntry("path", "dev/vida/vida-loader/0.2.0/vida-loader-0.2.0.jar");
        assertThat(art.get("sha1")).isInstanceOf(String.class);
        assertThat(((String) art.get("sha1"))).hasSize(40);
        assertThat(art.get("size")).isInstanceOf(Long.class);
        assertThat(((Long) art.get("size"))).isPositive();
    }

    @Test
    void empty_version_jar_is_a_valid_jar(@TempDir Path dir) throws IOException {
        new InstallerCore().install(InstallOptions.builder()
                .installDir(dir).loaderVersion("0.1.0").build());

        Path marker = dir.resolve("versions/vida-1.21.1-0.1.0/vida-1.21.1-0.1.0.jar");
        try (JarInputStream jin = new JarInputStream(Files.newInputStream(marker))) {
            assertThat(jin.getManifest()).isNotNull();
            // Единственная запись-либо-отсутствие содержимого — оба варианта ок,
            // главное — это валидный jar.
            java.util.jar.JarEntry e = jin.getNextJarEntry();
            // Не бросает и не зависает — достаточно.
            if (e != null) {
                assertThat(e.getName()).doesNotContain("..");
            }
        }
    }

    @Test
    void dry_run_writes_nothing(@TempDir Path dir) {
        InstallOptions opt = InstallOptions.builder()
                .installDir(dir).loaderVersion("0.1.0").dryRun(true).build();
        InstallReport rep = new InstallerCore().install(opt);

        assertThat(rep.isOk()).isTrue();
        assertThat(rep.loaderBytes()).isGreaterThan(0);
        assertThat(dir.resolve("libraries")).doesNotExist();
        assertThat(dir.resolve("versions")).doesNotExist();
        assertThat(dir.resolve("launcher_profiles.json")).doesNotExist();
        assertThat(dir.resolve("vida")).doesNotExist();
    }

    @Test
    void reinstall_without_overwrite_fails(@TempDir Path dir) {
        var opt = InstallOptions.builder().installDir(dir).loaderVersion("0.1.0").build();
        var r1 = new InstallerCore().install(opt);
        assertThat(r1.isOk()).as("initial install: %s", r1.errors()).isTrue();

        var r2 = new InstallerCore().install(opt);
        assertThat(r2.isOk()).isFalse();
        assertThat(r2.errors())
                .anyMatch(e -> e.toLowerCase().contains("already installed")
                        || e.toLowerCase().contains("already exists"));
    }

    @Test
    void reinstall_with_overwrite_succeeds(@TempDir Path dir) {
        var opt1 = InstallOptions.builder().installDir(dir).loaderVersion("0.1.0").build();
        assertThat(new InstallerCore().install(opt1).isOk()).isTrue();

        var opt2 = InstallOptions.builder().installDir(dir).loaderVersion("0.1.0")
                .overwrite(true).build();
        var r = new InstallerCore().install(opt2);
        assertThat(r.isOk()).as("overwrite: %s", r.errors()).isTrue();
    }

    @Test
    void install_skips_launcher_profile_when_disabled(@TempDir Path dir) {
        var opt = InstallOptions.builder()
                .installDir(dir).loaderVersion("0.1.0")
                .createLauncherProfile(false)
                .build();

        assertThat(new InstallerCore().install(opt).isOk()).isTrue();
        assertThat(dir.resolve("launcher_profiles.json")).doesNotExist();
        // Но версия и библиотека всё равно созданы.
        assertThat(dir.resolve("versions/vida-1.21.1-0.1.0/vida-1.21.1-0.1.0.json")).exists();
        assertThat(dir.resolve("libraries/dev/vida/vida-loader/0.1.0/vida-loader-0.1.0.jar")).exists();
    }

    @Test
    void install_skips_launch_script_when_disabled(@TempDir Path dir) {
        var opt = InstallOptions.builder()
                .installDir(dir).loaderVersion("0.1.0")
                .createLaunchScript(false)
                .build();

        assertThat(new InstallerCore().install(opt).isOk()).isTrue();
        assertThat(dir.resolve("vida/vida.bat")).doesNotExist();
        assertThat(dir.resolve("vida/vida.sh")).doesNotExist();
    }

    @Test
    void install_fails_when_vida_path_is_an_existing_file(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("vida"), "boom");
        var opt = InstallOptions.builder().installDir(dir).loaderVersion("0.1.0").build();

        var r = new InstallerCore().install(opt);
        assertThat(r.isOk()).isFalse();
        assertThat(r.errors()).anyMatch(e -> e.contains("exists"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void install_json_records_all_artifacts(@TempDir Path dir) throws IOException {
        var opt = InstallOptions.builder().installDir(dir).loaderVersion("0.1.0").build();
        var rep = new InstallerCore().install(opt);
        assertThat(rep.isOk()).isTrue();

        String body = Files.readString(dir.resolve("vida/install.json"));
        Map<String, Object> m = (Map<String, Object>) JsonTree.parse(body);
        assertThat(m).containsEntry("schema", (long) InstallerCore.INSTALL_MANIFEST_SCHEMA);
        assertThat(m).containsEntry("profileId", "vida-1.21.1-0.1.0");
        Map<String, Object> loader = (Map<String, Object>) m.get("loader");
        assertThat(loader.get("sha1")).isInstanceOf(String.class);
        List<Object> files = (List<Object>) m.get("files");
        assertThat(files).anyMatch(s -> String.valueOf(s)
                .contains("libraries/dev/vida/vida-loader/0.1.0/vida-loader-0.1.0.jar"));
        assertThat(files).anyMatch(s -> String.valueOf(s)
                .contains("versions/vida-1.21.1-0.1.0/vida-1.21.1-0.1.0.json"));
        assertThat(files).anyMatch(s -> String.valueOf(s)
                .contains("launcher_profiles.json"));
        assertThat(files).anyMatch(s -> String.valueOf(s)
                .contains("vida/install.json"));
    }
}
