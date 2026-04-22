/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.cache;

import dev.vida.core.ApiStatus;

/**
 * Константы бинарного формата {@code mods.idx}.
 *
 * <h2>Layout v1.0 (big-endian)</h2>
 * <pre>
 * Header:
 *   magic      8 bytes  = 'V','I','D','A','I','D','X','\n'
 *   major      1 byte   = 1
 *   minor      1 byte   = 0
 *   flags      2 bytes  = 0 (reserved)
 *   written_at 8 bytes  (millis since epoch)
 *
 * StringPool:
 *   count      u32
 *   entry*count:
 *     length   u32 (UTF-8 байт)
 *     bytes    UTF-8
 *   Строка по индексу 0 — всегда пустая.
 *
 * Entries:
 *   count      u32
 *   entry*count:
 *     sourceIdIdx   u32
 *     depth         u32
 *     mtimeMillis   u64
 *     sizeBytes     u64
 *     sha256        32 bytes
 *     modIdIdx      u32
 *     modVersionIdx u32
 *     nestedCount   u32
 *     innerPathIdx*nestedCount  u32
 *
 * Footer:
 *   u32 endMarker = 0xFFFFFFFF
 * </pre>
 */
@ApiStatus.Internal
final class ModsIndexFormat {

    private ModsIndexFormat() {}

    static final byte[] MAGIC = {'V', 'I', 'D', 'A', 'I', 'D', 'X', '\n'};
    static final int VERSION_MAJOR = 1;
    static final int VERSION_MINOR = 0;
    static final int END_MARKER = 0xFFFFFFFF;

    static final int EMPTY_STRING_INDEX = 0;
}
