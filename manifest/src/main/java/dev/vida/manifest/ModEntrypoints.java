/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;
import java.util.List;
import java.util.Objects;

/**
 * Набор точек входа мода. Каждое поле — имя класса, реализующего соответствующий
 * {@code Entrypoint}-интерфейс (будет добавлен в {@code vida-base}).
 *
 * <p>Порядок вызова: {@code preLaunch} → {@code main} → {@code client}/{@code server}.
 */
@ApiStatus.Stable
public record ModEntrypoints(
        List<String> preLaunch,
        List<String> main,
        List<String> client,
        List<String> server) {

    public static final ModEntrypoints EMPTY =
            new ModEntrypoints(List.of(), List.of(), List.of(), List.of());

    public ModEntrypoints {
        preLaunch = List.copyOf(Objects.requireNonNull(preLaunch, "preLaunch"));
        main      = List.copyOf(Objects.requireNonNull(main, "main"));
        client    = List.copyOf(Objects.requireNonNull(client, "client"));
        server    = List.copyOf(Objects.requireNonNull(server, "server"));
    }

    public boolean isEmpty() {
        return preLaunch.isEmpty() && main.isEmpty() && client.isEmpty() && server.isEmpty();
    }
}
