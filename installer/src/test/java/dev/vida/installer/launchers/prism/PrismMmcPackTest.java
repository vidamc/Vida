/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.mc.JsonTree;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PrismMmcPackTest {

    @SuppressWarnings("unchecked")
    @Test
    void renders_minecraft_and_vida_components() {
        String json = new PrismMmcPack("1.21.1", "0.1.0").render();
        Map<String, Object> root = (Map<String, Object>) JsonTree.parse(json);
        assertThat(root).containsEntry("formatVersion", 1L);

        List<Map<String, Object>> comps = (List<Map<String, Object>>) root.get("components");
        assertThat(comps).hasSize(2);
        assertThat(comps.get(0)).containsEntry("uid", "net.minecraft")
                .containsEntry("version", "1.21.1")
                .containsEntry("important", true);
        assertThat(comps.get(1)).containsEntry("uid", "dev.vida.loader")
                .containsEntry("version", "0.1.0")
                .containsEntry("cachedName", "Vida Loader");
    }

    @SuppressWarnings("unchecked")
    @Test
    void vida_requires_minecraft_equals() {
        String json = new PrismMmcPack("1.21.1", "0.1.0").render();
        Map<String, Object> root = (Map<String, Object>) JsonTree.parse(json);
        List<Map<String, Object>> comps = (List<Map<String, Object>>) root.get("components");
        List<Map<String, Object>> requires =
                (List<Map<String, Object>>) comps.get(1).get("cachedRequires");
        assertThat(requires).hasSize(1);
        assertThat(requires.get(0))
                .containsEntry("uid", "net.minecraft")
                .containsEntry("equals", "1.21.1");
    }

    @SuppressWarnings("unchecked")
    @Test
    void extra_components_go_between_mc_and_vida() {
        String json = new PrismMmcPack("1.21.1", "0.1.0")
                .addComponent("net.fabricmc.fabric-loader", "0.15.11", "Fabric Loader")
                .render();
        Map<String, Object> root = (Map<String, Object>) JsonTree.parse(json);
        List<Map<String, Object>> comps = (List<Map<String, Object>>) root.get("components");
        assertThat(comps).hasSize(3);
        assertThat(comps.get(1)).containsEntry("uid", "net.fabricmc.fabric-loader");
        assertThat(comps.get(2)).containsEntry("uid", "dev.vida.loader");
    }
}
