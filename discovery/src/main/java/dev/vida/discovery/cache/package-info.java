/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Бинарный кэш {@code mods.idx}: ускоряет повторный холодный старт — если
 * размер и mtime каждого jar-а не изменились, манифесты разбирать не
 * обязательно.
 */
package dev.vida.discovery.cache;
