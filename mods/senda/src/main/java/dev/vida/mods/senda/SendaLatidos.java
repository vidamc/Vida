/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.senda;

import dev.vida.base.latidos.Latido;
import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Латидо-события мода Senda.
 *
 * <p>Другие моды могут подписываться на эти события для реакции на
 * изменение путевых точек.
 */
@ApiStatus.Preview("senda")
public final class SendaLatidos {

    private SendaLatidos() {}

    /**
     * Испускается при успешной регистрации новой путевой точки.
     *
     * @param punto зарегистрированная точка
     */
    public record PuntoAgregado(PuntoRuta punto) {
        public PuntoAgregado {
            Objects.requireNonNull(punto, "punto");
        }

        public static final Latido<PuntoAgregado> TIPO =
                Latido.de("senda:punto_agregado", PuntoAgregado.class);
    }

    /**
     * Испускается при удалении путевой точки.
     *
     * @param punto удалённая точка
     */
    public record PuntoEliminado(PuntoRuta punto) {
        public PuntoEliminado {
            Objects.requireNonNull(punto, "punto");
        }

        public static final Latido<PuntoEliminado> TIPO =
                Latido.de("senda:punto_eliminado", PuntoEliminado.class);
    }

    /**
     * Испускается при очистке всех точек для конкретного измерения.
     *
     * @param dimension измерение, в котором очищены все точки
     */
    public record DimensionLimpiada(String dimension) {
        public DimensionLimpiada {
            Objects.requireNonNull(dimension, "dimension");
        }

        public static final Latido<DimensionLimpiada> TIPO =
                Latido.de("senda:dimension_limpiada", DimensionLimpiada.class);
    }
}
