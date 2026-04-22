/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.launchers.InstanceRef;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PrismInstanceScannerTest {

    @Test
    void empty_data_dir_returns_empty(@TempDir Path dir) throws IOException {
        List<InstanceRef> refs = PrismInstanceScanner.list(dir);
        assertThat(refs).isEmpty();
    }

    @Test
    void directory_without_instance_cfg_is_skipped(@TempDir Path dir) throws IOException {
        Path notInst = dir.resolve("instances/not-an-instance");
        Files.createDirectories(notInst);
        assertThat(PrismInstanceScanner.list(dir)).isEmpty();
    }

    @Test
    void reads_name_and_mc_version_from_fabric_instance(@TempDir Path dir) throws IOException {
        Path inst = dir.resolve("instances/MyPack");
        Files.createDirectories(inst);
        Files.writeString(inst.resolve("instance.cfg"), """
                InstanceType=OneSix
                name=My Fabric Pack
                """, StandardCharsets.UTF_8);
        Files.writeString(inst.resolve("mmc-pack.json"), """
                {
                  "components": [
                    {"uid":"net.minecraft","version":"1.21.1"},
                    {"uid":"net.fabricmc.fabric-loader","version":"0.15.11"}
                  ],
                  "formatVersion": 1
                }
                """, StandardCharsets.UTF_8);

        List<InstanceRef> refs = PrismInstanceScanner.list(dir);
        assertThat(refs).hasSize(1);
        InstanceRef r = refs.get(0);
        assertThat(r.id()).isEqualTo("MyPack");
        assertThat(r.displayName()).isEqualTo("My Fabric Pack");
        assertThat(r.minecraftVersion()).isEqualTo("1.21.1");
        assertThat(r.loader()).contains("fabric");
        assertThat(r.loaderVersion()).contains("0.15.11");
    }

    @Test
    void vanilla_instance_has_no_loader(@TempDir Path dir) throws IOException {
        Path inst = dir.resolve("instances/Vanilla");
        Files.createDirectories(inst);
        Files.writeString(inst.resolve("instance.cfg"),
                "InstanceType=OneSix\nname=Vanilla\n",
                StandardCharsets.UTF_8);
        Files.writeString(inst.resolve("mmc-pack.json"), """
                {
                  "components": [{"uid":"net.minecraft","version":"1.21.1"}],
                  "formatVersion": 1
                }
                """, StandardCharsets.UTF_8);

        InstanceRef r = PrismInstanceScanner.list(dir).get(0);
        assertThat(r.loader()).isEmpty();
        assertThat(r.loaderVersion()).isEmpty();
    }

    @Test
    void missing_mmc_pack_does_not_fail(@TempDir Path dir) throws IOException {
        Path inst = dir.resolve("instances/Unparsed");
        Files.createDirectories(inst);
        Files.writeString(inst.resolve("instance.cfg"),
                "InstanceType=OneSix\nname=Unparsed\n",
                StandardCharsets.UTF_8);

        List<InstanceRef> refs = PrismInstanceScanner.list(dir);
        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).minecraftVersion()).isEmpty();
    }
}
