/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Arrays;
import java.util.Objects;

/**
 * Синхронизация произвольного состояния block entity с клиентом (сервер → клиент).
 *
 * <p>{@code sincPayload} — сериализованное состояние (например NBT или компактный бинарный вид),
 * без привязки к конкретному кодеку в этом слое.
 */
@ApiStatus.Stable
public record PaqueteBloqueEntidadEstado(Identifier tipoEntidad, long posicionBloque, byte[] sincPayload)
        implements PaqueteCliente {

    public PaqueteBloqueEntidadEstado {
        Objects.requireNonNull(tipoEntidad, "tipoEntidad");
        Objects.requireNonNull(sincPayload, "sincPayload");
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PaqueteBloqueEntidadEstado that)) {
            return false;
        }
        return tipoEntidad.equals(that.tipoEntidad)
                && posicionBloque == that.posicionBloque
                && Arrays.equals(sincPayload, that.sincPayload);
    }

    @Override
    public int hashCode() {
        int h = Objects.hash(tipoEntidad, posicionBloque);
        return 31 * h + Arrays.hashCode(sincPayload);
    }

    @Override
    public String toString() {
        return "PaqueteBloqueEntidadEstado[tipoEntidad="
                + tipoEntidad
                + ", posicionBloque="
                + posicionBloque
                + ", sincPayload.len="
                + sincPayload.length
                + "]";
    }

}
