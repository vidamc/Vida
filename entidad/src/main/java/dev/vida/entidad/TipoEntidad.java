/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad;

import dev.vida.core.ApiStatus;

/**
 * Базовая категория сущности.
 *
 * <p>Это не runtime-класс сущности и не vanilla mob-category, а
 * компактная классификация для модов и bridge-слоёв Vida.
 */
@ApiStatus.Stable
public enum TipoEntidad {
    CRIATURA(false, true, true),
    MONSTRUO(true, true, true),
    AMBIENTAL(false, true, true),
    ACUATICA(false, true, true),
    UTILIDAD(false, false, false),
    PROYECTIL(false, false, false),
    JEFE(true, true, true),
    MISCELANEA(false, false, false);

    private final boolean hostil;
    private final boolean viva;
    private final boolean iaPorDefecto;

    TipoEntidad(boolean hostil, boolean viva, boolean iaPorDefecto) {
        this.hostil = hostil;
        this.viva = viva;
        this.iaPorDefecto = iaPorDefecto;
    }

    public boolean esHostil() { return hostil; }
    public boolean esViva() { return viva; }
    public boolean tieneIaPorDefecto() { return iaPorDefecto; }
}
