/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.loader.VidaRuntime;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Watcher (opcional) que, con {@code -Dvida.dev.hotReload=true} y
 * {@code -Dvida.dev.hotReload.watch=&lt;dir&gt;}, notifica cambios bajo el
 * directorio de clases y vacía {@code CatalogoManejador} para re-registro.
 */
@ApiStatus.Internal
public final class DesarrolloHotReloadServicio {

    private static final Log LOG = Log.of(DesarrolloHotReloadServicio.class);
    private static final AtomicBoolean ARRANCADO = new AtomicBoolean(false);

    private DesarrolloHotReloadServicio() {}

    public static void maybeStart() {
        if (!Boolean.getBoolean("vida.dev.hotReload")) {
            return;
        }
        String watch = System.getProperty("vida.dev.hotReload.watch", "").trim();
        if (watch.isEmpty()) {
            LOG.info("Vida dev hot-reload: no vida.dev.hotReload.watch — catalog reset on file changes disabled");
            return;
        }
        Path root = Path.of(watch);
        if (!Files.isDirectory(root)) {
            LOG.warn("Vida dev hot-reload: watch dir does not exist yet — {}", root);
            return;
        }
        if (!ARRANCADO.compareAndSet(false, true)) {
            return;
        }
        Thread t = new Thread(() -> ejecutarWatcher(root), "vida-hot-reload");
        t.setDaemon(true);
        t.start();
        LOG.info("Vida dev hot-reload watching {}", root.toAbsolutePath());
    }

    private static void ejecutarWatcher(Path root) {
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            registrarArbol(root, ws);
            while (!Thread.interrupted()) {
                WatchKey key = ws.take();
                for (var ev : key.pollEvents()) {
                    if (ev.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    dispararReset();
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            LOG.error("Vida dev hot-reload watcher failed", ex);
        }
    }

    private static void registrarArbol(Path root, WatchService ws) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory).forEach(d -> {
                try {
                    d.register(ws,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE);
                } catch (IOException ex) {
                    LOG.warn("Vida hot-reload: skip watch {} ({})", d, ex.toString());
                }
            });
        }
    }

    private static void dispararReset() {
        VidaRuntime.maybeCurrent().ifPresent(env -> {
            try {
                env.catalogos().reiniciarParaHotReloadDesarrollo();
                LOG.info("Vida dev hot-reload: CatalogoManejador cleared (re-register mod content)");
            } catch (UnsupportedOperationException ex) {
                LOG.warn("Vida hot-reload reset skipped: {}", ex.getMessage());
            }
        });
    }
}
