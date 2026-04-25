/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.cli;

import dev.vida.installer.InstallOptions;
import dev.vida.installer.McDirDetector;
import dev.vida.installer.launchers.LauncherKind;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Разбор аргументов CLI.
 *
 * <pre>
 * Usage: vida-installer [options]
 *   --help                   show help
 *   --version                show version
 *   --headless               force headless mode (no GUI)
 *   --launcher &lt;name&gt;        target launcher: minecraft | prism | multimc | atlauncher | modrinth | curseforge
 *   --list-instances         list instances in --dir for --launcher and exit
 *   --dir &lt;path&gt;             launcher data dir
 *   --instance &lt;path&gt;        path to existing instance (ATLauncher etc.)
 *   --instance-name &lt;str&gt;    new instance name (Prism/MultiMC)
 *   --minecraft &lt;ver&gt;        target Minecraft version (default: 1.21.1);
 *                                supported: 1.21.1+, 26.1.0–26.1.2, or 26.1.preview
 *   --loader-version &lt;v&gt;     loader version tag (default: from bundled jar)
 *   --no-launcher-profile    do not patch launcher_profiles.json (Mojang only)
 *   --no-launch-script       do not write standalone vida.bat/vida.sh (Mojang only)
 *   --dry-run                simulate, do not write files
 *   --overwrite              overwrite existing loader installation
 *   --yes, -y                skip confirmation prompts
 * </pre>
 */
public final class CliArgs {

    public enum Action { INSTALL, HELP, VERSION, LIST_INSTANCES, VALIDATE_PUERTAS }

    private final Action action;
    private final boolean headless;
    private final InstallOptions options;
    private final boolean assumeYes;
    private final Path validatePuertasPath;

    private CliArgs(Action action, boolean headless, InstallOptions options,
                    boolean assumeYes, Path validatePuertasPath) {
        this.action = action;
        this.headless = headless;
        this.options = options;
        this.assumeYes = assumeYes;
        this.validatePuertasPath = validatePuertasPath;
    }

    public Action action()                 { return action; }
    public boolean headless()              { return headless; }
    public InstallOptions options()        { return options; }
    public boolean assumeYes()             { return assumeYes; }
    public Path validatePuertasPath()      { return validatePuertasPath; }

    public static CliArgs parse(String[] argv, McDirDetector detector, String defaultLoaderVersion) {
        Objects.requireNonNull(argv, "argv");
        Objects.requireNonNull(detector, "detector");

        LauncherKind launcher = LauncherKind.MOJANG;
        boolean headless = false;
        boolean createLauncherProfile = true;
        boolean createLaunchScript = true;
        boolean dryRun = false;
        boolean overwrite = false;
        boolean assumeYes = false;
        String mc = "1.21.1";
        String loaderVersion = defaultLoaderVersion;
        Path dir = null;
        Path instance = null;
        Path validatePuertasPath = null;
        String instanceName = null;
        Action action = Action.INSTALL;

        for (int i = 0; i < argv.length; i++) {
            String a = argv[i];
            switch (a) {
                case "--help", "-h" -> action = Action.HELP;
                case "--version", "-V" -> action = Action.VERSION;
                case "--headless" -> headless = true;
                case "--list-instances" -> action = Action.LIST_INSTANCES;
                case "--no-launcher-profile" -> createLauncherProfile = false;
                case "--no-launch-script" -> createLaunchScript = false;
                case "--dry-run" -> dryRun = true;
                case "--overwrite" -> overwrite = true;
                case "--yes", "-y" -> assumeYes = true;
                case "--launcher" -> {
                    if (i + 1 >= argv.length) throw new IllegalArgumentException("--launcher requires a name");
                    launcher = LauncherKind.fromCli(argv[++i]);
                    if (!launcher.isImplemented()) {
                        throw new IllegalArgumentException("Launcher '" + launcher.cliName()
                                + "' is not yet implemented. Available: "
                                + String.join(", ", implementedCliNames()));
                    }
                }
                case "--dir" -> {
                    if (i + 1 >= argv.length) throw new IllegalArgumentException("--dir requires a path");
                    dir = Paths.get(argv[++i]);
                }
                case "--instance" -> {
                    if (i + 1 >= argv.length) throw new IllegalArgumentException("--instance requires a path");
                    instance = Paths.get(argv[++i]);
                }
                case "--instance-name" -> {
                    if (i + 1 >= argv.length) throw new IllegalArgumentException("--instance-name requires a value");
                    instanceName = argv[++i];
                }
                case "--minecraft" -> {
                    if (i + 1 >= argv.length) throw new IllegalArgumentException("--minecraft requires a version");
                    mc = argv[++i];
                }
                case "--loader-version" -> {
                    if (i + 1 >= argv.length) throw new IllegalArgumentException("--loader-version requires a value");
                    loaderVersion = argv[++i];
                }
                case "--validate-puertas" -> {
                    if (i + 1 >= argv.length)
                        throw new IllegalArgumentException("--validate-puertas requires a path");
                    validatePuertasPath = Paths.get(argv[++i]);
                    action = Action.VALIDATE_PUERTAS;
                }
                default -> {
                    if (a.startsWith("--") || a.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + a);
                    }
                    if (dir == null) {
                        dir = Paths.get(a);
                    } else {
                        throw new IllegalArgumentException("Unexpected argument: " + a);
                    }
                }
            }
        }

        if (dir == null) {
            dir = switch (launcher) {
                case MOJANG -> detector.defaultDir();
                default -> Paths.get("."); // для non-Mojang обязательно --dir; проверим ниже
            };
        }

        if (launcher != LauncherKind.MOJANG && dir.toString().equals(".")
                && action != Action.HELP && action != Action.VERSION) {
            throw new IllegalArgumentException(
                    "--dir is required when --launcher is not 'minecraft'");
        }

        InstallOptions opt = InstallOptions.builder()
                .launcherKind(launcher)
                .installDir(dir)
                .targetInstance(instance)
                .instanceName(instanceName)
                .minecraftVersion(mc)
                .loaderVersion(loaderVersion == null ? "unknown" : loaderVersion)
                .createLauncherProfile(createLauncherProfile)
                .createLaunchScript(createLaunchScript)
                .dryRun(dryRun)
                .overwrite(overwrite)
                .build();
        return new CliArgs(action, headless, opt, assumeYes, validatePuertasPath);
    }

    private static String[] implementedCliNames() {
        LauncherKind[] values = LauncherKind.values();
        int n = 0;
        for (LauncherKind k : values) if (k.isImplemented()) n++;
        String[] out = new String[n];
        int i = 0;
        for (LauncherKind k : values) if (k.isImplemented()) out[i++] = k.cliName();
        return out;
    }

    public static String helpText() {
        return """
                Vida Installer

                Usage: vida-installer [options]
                   --help, -h                   show this message
                   --version, -V                show installer version
                   --headless                   force headless mode (no GUI)
                   --launcher <name>            target launcher: minecraft | prism | multimc | atlauncher
                                                | modrinth | curseforge (default: minecraft)
                   --list-instances             list instances in --dir and exit
                   --dir <path>                 launcher data dir (required unless --launcher=minecraft)
                   --instance <path>            path to existing instance (ATLauncher requires this)
                   --instance-name <str>        new instance name (Prism/MultiMC)
                   --minecraft <ver>            target Minecraft version (default: 1.21.1):
                                                  1.21.1+, 26.1.0–26.1.2, or 26.1.preview
                   --loader-version <v>         loader version tag (default: from bundled jar)
                   --no-launcher-profile        do not patch launcher_profiles.json (Mojang only)
                   --no-launch-script           do not write vida.bat/vida.sh (Mojang only)
                   --dry-run                    simulate, do not write files
                   --overwrite                  overwrite existing loader installation
                   --yes, -y                    skip confirmation prompts
                   --validate-puertas <path>    parse and validate .ptr file(s) and exit
                                                (path can be a file or a directory)
                """;
    }
}
