/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import dev.vida.bloque.Bloque;
import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Предмет, ставящий блок в мир. Аналог vanilla {@code BlockItem}.
 *
 * <p>id предмета по умолчанию совпадает с id блока — это соответствует
 * vanilla-соглашению. Если модератор хочет иной id, он может использовать
 * {@link #conId(Bloque, dev.vida.core.Identifier, PropiedadesObjeto)}.
 */
@ApiStatus.Preview("objeto")
public final class ObjetoDeBloque extends Objeto {

    private final Bloque bloque;

    private ObjetoDeBloque(dev.vida.core.Identifier id, PropiedadesObjeto propiedades, Bloque bloque) {
        super(id, propiedades);
        this.bloque = Objects.requireNonNull(bloque, "bloque");
    }

    /** id предмета = id блока. */
    public static ObjetoDeBloque de(Bloque bloque) {
        return de(bloque, PropiedadesObjeto.con().tipo(TipoObjeto.BLOQUE).construir());
    }

    /** id предмета = id блока, пользовательские свойства. */
    public static ObjetoDeBloque de(Bloque bloque, PropiedadesObjeto propiedades) {
        Objects.requireNonNull(bloque, "bloque");
        Objects.requireNonNull(propiedades, "propiedades");
        return new ObjetoDeBloque(bloque.id(), propiedades, bloque);
    }

    /** Полный override id. */
    public static ObjetoDeBloque conId(Bloque bloque, dev.vida.core.Identifier id,
                                       PropiedadesObjeto propiedades) {
        Objects.requireNonNull(bloque, "bloque");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(propiedades, "propiedades");
        return new ObjetoDeBloque(id, propiedades, bloque);
    }

    /** Связанный блок. */
    public Bloque bloque() { return bloque; }
}
