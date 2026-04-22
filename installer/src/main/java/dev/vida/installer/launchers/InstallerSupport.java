/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

import dev.vida.installer.InstallerCore;
import dev.vida.installer.mc.McArtifacts;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Общие утилиты, которые переиспользуют {@link LauncherHandler}'ы:
 * извлечение «вшитого» loader.jar, атомарная запись файлов, OS-детект
 * и пр.
 *
 * <p>Все методы — static, класс не инстанцируется.
 */
public final class InstallerSupport {

    private InstallerSupport() {}

    /**
     * Извлекает embedded {@code /loader/loader.jar} из classpath installer'а
     * в указанный файл, возвращая SHA-1 и размер.
     *
     * <p>При {@code dryRun=true} файл не пишется, но sha1/size считаются
     * (прогоняем stream в {@link OutputStream#nullOutputStream()}).
     *
     * @throws IOException если ресурс отсутствует или запись не удалась
     */
    public static McArtifacts.Sha1Result extractEmbeddedLoader(Path target, boolean dryRun)
            throws IOException {
        try (InputStream in = InstallerCore.class
                .getResourceAsStream(InstallerCore.EMBEDDED_LOADER_RESOURCE)) {
            if (in == null) {
                throw new IOException(
                        "Embedded loader resource missing: " + InstallerCore.EMBEDDED_LOADER_RESOURCE
                        + ". The installer jar was built incorrectly.");
            }
            if (dryRun) {
                return McArtifacts.copyWithSha1(in, OutputStream.nullOutputStream());
            }
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".part");
            McArtifacts.Sha1Result result;
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp))) {
                result = McArtifacts.copyWithSha1(in, out);
            }
            try {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return result;
        }
    }

    /**
     * Атомарная запись текстового файла (через {@code .tmp} + rename).
     * Родительские каталоги создаются автоматически.
     */
    public static void writeAtomically(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** {@code true} если запущены на Windows. */
    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
