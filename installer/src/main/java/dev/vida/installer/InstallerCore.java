/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import dev.vida.core.ApiStatus;
import dev.vida.installer.launchers.LauncherHandler;
import dev.vida.installer.launchers.LauncherRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Тонкий диспетчер инсталлятора Vida. Принимает {@link InstallOptions},
 * выбирает {@link LauncherHandler} через {@link LauncherRegistry} и
 * делегирует ему установку.
 *
 * <p>Сам по себе кода установки не содержит; вся логика разложена по
 * {@code launchers/&lt;kind&gt;/} пакетам. Это позволяет добавлять
 * поддержку новых лаунчеров без изменения этой точки входа.
 *
 * <p>Константы {@link #EMBEDDED_LOADER_RESOURCE} и
 * {@link #INSTALL_MANIFEST} оставлены здесь публичными для тестов и
 * обратной совместимости с внешним кодом.
 */
@ApiStatus.Preview("installer")
public final class InstallerCore {

    private static final Logger LOG = LoggerFactory.getLogger(InstallerCore.class);

    /** Имя ресурса со вшитым loader.jar внутри installer.jar. */
    public static final String EMBEDDED_LOADER_RESOURCE = "/loader/loader.jar";

    /** Имя audit-inventory файла в {@code vida/}. */
    public static final String INSTALL_MANIFEST = "install.json";

    /**
     * Текущая версия schema install.json.
     *
     * <p>Дублируется в {@code MojangHandler.INSTALL_MANIFEST_SCHEMA},
     * чтобы не вносить циклические зависимости — handler'ы могут
     * эволюционировать независимо.
     */
    public static final int INSTALL_MANIFEST_SCHEMA = 2;

    private final Consumer<String> progress;

    public InstallerCore() {
        this(msg -> {});
    }

    public InstallerCore(Consumer<String> progress) {
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    /** Основная точка входа. */
    public InstallReport install(InstallOptions opt) {
        Objects.requireNonNull(opt, "opt");
        try {
            LauncherHandler handler = LauncherRegistry.forKind(opt.launcherKind());
            LOG.info("Installing via {} → {}", handler.kind().displayName(), opt.installDir());
            return handler.install(opt, progress);
        } catch (IllegalArgumentException e) {
            // Launcher не реализован (Modrinth/CurseForge в Фазе B).
            progress.accept("ERROR: " + e.getMessage());
            return new InstallReport(
                    opt, Instant.now(),
                    List.of(),
                    List.of(),
                    List.of(e.getMessage()),
                    0L);
        } catch (RuntimeException e) {
            LOG.error("Unexpected installer failure", e);
            progress.accept("ERROR: " + e);
            return new InstallReport(
                    opt, Instant.now(),
                    List.of(),
                    List.of(),
                    List.of("Unexpected: " + e),
                    0L);
        }
    }

    /** {@code true} если запущены на Windows (оставлено для совместимости с тестами). */
    public static boolean isWindows() {
        return dev.vida.installer.launchers.InstallerSupport.isWindows();
    }
}
