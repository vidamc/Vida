/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.loader.MorphIndex;
import dev.vida.vifada.MorphSource;
import java.io.IOException;
import java.io.InputStream;

/**
 * Регистрирует платформенные Vifada-морфы {@code dev.vida.platform.*} в
 * {@link MorphIndex} — без участия какого-либо мода.
 *
 * <p>Морфы лежат в {@code :loader}-jar как обычные классы и читаются тем же
 * {@link ClassLoader}, что загрузил сам этот класс; это работает как при
 * запуске через fat-jar агента ({@code agentJar}), так и в юнит-тестах,
 * запускающих {@code BootSequence} из gradle-project-classpath.
 *
 * <h2>Список морфов</h2>
 * <ul>
 *   <li>{@code dev.vida.platform.MinecraftTickMorph} →
 *       {@code net/minecraft/client/Minecraft};</li>
 *   <li>{@code dev.vida.platform.GuiRenderMorph} →
 *       {@code net/minecraft/client/gui/Gui}.</li>
 * </ul>
 */
@ApiStatus.Internal
public final class PlatformMorphs {

    private static final Log LOG = Log.of(PlatformMorphs.class);

    /** Пары: (FQN морф-класса, internal-name целевого класса). */
    private static final String[][] ENTRIES = {
            {"dev.vida.platform.MinecraftTickMorph", "net/minecraft/client/Minecraft"},
            {"dev.vida.platform.GuiRenderMorph",     "net/minecraft/client/gui/Gui"},
    };

    private PlatformMorphs() {}

    /**
     * Добавляет байты платформенных морфов в билдер. Каждый отсутствующий
     * ресурс логируется как warn и не ломает бутстрап.
     *
     * @return количество фактически добавленных морфов
     */
    public static int register(MorphIndex.Builder sink) {
        int added = 0;
        ClassLoader cl = PlatformMorphs.class.getClassLoader();
        if (cl == null) cl = ClassLoader.getSystemClassLoader();

        for (String[] entry : ENTRIES) {
            String fqn = entry[0];
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
