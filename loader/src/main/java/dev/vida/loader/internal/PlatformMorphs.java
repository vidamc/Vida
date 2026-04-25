/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.loader.MorphIndex;
import dev.vida.loader.profile.PlatformProfileDescriptor;
import dev.vida.vifada.MorphSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Регистрирует платформенные Vifada-морфы {@code dev.vida.platform.*} в
 * {@link MorphIndex} — без участия какого-либо мода.
 *
 * <p>Если активен {@linkplain PlatformProfileDescriptor профиль} и в нём задан
 * {@code morphBundle}, в индекс попадают только перечисленные классы; иначе
 * регистрируются все известные платформенные морфы ({@link #ENTRIES}).
 */
@ApiStatus.Internal
public final class PlatformMorphs {

    private static final Log LOG = Log.of(PlatformMorphs.class);

    /** Пары: (FQN морф-класса, internal-name целевого класса). */
    static final String[][] ENTRIES = {
            {"dev.vida.platform.MinecraftTickMorph", "net/minecraft/client/Minecraft"},
            {"dev.vida.platform.GuiRenderMorph", "net/minecraft/client/gui/Gui"},
            {"dev.vida.platform.ServerTickMorph", "net/minecraft/server/MinecraftServer"},
            {"dev.vida.platform.ClientResourceReloadMorph", "net/minecraft/client/Minecraft"},
    };

    private PlatformMorphs() {}

    /**
     * @return количество фактически добавленных морфов
     */
    public static int register(MorphIndex.Builder sink, Optional<PlatformProfileDescriptor> profile) {
        Set<String> allowFqcn = null;
        if (profile.isPresent() && profile.get().morphBundle().isPresent()) {
            allowFqcn = new HashSet<>(profile.get().morphBundle().get());
        }

        int added = 0;
        ClassLoader cl = PlatformMorphs.class.getClassLoader();
        if (cl == null) cl = ClassLoader.getSystemClassLoader();

        for (String[] entry : ENTRIES) {
            String fqn = entry[0];
            if (allowFqcn != null && !allowFqcn.contains(fqn)) {
                continue;
            }
            String target = entry[1];
            String resource = fqn.replace('.', '/') + ".class";

            byte[] bytes = readBytes(cl, resource);
            if (bytes == null) {
                LOG.warn("Vida: platform morph '{}' not found on classpath", fqn);
                continue;
            }
            sink.add(target, new MorphSource(fqn.replace('.', '/'), bytes));
            added++;
        }
        if (allowFqcn != null) {
            for (String req : allowFqcn) {
                boolean ok = false;
                for (String[] e : ENTRIES) {
                    if (req.equals(e[0])) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    LOG.warn("Vida: morphBundle lists unknown platform morph '{}'", req);
                }
            }
        }
        if (added > 0) {
            LOG.info("Vida: registered {} platform morph(s)", added);
        }
        return added;
    }

    private static byte[] readBytes(ClassLoader cl, String resource) {
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (IOException ex) {
            LOG.warn("Vida: failed to read platform morph '{}' ({})", resource, ex.toString());
            return null;
        }
    }
}
