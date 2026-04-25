/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cima;

import dev.vida.mundo.Mundo;
import java.util.Optional;

/**
 * Cima, пока загрузчик ещё не поставил реализацию, и в тестах без
 * <strong>Level</strong>.
 */
public enum CimaJuegoNulo implements CimaJuego {
    /** Единый экземпляр-заглушка. */
    INSTANCIA;

    @Override
    public boolean vinculado() {
        return false;
    }

    @Override
    public Optional<Object> nivelMinecraftVivo() {
        return Optional.empty();
    }

    @Override
    public Optional<Mundo> mundoSobreNivelCargado() {
        return Optional.empty();
    }
}
