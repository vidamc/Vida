/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Vifada morphs for Valenta render pipeline injection.
 *
 * <p>All morphs target {@code net/minecraft/client/renderer/...} classes
 * and are grouped under the namespace {@code valenta.escultor} for
 * conflict detection by {@code VidaClassTransformer}.
 *
 * <p>Each morph uses {@code requireTarget = false} to gracefully degrade
 * on unsupported MC versions.
 */
@dev.vida.core.ApiStatus.Internal
package dev.vida.mods.valenta.escultor;
