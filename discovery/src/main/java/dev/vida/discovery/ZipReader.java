/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Абстракция над zip/jar-архивом.
 *
 * <p>Две реализации:
 * <ul>
 *   <li>{@link FileZipReader} — поверх {@link java.util.zip.ZipFile},
 *       эффективен для больших дисковых файлов (random-access через ZIP-CD);</li>
 *   <li>{@link BytesZipReader} — поверх {@link java.util.zip.ZipInputStream},
 *       предназначен для небольших вложенных JAR, прочитанных в память.</li>
 * </ul>
 *
 * <p>Реализации <b>не потокобезопасны</b>: одновременный доступ из нескольких
 * потоков даёт неопределённое поведение. Ридер должен быть закрыт после
 * использования; {@link #close()} не бросает исключений.
 */
@ApiStatus.Stable
public interface ZipReader extends AutoCloseable {

    /** Имя источника для диагностических сообщений (например, полный путь). */
    String source();

    /** {@code true}, если архив содержит запись с указанным относительным путём. */
    boolean contains(String path);

    /**
     * Возвращает содержимое записи целиком.
     *
     * @throws IOException если запись отсутствует или поток повреждён
     */
    byte[] read(String path) throws IOException;

    /**
     * Открывает запись как поток. Вызывающий обязан закрыть его.
     *
     * @throws IOException если запись отсутствует или поток повреждён
     */
    InputStream open(String path) throws IOException;

    /**
     * Имена всех записей архива (только файлы, без директорий). Коллекция
     * иммутабельна для читателя — её безопасно передавать наружу.
     */
    Collection<String> entries();

    /** Закрывает ридер. Не бросает проверяемых исключений. */
    @Override
    void close();
}
