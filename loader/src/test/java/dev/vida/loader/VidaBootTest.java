/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.*;

import dev.vida.base.LatidoGlobal;
import dev.vida.platform.VanillaBridge;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Сквозной бутстрап: сгенерировать моды на диск, запустить {@link VidaBoot},
 * убедиться, что indexed morphs появились и classloaders построены.
 */
final class VidaBootTest {

    @BeforeEach
    void reset() {
        VidaRuntime.resetForTests();
        LatidoGlobal.resetForTests();
        VanillaBridge.resetForTests();
    }

    @AfterEach
    void cleanup() {
        VidaRuntime.resetForTests();
        LatidoGlobal.resetForTests();
        VanillaBridge.resetForTests();
    }

    @Test
    void boots_with_single_mod_and_morph(@TempDir Path root) throws Exception {
        Path mods = root.resolve("mods");
        java.nio.file.Files.createDirectories(mods);

        byte[] morph = TestSupport.buildHeadInjectMorph("demo/FooMorph", "demo.Foo");
        TestSupport.buildModJar(mods.resolve("demo.jar"), "demo", "1.0.0",
                morph, "demo/FooMorph");

        BootOptions opts = BootOptions.builder()
                .modsDir(mods)
                .build();

        BootReport report = VidaBoot.boot(opts);
        assertThat(report.errors()).as("errors=%s", report.errors()).isEmpty();
        assertThat(report.isOk()).isTrue();

        VidaEnvironment env = report.environment();
        assertThat(env.resolvedMods()).hasSize(1);
        assertThat(env.resolvedMods().get(0).id()).isEqualTo("demo");
        // Мод-морф + два платформенных (MinecraftTickMorph, GuiRenderMorph),
        // которые BootSequence регистрирует автоматически.
        assertThat(env.morphs().totalMorphs()).isEqualTo(3);
        assertThat(env.morphs().forTarget("demo/Foo")).hasSize(1);
        assertThat(env.morphs().forTarget("net/minecraft/client/Minecraft")).hasSize(1);
        assertThat(env.morphs().forTarget("net/minecraft/client/gui/Gui")).hasSize(1);
        assertThat(env.juegoLoader()).isNotNull();
        assertThat(env.modLoaders()).containsKey("demo");
        assertThat(env.transformer().index()).isSameAs(env.morphs());
        assertThat(env.fuenteDataDriven()).containsKey("demo");
        assertThat(env.fuenteDataDriven().get("demo").habilitado()).isFalse();
        assertThat(VidaRuntime.current()).isSameAs(env);
    }

    @Test
    void boots_empty_when_no_mods(@TempDir Path root) throws Exception {
        Path mods = root.resolve("mods");
        java.nio.file.Files.createDirectories(mods);

        BootOptions opts = BootOptions.builder().modsDir(mods).build();
        BootReport report = VidaBoot.boot(opts);

        assertThat(report.isOk()).isTrue();
        assertThat(report.environment().resolvedMods()).isEmpty();
        // Платформенные морфы регистрируются всегда, даже без модов.
        assertThat(report.environment().morphs().totalMorphs()).isEqualTo(2);
    }

    @Test
    void skipDiscovery_bypasses_directory_scan(@TempDir Path root) throws Exception {
        // Нарочно указываем несуществующую modsDir, но с skipDiscovery=true.
        BootOptions opts = BootOptions.builder()
                .modsDir(root.resolve("nope"))
                .skipDiscovery(true)
                .build();
        BootReport report = VidaBoot.boot(opts);
        assertThat(report.isOk()).isTrue();
        assertThat(report.environment().resolvedMods()).isEmpty();
    }

    @Test
    void runtime_install_is_one_shot() throws Exception {
        BootOptions opts = BootOptions.builder().skipDiscovery(true).build();
        BootReport first = VidaBoot.boot(opts);
        assertThat(first.isOk()).isTrue();

        // Второй boot должен упасть при попытке install.
        assertThatThrownBy(() -> VidaBoot.boot(opts))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bad_mod_in_dir_reported_as_error(@TempDir Path root) throws Exception {
        Path mods = root.resolve("mods");
        java.nio.file.Files.createDirectories(mods);

        // Не-zip файл, но с расширением .jar
        java.nio.file.Files.writeString(mods.resolve("broken.jar"), "not a zip");

        BootReport report = VidaBoot.boot(BootOptions.builder().modsDir(mods).build());
        // ошибка зафиксирована, но не бросила
        assertThat(report.errors()).isNotEmpty();
        // environment всё равно собран (strict=false по умолчанию)
        assertThat(report.environment()).isNotNull();
    }
}
