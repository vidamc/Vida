/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.cima.CimaJuego;
import dev.vida.core.ApiStatus;
import dev.vida.mundo.Mundo;
import java.util.Optional;

/**
 * Cima que reutiliza el mismo {@code Object Level} (cliente) que
 * {@link MundoNivelVanilla} y {@link VanillaBridge}.
 */
@ApiStatus.Internal
public final class CimaJuegoCarga implements CimaJuego {

    @Override
    public boolean vinculado() {
        return MinecraftClientReflect.nivelHabitualOEmpty().isPresent();
    }

    @Override
    public Optional<Object> nivelMinecraftVivo() {
        return MinecraftClientReflect.nivelHabitualOEmpty();
    }

    @Override
    public Optional<Mundo> mundoSobreNivelCargado() {
        return MinecraftClientReflect.nivelHabitualOEmpty().map(MundoNivelVanilla::new);
    }
}
