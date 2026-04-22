/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import dev.vida.core.ApiStatus;

/**
 * Константы формата Vida CarToGrafía ({@code .ctg}).
 *
 * <p>Файл {@code .ctg} — компактное бинарное представление {@link
 * dev.vida.cartografia.MappingTree} со строковым пулом и секциями
 * namespace/classes/fields/methods.
 *
 * <h2>Layout v1.0 (big-endian, как у {@code DataInputStream})</h2>
 * <pre>
 * Header:
 *   magic          8 bytes    = 'V','I','D','A','C','T','G','\n'
 *   version_major  1 byte     = 1
 *   version_minor  1 byte     = 0
 *   flags          2 bytes    = 0 (reserved)
 *
 * StringPool:
 *   count          u32
 *   entry*count:
 *     length       u32 (в байтах UTF-8)
 *     bytes        UTF-8
 *
 *   Строка по индексу 0 — всегда пустая; никаких других гарантий
 *   упорядоченности не даётся.
 *
 * Namespaces:
 *   count          u16
 *   stringIndex*count  u32
 *
 * Classes:
 *   count          u32
 *   classEntry*count:
 *     nameIndex*nsCount     u32
 *     fieldCount            u32
 *     field*fieldCount:
 *       descIndex           u32
 *       nameIndex*nsCount   u32
 *     methodCount           u32
 *     method*methodCount:
 *       descIndex           u32
 *       nameIndex*nsCount   u32
 *
 * Footer:
 *   u32 end marker = 0xFFFFFFFF
 * </pre>
 *
 * <p>Класс — только контейнер констант; логика чтения и записи в
 * {@link CtgReader}/{@link CtgWriter}.
 */
@ApiStatus.Internal
final class CtgFormat {

    private CtgFormat() {}

    static final byte[] MAGIC = {'V', 'I', 'D', 'A', 'C', 'T', 'G', '\n'};
    static final int VERSION_MAJOR = 1;
    static final int VERSION_MINOR = 0;
    static final int END_MARKER = 0xFFFFFFFF;

    /** Индекс «пустой» строки в пуле; резервируется записывателем. */
    static final int EMPTY_STRING_INDEX = 0;
}
