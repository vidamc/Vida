/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import dev.vida.core.ApiStatus;

/**
 * Публичная спецификация бинарного формата Cartografía ({@code .ctg}).
 *
 * <p>Версия задаётся парой {@linkplain #VERSION_MAJOR major}/{@linkplain
 * #VERSION_MINOR minor}. Читатель принимает файлы с тем же {@code major} и
 * {@code minor ≤}{@link #MAX_SUPPORTED_MINOR}; более новый {@code minor} при
 * том же {@code major} отклоняется как {@link dev.vida.cartografia.MappingError.UnsupportedVersion}
 * до обновления читателя.
 *
 * <p>Полное бинарное описание секций см. {@link CtgFormat} (внутренний класс).
 *
 * @see CtgReader
 * @see CtgWriter
 */
@ApiStatus.Stable
public final class CtgMappingFormat {

    private CtgMappingFormat() {}

    /** Major version stored in file header (must match reader). */
    public static final int VERSION_MAJOR = 1;

    /** Minor version produced by {@link CtgWriter} for this release line. */
    public static final int VERSION_MINOR = 0;

    /**
     * Наибольший minor для {@link #VERSION_MAJOR}, который понимает текущий
     * {@link CtgReader}.
     */
    public static final int MAX_SUPPORTED_MINOR = 0;

    /** Magic prefix bytes at start of every {@code .ctg} file. */
    public static final byte[] MAGIC = CtgFormat.MAGIC.clone();
}
