/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import dev.vida.installer.mc.JsonTree;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сборщик {@code mmc-pack.json} — component-list'а Prism/MultiMC instance'а.
 *
 * <p>Формат (см. research report и
 * <a href="https://meta.prismlauncher.org/v1/index.json">meta.prismlauncher.org</a>):
 * <pre>{@code
 * {
 *   "formatVersion": 1,
 *   "components": [
 *     { "uid": "net.minecraft",      "version": "1.21.1",
 *       "cachedName": "Minecraft", "important": true },
 *     { "uid": "dev.vida.loader",    "version": "<loaderVer>",
 *       "cachedName": "Vida Loader",
 *       "cachedRequires": [ { "equals": "1.21.1", "uid": "net.minecraft" } ] }
 *   ]
 * }
 * }</pre>
 *
 * <p>{@code cachedName} / {@code cachedRequires} — optimization для offline-списка
 * instance'ов; launcher использует их, пока мета-кеш не обновился. Опционально.
 */
public final class PrismMmcPack {

    private static final String NET_MINECRAFT_UID = "net.minecraft";

    private final String minecraftVersion;
    private final String loaderVersion;
    private final List<Map<String, Object>> extraComponents = new ArrayList<>();

    public PrismMmcPack(String minecraftVersion, String loaderVersion) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion);
        this.loaderVersion = Objects.requireNonNull(loaderVersion);
    }

    /**
     * Добавляет дополнительный компонент (например, Fabric Loader).
     * Компонент лежит в массиве до Vida, но после Minecraft.
     */
    public PrismMmcPack addComponent(String uid, String version, String cachedName) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("uid", uid);
        c.put("version", version);
        if (cachedName != null && !cachedName.isBlank()) {
            c.put("cachedName", cachedName);
        }
        extraComponents.add(c);
        return this;
    }

    public String render() {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Object> components = new ArrayList<>();

        // 1) net.minecraft — важный, чтобы launcher resolve'ил vanilla.
        Map<String, Object> mc = new LinkedHashMap<>();
        mc.put("cachedName", "Minecraft");
        mc.put("cachedVersion", minecraftVersion);
        mc.put("important", true);
        mc.put("uid", NET_MINECRAFT_UID);
        mc.put("version", minecraftVersion);
        components.add(mc);

        // 2) дополнительные (Fabric и т. п.)
        components.addAll(extraComponents);

        // 3) наш компонент.
        Map<String, Object> vida = new LinkedHashMap<>();
        vida.put("cachedName", PrismInstanceLayout.COMPONENT_NAME);
        List<Object> requires = new ArrayList<>();
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("equals", minecraftVersion);
        req.put("uid", NET_MINECRAFT_UID);
        requires.add(req);
        vida.put("cachedRequires", requires);
        vida.put("cachedVersion", loaderVersion);
        vida.put("uid", PrismInstanceLayout.COMPONENT_UID);
        vida.put("version", loaderVersion);
        components.add(vida);

        root.put("components", components);
        root.put("formatVersion", 1L);
        return JsonTree.write(root);
    }
}
