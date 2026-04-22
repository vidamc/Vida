/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.cli;

import dev.vida.installer.InstallReport;
import dev.vida.installer.InstallerCore;
import dev.vida.installer.JavaRuntimeCheck;
import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.launchers.LauncherHandler;
import dev.vida.installer.launchers.LauncherKind;
import dev.vida.installer.launchers.LauncherRegistry;
import dev.vida.puertas.PuertaError;
import dev.vida.puertas.PuertaParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Headless-runner инсталлятора.
 *
 * <p>Печатает прогресс в {@code System.out}, возвращает int-код (0 при
 * успехе, 1 при пользовательской ошибке, 2 при I/O).
 */
public final class CliInstaller {

    private CliInstaller() {}

    public static int run(CliArgs args, String installerVersion) {
        switch (args.action()) {
            case HELP    -> { System.out.println(CliArgs.helpText()); return 0; }
            case VERSION -> { System.out.println("vida-installer " + installerVersion); return 0; }
            case LIST_INSTANCES -> { return listInstances(args); }
            case VALIDATE_PUERTAS -> { return validatePuertas(args.validatePuertasPath()); }
            default      -> {}
        }

        JavaRuntimeCheck.Result jre = JavaRuntimeCheck.checkCurrent();
        System.out.println("[runtime] " + jre.message());
        if (!jre.ok()) {
            System.out.println("[runtime] WARNING: Vida will not start on this JVM,"
                    + " but the files will still be written.");
        }

        Path dir = args.options().installDir();
        LauncherKind kind = args.options().launcherKind();
        System.out.println("[plan] launcher:        " + kind.displayName() + " (" + kind.cliName() + ")");
        System.out.println("[plan] install dir:     " + dir);
        if (args.options().targetInstance() != null) {
            System.out.println("[plan] target instance: " + args.options().targetInstance());
        }
        if (args.options().instanceName() != null) {
            System.out.println("[plan] instance name:   " + args.options().instanceName());
        }
        System.out.println("[plan] minecraft:       " + args.options().minecraftVersion());
        System.out.println("[plan] loader:          " + args.options().loaderVersion());
        if (kind == LauncherKind.MOJANG) {
            System.out.println("[plan] launcher-prof:   " + args.options().createLauncherProfile());
            System.out.println("[plan] launch-script:   " + args.options().createLaunchScript());
        }
        System.out.println("[plan] dry-run:         " + args.options().dryRun());
        System.out.println("[plan] overwrite:       " + args.options().overwrite());

        if (!args.assumeYes() && !args.options().dryRun() && !confirm()) {
            System.out.println("aborted by user");
            return 1;
        }

        InstallerCore core = new InstallerCore(msg -> System.out.println("[install] " + msg));
        InstallReport rep = core.install(args.options());

        for (String w : rep.warnings()) {
            System.out.println("[warn] " + w);
        }

        if (rep.isOk()) {
            System.out.println("[ok] wrote " + rep.installedFiles().size() + " files"
                    + " (" + rep.loaderBytes() + " bytes loader).");
            return 0;
        }
        for (String err : rep.errors()) {
            System.err.println("[err] " + err);
        }
        return 2;
    }

    // ------------------------------------------------------------------
    //  --list-instances
    // ------------------------------------------------------------------

    private static int listInstances(CliArgs args) {
        LauncherKind kind = args.options().launcherKind();
        if (!LauncherRegistry.isAvailable(kind)) {
            System.err.println("[err] launcher not implemented: " + kind.cliName());
            return 1;
        }
        LauncherHandler handler = LauncherRegistry.forKind(kind);

        List<Path> dataDirs;
        if (args.options().installDir() != null
                && !args.options().installDir().toString().equals(".")) {
            dataDirs = List.of(args.options().installDir());
        } else {
            dataDirs = handler.detectDataDirs();
            if (dataDirs.isEmpty()) {
                System.err.println("[err] no data dirs found for " + kind.displayName()
                        + " — pass --dir <path>");
                return 1;
            }
        }

        int total = 0;
        for (Path dir : dataDirs) {
            System.out.println("# " + kind.displayName() + " @ " + dir);
            try {
                List<InstanceRef> refs = handler.listInstances(dir);
                if (refs.isEmpty()) {
                    System.out.println("  (no instances)");
                    continue;
                }
                for (InstanceRef r : refs) {
                    String loader = r.loader().map(l -> " " + l
                            + r.loaderVersion().map(v -> "=" + v).orElse(""))
                            .orElse(" vanilla");
                    System.out.printf("  %-32s  mc=%-10s%s%n",
                            r.id(),
                            r.minecraftVersion().isEmpty() ? "?" : r.minecraftVersion(),
                            loader);
                    total++;
                }
            } catch (IOException e) {
                System.err.println("[err] " + e.getMessage());
            }
        }
        System.out.println("# total: " + total + " instance(s)");
        return 0;
    }

    // ------------------------------------------------------------------
    //  --validate-puertas
    // ------------------------------------------------------------------

    private static int validatePuertas(Path target) {
        if (target == null) {
            System.err.println("[err] --validate-puertas requires a path");
            return 1;
        }
        if (!Files.exists(target)) {
            System.err.println("[err] path not found: " + target);
            return 1;
        }
        List<Path> files = new ArrayList<>();
        try {
            if (Files.isDirectory(target)) {
                try (Stream<Path> st = Files.walk(target)) {
                    st.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().endsWith(".ptr"))
                            .sorted(Comparator.naturalOrder())
                            .forEach(files::add);
                }
                if (files.isEmpty()) {
                    System.out.println("[info] no .ptr files under " + target);
                    return 0;
                }
            } else {
                files.add(target);
            }
        } catch (IOException e) {
            System.err.println("[err] I/O walking " + target + ": " + e.getMessage());
            return 2;
        }

        int fallidos = 0;
        int directivas = 0;
        for (Path p : files) {
            try {
                var res = PuertaParser.parsear(p);
                if (res.esExitoso()) {
                    directivas += res.archivo().directivas().size();
                    System.out.println("[ok] " + p + " — " + res.archivo().directivas().size()
                            + " directiva(s), namespace=" + res.archivo().namespace());
                } else {
                    fallidos++;
                    System.err.println("[fail] " + p + " (" + res.errores().size() + " error(es)):");
                    for (PuertaError err : res.errores()) {
                        System.err.println("   - " + err);
                    }
                }
            } catch (IOException e) {
                fallidos++;
                System.err.println("[err] " + p + ": " + e.getMessage());
            }
        }
        System.out.println("[summary] " + files.size() + " file(s), "
                + directivas + " directiva(s), " + fallidos + " failure(s)");
        return fallidos == 0 ? 0 : 2;
    }

    private static boolean confirm() {
        System.out.print("Proceed with install? [y/N] ");
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line = r.readLine();
            return line != null && (line.equalsIgnoreCase("y") || line.equalsIgnoreCase("yes"));
        } catch (Exception e) {
            return false;
        }
    }
}
