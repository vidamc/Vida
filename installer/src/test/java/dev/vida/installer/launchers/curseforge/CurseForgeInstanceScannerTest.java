/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.curseforge;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CurseForgeInstanceScannerTest {

    @TempDir Path tmp;

    @Test
    void discovers_instances_with_minecraftinstance_json() throws IOException {
        Path instances = tmp.resolve("Instances");
        Path inst1 = instances.resolve("MyPack");
        Files.createDirectories(inst1);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "My Pack");
        data.put("gameVersion", "1.21.1");

        Map<String, Object> bml = new LinkedHashMap<>();
        bml.put("name", "fabric-0.16.0");
        data.put("baseModLoader", bml);

        Files.writeString(inst1.resolve("minecraftinstance.json"),
                JsonTree.write(data), StandardCharsets.UTF_8);

        List<InstanceRef> refs = CurseForgeInstanceScanner.list(tmp);

        assertThat(refs).hasSize(1);
        InstanceRef ref = refs.getFirst();
        assertThat(ref.id()).isEqualTo("MyPack");
        assertThat(ref.displayName()).isEqualTo("My Pack");
        assertThat(ref.minecraftVersion()).isEqualTo("1.21.1");
        assertThat(ref.loader()).hasValue("fabric");
        assertThat(ref.loaderVersion()).hasValue("0.16.0");
    }

    @Test
    void empty_dir_returns_empty_list() throws IOException {
        assertThat(CurseForgeInstanceScanner.list(tmp)).isEmpty();
    }

    @Test
    void skips_dirs_without_json() throws IOException {
        Path instances = tmp.resolve("Instances");
        Files.createDirectories(instances.resolve("NoJson"));

        assertThat(CurseForgeInstanceScanner.list(tmp)).isEmpty();
    }

    @Test
    void handles_missing_loader() throws IOException {
        Path instances = tmp.resolve("Instances");
        Path inst = instances.resolve("Vanilla");
        Files.createDirectories(inst);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Vanilla");
        data.put("gameVersion", "1.21.1");
        Files.writeString(inst.resolve("minecraftinstance.json"),
                JsonTree.write(data), StandardCharsets.UTF_8);

        List<InstanceRef> refs = CurseForgeInstanceScanner.list(tmp);
        assertThat(refs).hasSize(1);
        assertThat(refs.getFirst().loader()).isEmpty();
        assertThat(refs.getFirst().loaderVersion()).isEmpty();
    }
}
