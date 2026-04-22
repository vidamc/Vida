/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;
import java.util.Objects;
import java.util.Optional;

/**
 * Автор мода — запись в поле {@code authors[]} манифеста.
 */
@ApiStatus.Stable
public record ModAuthor(String name, Optional<String> contact) {

    public ModAuthor {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(contact, "contact");
        if (name.isBlank()) {
            throw new IllegalArgumentException("author name must not be blank");
        }
    }

    /** Автор без контактной информации. */
    public static ModAuthor of(String name) {
        return new ModAuthor(name, Optional.empty());
    }

    /** Автор с email/URL. */
    public static ModAuthor of(String name, String contact) {
        return new ModAuthor(name, Optional.ofNullable(contact));
    }
}
