/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Ссылка на существующий instance (профиль) в одном из лаунчеров.
 *
 * <p>Используется:
 * <ul>
 *   <li>для {@link InstallMode#PATCH_EXISTING_INSTANCE} — выбор цели;</li>
 *   <li>для предупреждений GUI «уже установлен в этот instance»;</li>
 *   <li>для CLI-команды {@code --list-instances}.</li>
 * </ul>
 *
 * @param id               стабильный идентификатор в контексте лаунчера
 *                         (имя каталога, UUID, SQLite-rowid и т. п.)
 * @param displayName      человекочитаемое имя
 * @param instancePath     абсолютный путь к каталогу instance'а
 * @param minecraftVersion версия Minecraft или пустая строка, если неизвестно
 * @param loader           {@code fabric}/{@code forge}/{@code neoforge}/{@code quilt}/{@code vanilla}
 * @param loaderVersion    версия модлоадера или {@link Optional#empty()}
 */
public record InstanceRef(
        String id,
        String displayName,
        Path instancePath,
        String minecraftVersion,
        Optional<String> loader,
        Optional<String> loaderVersion) {

    public InstanceRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(instancePath, "instancePath");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(loaderVersion, "loaderVersion");
    }
}
