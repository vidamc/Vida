/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

/**
 * Режим инсталляции в конкретный лаунчер.
 *
 * <p>{@link #CREATE_NEW_PROFILE} — handler умеет создать профиль/instance с
 * нуля; требует только корневой каталог лаунчера и (опционально) имя
 * нового instance.
 *
 * <p>{@link #PATCH_EXISTING_INSTANCE} — handler требует уже существующий
 * instance; инсталлятор может только дописать в него {@code -javaagent}
 * и положить loader.jar внутрь. Применяется там, где попытка создать
 * instance «снизу» сломалась бы при следующем запуске лаунчера
 * (ATLauncher, CurseForge).
 */
public enum InstallMode {
    CREATE_NEW_PROFILE,
    PATCH_EXISTING_INSTANCE
}
