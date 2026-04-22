/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Valenta — Sodium-class rendering optimization mod for Vida/Minecraft 1.21.1.
 *
 * <p>Provides VBO-batched {@code glMultiDrawElementsIndirect} rendering,
 * compact 16-byte vertex format, parallel chunk meshing via Susurro,
 * three-tier culling (frustum + occlusion + PVS), and quality-of-life
 * features like particle filtering, cloud control and GPU timing.
 *
 * @see dev.vida.mods.valenta.ValentaMod
 */
@dev.vida.core.ApiStatus.Preview("valenta")
package dev.vida.mods.valenta;
