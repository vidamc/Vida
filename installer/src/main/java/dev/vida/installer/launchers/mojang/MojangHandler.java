/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.mojang;

import dev.vida.installer.InstallOptions;
import dev.vida.installer.InstallReport;
import dev.vida.installer.InstallerMain;
import dev.vida.installer.launchers.InstallMode;
import dev.vida.installer.launchers.InstallerSupport;
import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.launchers.LauncherHandler;
import dev.vida.installer.launchers.LauncherKind;
import dev.vida.installer.launchers.OsPaths;
import dev.vida.installer.mc.JsonTree;
import dev.vida.installer.mc.LauncherProfilesPatcher;
import dev.vida.installer.mc.McArtifacts;
import dev.vida.installer.mc.McLayout;
import dev.vida.installer.mc.VersionJson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler для официального Mojang Launcher (версия 2.x).
 *
 * <p>Раскладка стандартного {@code .minecraft}:
 * <pre>
 * .minecraft/
 *   libraries/dev/vida/vida-loader/&lt;ver&gt;/vida-loader-&lt;ver&gt;.jar
 *   versions/vida-&lt;mc&gt;-&lt;ver&gt;/
 *     vida-&lt;mc&gt;-&lt;ver&gt;.json   ← inheritsFrom + -javaagent в arguments.jvm
 *     vida-&lt;mc&gt;-&lt;ver&gt;.jar    ← пустой jar-маркер
 *   launcher_profiles.json     ← патчится, профиль «Vida 1.21.1 (ver)»
 *   mods/
 *   vida/
 *     config/, logs/
 *     install.json             ← audit-inventory
 *     vida.bat / vida.sh       ← standalone-скрипт (опц.)
 * </pre>
 *
 * <p>Поддерживает только {@link InstallMode#CREATE_NEW_PROFILE}.
 */
public final class MojangHandler implements LauncherHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MojangHandler.class);

    public static final int INSTALL_MANIFEST_SCHEMA = 2;

    @Override
    public LauncherKind kind() { return LauncherKind.MOJANG; }

    @Override
    public Set<InstallMode> supportedModes() {
        return EnumSet.of(InstallMode.CREATE_NEW_PROFILE);
    }

    @Override
    public List<Path> detectDataDirs() {
        Path p = OsPaths.system().minecraft();
        return Files.isDirectory(p) ? List.of(p) : List.of();
    }

    @Override
    public List<InstanceRef> listInstances(Path dataDir) throws IOException {
        // Mojang не имеет «instance'ов» в обычном понимании, но у него есть
        // версии в versions/. Для UX показываем их как «instance-подобные»
        // ссылки, чтобы GUI мог дать выбор.
        Path versions = dataDir.resolve("versions");
        if (!Files.isDirectory(versions)) return List.of();

        List<InstanceRef> refs = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(versions, Files::isDirectory)) {
            for (Path v : ds) {
                String id = v.getFileName().toString();
                String mc = extractMcVersion(v, id);
                refs.add(new InstanceRef(
                        id, id, v, mc, Optional.empty(), Optional.empty()));
            }
        }
        return refs;
    }

    @Override
    public InstallReport install(InstallOptions opt, Consumer<String> progress) {
        Consumer<String> p = progress != null ? progress : msg -> {};
        List<Path> installed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long loaderBytes = 0L;

        p.accept("Starting Vida install (Mojang) → " + opt.installDir());

        try {
            loaderBytes = doInstall(opt, installed, warnings, p);
            p.accept("Install complete.");
        } catch (IOException e) {
            errors.add("I/O: " + e.getMessage());
            LOG.error("Installer I/O failure", e);
            p.accept("ERROR (I/O): " + e.getMessage());
        } catch (RuntimeException e) {
            errors.add("Unexpected: " + e);
            LOG.error("Unexpected installer failure", e);
            p.accept("ERROR: " + e);
        }
        return new InstallReport(opt, Instant.now(), installed, warnings, errors, loaderBytes);
    }

    // ============================================================ internals

    private long doInstall(InstallOptions opt, List<Path> installed,
                           List<String> warnings, Consumer<String> progress) throws IOException {
        Path mcDir = opt.installDir().toAbsolutePath().normalize();
        McLayout layout = new McLayout(mcDir, opt.minecraftVersion(), opt.loaderVersion());

        preflight(layout, opt);

        progress.accept("Creating directories…");
        if (!opt.dryRun()) {
            Files.createDirectories(layout.libraryJar().getParent());
            Files.createDirectories(layout.versionDir());
            Files.createDirectories(layout.modsDir());
            Files.createDirectories(layout.vidaConfigDir());
            Files.createDirectories(layout.vidaLogsDir());
        }

        // 1) loader в libraries/ (Maven-раскладка).
        progress.accept("Extracting loader → " + layout.libraryJar());
        McArtifacts.Sha1Result loader =
                InstallerSupport.extractEmbeddedLoader(layout.libraryJar(), opt.dryRun());
        long loaderBytes = loader.sizeBytes();
        progress.accept("  wrote " + loaderBytes + " bytes, sha1=" + loader.sha1Hex());
        if (!opt.dryRun()) installed.add(layout.libraryJar());

        // 2) versions/<id>/<id>.json + пустой <id>.jar-маркер.
        progress.accept("Writing version JSON → " + layout.versionJson());
        String versionJson = VersionJson.render(new VersionJson.Params(
                layout, loader.sha1Hex(), loaderBytes, Instant.now()));
        if (!opt.dryRun()) {
            InstallerSupport.writeAtomically(layout.versionJson(), versionJson);
            installed.add(layout.versionJson());
            McArtifacts.writeEmptyJar(layout.versionJar());
            installed.add(layout.versionJar());
        }

        // 3) Патчим launcher_profiles.json.
        if (opt.createLauncherProfile()) {
            progress.accept("Patching launcher_profiles.json");
            LauncherProfilesPatcher.Result r = LauncherProfilesPatcher.patch(
                    layout.launcherProfiles(), layout, Instant.now(), opt.dryRun());
            if (r.profileExisted()) {
                progress.accept("  updated existing Vida profile.");
            } else if (r.fileExisted()) {
                progress.accept("  added Vida profile to existing launcher_profiles.");
            } else {
                progress.accept("  launcher_profiles.json did not exist, created fresh.");
            }
            if (r.backupCreated()) {
                warnings.add("launcher_profiles.json was unreadable; backup at " + r.backupPath());
            }
            if (!opt.dryRun()) installed.add(layout.launcherProfiles());
        }

        // 4) Launch-скрипт.
        Path launchScript = writeLaunchScript(layout, opt, warnings, progress);
        if (launchScript != null) installed.add(launchScript);

        // 5) install.json — audit-inventory.
        Path manifest = writeInstallManifest(layout, opt, installed, loader, progress);
        if (manifest != null) installed.add(manifest);

        return loaderBytes;
    }

    private void preflight(McLayout layout, InstallOptions opt) throws IOException {
        if (Files.isRegularFile(layout.vidaDir())) {
            throw new IOException(layout.vidaDir() + " exists and is a file");
        }
        if (Files.exists(layout.libraryJar()) && !opt.overwrite()) {
            throw new IOException("Vida loader already installed at "
                    + layout.libraryJar() + " — use overwrite=true to replace.");
        }
        if (Files.exists(layout.versionJson()) && !opt.overwrite()) {
            throw new IOException("Vida version profile already exists at "
                    + layout.versionJson() + " — use overwrite=true to replace.");
        }
    }

    // ----------------------------------------------------------------
    //  launch-script (standalone)
    // ----------------------------------------------------------------

    private Path writeLaunchScript(McLayout layout, InstallOptions opt,
                                   List<String> warnings, Consumer<String> progress) throws IOException {
        if (!opt.createLaunchScript()) return null;
        boolean windows = InstallerSupport.isWindows();
        Path script = layout.vidaDir().resolve(windows ? "vida.bat" : "vida.sh");
        progress.accept("Writing standalone launch script → " + script);
        String content = buildLaunchScript(layout, windows);
        if (opt.dryRun()) return script;
        Files.createDirectories(script.getParent());
        Files.writeString(script, content, StandardCharsets.UTF_8);
        if (!windows) {
            try {
                Files.setPosixFilePermissions(script,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
            } catch (UnsupportedOperationException ignore) {
                warnings.add("filesystem does not support POSIX perms; set +x manually");
            }
        }
        return script;
    }

    private static String buildLaunchScript(McLayout layout, boolean windows) {
        String agent = layout.libraryJar().toAbsolutePath().toString();
        String mods  = layout.modsDir().toAbsolutePath().toString();
        String conf  = layout.vidaConfigDir().toAbsolutePath().toString();
        String mcVer = layout.minecraftVersion();
        String lvVer = layout.loaderVersion();
        if (windows) {
            return """
                    @echo off
                    rem Vida standalone launch script — generated by installer.
                    rem Minecraft: %MC%, loader: %LV%
                    rem Usage: vida.bat <classpath> <mainClass> [args...]
                    java -javaagent:"%AGENT%" ^
                         -Dvida.mods="%MODS%" ^
                         -Dvida.config="%CONF%" ^
                         -Dvida.loader.version=%LV% ^
                         -Dvida.minecraft.version=%MC% ^
                         -cp %%1 %%2 %%*
                    """
                    .replace("%MC%",    mcVer)
                    .replace("%LV%",    lvVer)
                    .replace("%AGENT%", agent)
                    .replace("%MODS%",  mods)
                    .replace("%CONF%",  conf);
        }
        return """
                #!/usr/bin/env sh
                # Vida standalone launch script — generated by installer.
                # Minecraft: %MC%, loader: %LV%
                # Usage: vida.sh <classpath> <mainClass> [args...]
                set -e
                exec java \\
                    -javaagent:'%AGENT%' \\
                    -Dvida.mods='%MODS%' \\
                    -Dvida.config='%CONF%' \\
                    -Dvida.loader.version=%LV% \\
                    -Dvida.minecraft.version=%MC% \\
                    -cp "$1" "$2" "$@"
                """
                .replace("%MC%",    mcVer)
                .replace("%LV%",    lvVer)
                .replace("%AGENT%", agent)
                .replace("%MODS%",  mods)
                .replace("%CONF%",  conf);
    }

    // ----------------------------------------------------------------
    //  install.json
    // ----------------------------------------------------------------

    private Path writeInstallManifest(McLayout layout, InstallOptions opt,
                                      List<Path> installed, McArtifacts.Sha1Result loader,
                                      Consumer<String> progress) throws IOException {
        Path manifestFile = layout.vidaInstallJson();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", (long) INSTALL_MANIFEST_SCHEMA);
        root.put("installedAt", Instant.now().toString());
        root.put("installerVersion", InstallerMain.version());
        root.put("launcherKind", opt.launcherKind().cliName());
        root.put("minecraftVersion", layout.minecraftVersion());
        root.put("loaderVersion", layout.loaderVersion());
        root.put("profileId", layout.profileId());

        Map<String, Object> loaderMap = new LinkedHashMap<>();
        loaderMap.put("path", layout.mcDir().relativize(layout.libraryJar())
                .toString().replace('\\', '/'));
        loaderMap.put("sha1", loader.sha1Hex());
        loaderMap.put("size", loader.sizeBytes());
        root.put("loader", loaderMap);

        List<Object> files = new ArrayList<>();
        for (Path f : installed) {
            files.add(layout.mcDir().relativize(f).toString().replace('\\', '/'));
        }
        files.add(layout.mcDir().relativize(manifestFile).toString().replace('\\', '/'));
        root.put("files", files);

        String json = JsonTree.write(root);
        progress.accept("Writing install manifest → " + manifestFile);
        if (!opt.dryRun()) {
            Files.createDirectories(manifestFile.getParent());
            InstallerSupport.writeAtomically(manifestFile, json);
        }
        return manifestFile;
    }

    // ----------------------------------------------------------------
    //  helpers
    // ----------------------------------------------------------------

    /** Пытается прочитать {@code versions/<id>/<id>.json} и достать Minecraft-версию. */
    @SuppressWarnings("unchecked")
    private static String extractMcVersion(Path versionDir, String id) {
        Path json = versionDir.resolve(id + ".json");
        if (!Files.isReadable(json)) return "";
        try {
            Object tree = JsonTree.parse(Files.readString(json, StandardCharsets.UTF_8));
            if (tree instanceof Map<?, ?> map) {
                Object inherits = ((Map<String, Object>) map).get("inheritsFrom");
                if (inherits instanceof String s) return s;
                Object idv = ((Map<String, Object>) map).get("id");
                if (idv instanceof String s) return s;
            }
        } catch (IOException | RuntimeException ignore) {
            // Поломанный JSON — ок, просто не показываем версию.
        }
        return "";
    }
}
