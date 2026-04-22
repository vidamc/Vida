/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.atlauncher;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.launchers.InstanceRef;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ATLauncherInstanceScannerTest {

    @Test
    void empty_base_returns_empty(@TempDir Path dir) throws IOException {
        assertThat(ATLauncherInstanceScanner.list(dir)).isEmpty();
    }

    @Test
    void reads_fabric_instance(@TempDir Path dir) throws IOException {
        Path inst = dir.resolve("instances/MyPack");
        Files.createDirectories(inst);
        Files.writeString(inst.resolve("instance.json"), """
                {
                  "id":"1.21.1",
                  "launcher":{
                    "name":"My Awesome Pack",
                    "loaderVersion":{"type":"Fabric","version":"0.15.11"}
                  }
                }
                """, StandardCharsets.UTF_8);

        List<InstanceRef> refs = ATLauncherInstanceScanner.list(dir);
        assertThat(refs).hasSize(1);
        InstanceRef r = refs.get(0);
        assertThat(r.id()).isEqualTo("MyPack");
        assertThat(r.displayName()).isEqualTo("My Awesome Pack");
        assertThat(r.minecraftVersion()).isEqualTo("1.21.1");
        assertThat(r.loader()).contains("fabric");
        assertThat(r.loaderVersion()).contains("0.15.11");
    }

    @Test
    void vanilla_instance_without_loader(@TempDir Path dir) throws IOException {
        Path inst = dir.resolve("instances/Plain");
        Files.createDirectories(inst);
        Files.writeString(inst.resolve("instance.json"), """
                {"id":"1.21.1","launcher":{"name":"Plain"}}
                """, StandardCharsets.UTF_8);

        InstanceRef r = ATLauncherInstanceScanner.list(dir).get(0);
        assertThat(r.loader()).isEmpty();
        assertThat(r.loaderVersion()).isEmpty();
    }

    @Test
    void dir_without_instance_json_is_skipped(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("instances/empty"));
        assertThat(ATLauncherInstanceScanner.list(dir)).isEmpty();
    }

    @Test
    void broken_json_is_skipped(@TempDir Path dir) throws IOException {
        Path inst = dir.resolve("instances/Broken");
        Files.createDirectories(inst);
        Files.writeString(inst.resolve("instance.json"),
                "{ this is not json", StandardCharsets.UTF_8);

        assertThat(ATLauncherInstanceScanner.list(dir)).isEmpty();
    }
}
