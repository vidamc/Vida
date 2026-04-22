/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.escultor;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.ValentaPantallaGraficos;
import dev.vida.vifada.VifadaMorph;
import dev.vida.vifada.VifadaOverwrite;

/**
 * Sustituye el cuerpo de {@code VideoSettingsScreen#addOptions} para mostrar
 * primero los ajustes de Valenta y después las opciones de vídeo vanilla.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.gui.screens.options.VideoSettingsScreen", priority = 850)
public abstract class VideoSettingsScreenMorph {

    @VifadaOverwrite
    protected void addOptions() {
        ValentaPantallaGraficos.reemplazarOpcionesVideo(this);
    }
}
