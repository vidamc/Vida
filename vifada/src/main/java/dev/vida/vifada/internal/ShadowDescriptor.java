/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Одно {@link dev.vida.vifada.VifadaShadow}-объявление: поле либо метод,
 * которое «на самом деле» живёт в целевом классе.
 *
 * @param kind          тип члена
 * @param name          имя
 * @param descriptor    ASM-дескриптор (для поля — тип, для метода — сигнатура)
 * @param silentMissing молчать ли, если член отсутствует в целевом
 */
@ApiStatus.Internal
public record ShadowDescriptor(Kind kind, String name, String descriptor, boolean silentMissing) {

    public ShadowDescriptor {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(name);
        Objects.requireNonNull(descriptor);
    }

    public enum Kind { FIELD, METHOD }
}
