/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import dev.vida.installer.cli.CliArgs;
import dev.vida.installer.cli.CliInstaller;
import dev.vida.installer.gui.InstallerFrame;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа installer'а.
 *
 * <p>Логика выбора режима:
 * <ul>
 *   <li>если в аргументах есть {@code --headless}, {@code --help}, {@code --version}
 *       или {@code --dry-run} — всегда CLI;</li>
 *   <li>иначе если {@code GraphicsEnvironment.isHeadless()} — тоже CLI;</li>
 *   <li>иначе — Swing-GUI.</li>
 * </ul>
 *
 * <p>Версия installer'а берётся из {@code Implementation-Version} своего
 * jar-манифеста (см. {@link #version()}).
 */
public final class InstallerMain {

    private static final Logger LOG = LoggerFactory.getLogger(InstallerMain.class);

    private InstallerMain() {}

    public static void main(String[] argv) {
        // Ловим всё, что могло просочиться мимо runGui/runCli — чтобы
        // пользователь, кликнувший по jar-у, никогда не оставался перед
        // «пустой зависшей» JVM без сообщения об ошибке.
        installCrashReporters();

        int code = run(argv);
        // В GUI-режиме run() успел только ПОКАЗАТЬ окно и вернул 0; EDT
        // (Event Dispatch Thread) — это не-daemon тред, он сам держит JVM
        // живым до закрытия окна, а InstallerFrame навесил
        // setDefaultCloseOperation(EXIT_ON_CLOSE), который при закрытии
        // вызовет System.exit(0) как положено.
        //
        // Если же мы пойдём по CLI-пути, run() вернётся уже ПОСЛЕ всей
        // установки — и тогда ненулевой код надо явно пробросить в JVM,
        // а нулевой — просто дать main'у завершиться (не-daemon тредов у
        // CLI нет, JVM остановится естественно).
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int run(String[] argv) {
        String installerVersion = version();
        LOG.info("vida-installer {} starting (args={})",
                installerVersion, Arrays.toString(argv));

        boolean forceCli = containsAny(argv, "--headless", "--help", "-h",
                "--version", "-V", "--list-instances", "--validate-puertas");
        boolean headlessEnv = GraphicsEnvironment.isHeadless();

        if (forceCli || headlessEnv) {
            return runCli(argv, installerVersion);
        }
        return runGui(installerVersion);
    }

    /**
     * Ставит JVM-глобальный handler: если EDT или любой другой поток упадёт
     * с неперехваченным throwable'ом — пишем полный stack-trace в файл
     * {@code %TEMP%/vida-installer-crash.log} и показываем модальный Swing-диалог
     * (если display доступен). Это гарантирует, что пользователь, запустивший
     * установщик через {@code javaw.exe} (иконка на рабочем столе / двойной клик),
     * не останется перед «пустой» JVM без каких-либо индикаций, что произошло.
     *
     * <p>На headless-системах диалог просто пропускается — всё остаётся в
     * crash-log'е и в stderr, где его увидит скрипт.
     */
    private static void installCrashReporters() {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, t) -> handleCrash("thread " + thread.getName(), t));
    }

    static void handleCrash(String origin, Throwable t) {
        String stack = formatStack(t);
        LOG.error("Installer crashed in {}:\n{}", origin, stack);

        Path crashLog = writeCrashLog(origin, stack);
        System.err.println();
        System.err.println("=== VIDA INSTALLER CRASHED (" + origin + ") ===");
        System.err.println(stack);
        if (crashLog != null) {
            System.err.println("Crash log: " + crashLog);
        }

        if (!GraphicsEnvironment.isHeadless()) {
            showCrashDialog(stack, crashLog);
        }
    }

    private static String formatStack(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** Best-effort файл с crash-логом в системном temp-dir. */
    private static Path writeCrashLog(String origin, String stack) {
        try {
            String tmp = System.getProperty("java.io.tmpdir", ".");
            Path p = Paths.get(tmp, "vida-installer-crash.log");
            String header = "# vida-installer " + version() + " crash at " + Instant.now()
                    + " (" + origin + ")\n\n";
            Files.writeString(p, header + stack + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return p;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void showCrashDialog(String stack, Path crashLog) {
        Runnable r = () -> {
            String body = "Installer failed to start.\n\n"
                    + (crashLog != null ? "Crash log: " + crashLog + "\n\n" : "")
                    + truncate(stack, 1_500);
            JOptionPane.showMessageDialog(null, body,
                    "Vida Installer — startup error",
                    JOptionPane.ERROR_MESSAGE);
        };
        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            try { SwingUtilities.invokeAndWait(r); }
            catch (Exception ignored) { /* ничего лучше уже не выжать */ }
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "\n…(truncated, see crash log)";
    }

    // ============================================================ CLI

    private static int runCli(String[] argv, String installerVersion) {
        try {
            CliArgs args = CliArgs.parse(argv, new McDirDetector(), installerVersion);
            return CliInstaller.run(args, installerVersion);
        } catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.err.println();
            System.err.println(CliArgs.helpText());
            return 1;
        }
    }

    // ============================================================ GUI

    private static int runGui(String installerVersion) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                InstallerFrame frame = new InstallerFrame(installerVersion);
                frame.setVisible(true);
            });
            return 0;
        } catch (java.awt.HeadlessException headless) {
            LOG.warn("Headless environment detected at GUI time, falling back to CLI.");
            return runCli(new String[]{"--headless", "--help"}, installerVersion);
        } catch (Exception e) {
            // invokeAndWait оборачивает исключение EDT в InvocationTargetException —
            // достаём внутреннее, чтобы пользователю показать настоящую причину.
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite
                    && ite.getTargetException() != null ? ite.getTargetException() : e;
            handleCrash("GUI init", cause);
            return 3;
        }
    }

    // ============================================================ helpers

    static boolean containsAny(String[] argv, String... wanted) {
        for (String a : argv) {
            for (String w : wanted) {
                if (w.equals(a)) return true;
            }
        }
        return false;
    }

    /** Читаем {@code Implementation-Version} из собственного jar-манифеста. */
    public static String version() {
        Package p = InstallerMain.class.getPackage();
        if (p != null) {
            String v = p.getImplementationVersion();
            if (v != null && !v.isBlank()) return v;
        }
        return "dev";
    }
}
