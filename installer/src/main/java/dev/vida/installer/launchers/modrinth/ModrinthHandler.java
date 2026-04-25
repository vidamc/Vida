/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.modrinth;

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
import dev.vida.installer.mc.McArtifacts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler для Modrinth App.
 *
 * <p>Modrinth App хранит профили в SQLite {@code app.db} (а не в
 * файловой системе), а игровые файлы — в {@code profiles/<id>/}.
 *
 * <p>Процедура установки:
 * <ol>
 *   <li>Пользователь уже создал профиль в Modrinth App.</li>
 *   <li>Мы читаем {@code app.db} для получения списка профилей.</li>
 *   <li>Копируем loader.jar в {@code profiles/<id>/vida/}.</li>
 *   <li>Обновляем {@code java_args} в {@code app.db} с
 *       {@code -javaagent:&lt;path&gt;}.</li>
 *   <li>Пишем {@code install.json} — audit-inventory.</li>
 * </ol>
 */
public final class ModrinthHandler implements LauncherHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ModrinthHandler.class);

    @Override
    public LauncherKind kind() { return LauncherKind.MODRINTH; }

    @Override
    public Set<InstallMode> supportedModes() {
        return EnumSet.of(InstallMode.PATCH_EXISTING_INSTANCE);
    }

    @Override
    public List<Path> detectDataDirs() {
        OsPaths os = OsPaths.system();
        Path candidate = os.modrinth();
        List<Path> out = new ArrayList<>();
        if (candidate != null && Files.isDirectory(candidate)
                && Files.isRegularFile(candidate.resolve("app.db"))) {
            out.add(candidate);
        }
        return out;
    }

    @Override
    public List<InstanceRef> listInstances(Path dataDir) throws IOException {
        return ModrinthDbReader.listProfiles(dataDir);
    }

    @Override
    public InstallReport install(InstallOptions opt, Consumer<String> progress) {
        Consumer<String> p = progress != null ? progress : msg -> {};
        List<Path> installed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long loaderBytes = 0L;

        p.accept("Starting Vida install (Modrinth App) → " + opt.targetInstance());

        try {
            loaderBytes = doInstall(opt, installed, warnings, p);
            p.accept("Install complete.");
            p.accept("Restart Modrinth App to apply changes.");
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

    private long doInstall(InstallOptions opt, List<Path> installed,
                           List<String> warnings, Consumer<String> progress) throws IOException {
        Path instanceDir = opt.targetInstance();
        if (instanceDir == null) {
            throw new IOException("Modrinth requires a target instance "
                    + "(pass --instance <path> or pick one in GUI).");
        }
        instanceDir = instanceDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(instanceDir)) {
            throw new IOException("Instance dir does not exist: " + instanceDir);
        }

        Path dataDir = opt.installDir().toAbsolutePath().normalize();
        Path db = dataDir.resolve("app.db");
        if (!Files.isRegularFile(db)) {
            throw new IOException("Modrinth app.db not found in " + dataDir);
        }

        Path vidaDir = instanceDir.resolve("vida");
        Path loaderJar = vidaDir.resolve("vida-loader-" + opt.loaderVersion() + ".jar");

        if (Files.exists(loaderJar) && !opt.overwrite()) {
            throw new IOException("Vida loader already installed at " + loaderJar
                    + " — use overwrite=true to replace.");
        }

        progress.accept("Creating " + vidaDir);
        if (!opt.dryRun()) Files.createDirectories(vidaDir);

        progress.accept("Extracting loader → " + loaderJar);
        McArtifacts.Sha1Result loader =
                InstallerSupport.extractEmbeddedLoader(loaderJar, opt.dryRun());
        long loaderBytes = loader.sizeBytes();
        progress.accept("  wrote " + loaderBytes + " bytes, sha1=" + loader.sha1Hex());
        if (!opt.dryRun()) installed.add(loaderJar);

        String profilePath = resolveProfilePath(dataDir, instanceDir);
        String agentArg = "-javaagent:" + loaderJar.toAbsolutePath();

        progress.accept("Patching app.db for profile '" + profilePath + "'");
        if (!opt.dryRun()) {
            boolean updated = ModrinthDbReader.patchJavaArgs(dataDir, profilePath, agentArg,
                    opt.minecraftVersion(), opt.loaderVersion());
            if (!updated) {
                warnings.add("Profile '" + profilePath + "' not found in app.db — "
                        + "java_args not updated. You may need to add -javaagent manually.");
            } else {
                progress.accept("  java_args updated in app.db.");
            }
        }

        Path manifest = writeInstallManifest(instanceDir, opt, installed, loader, progress);
        if (manifest != null) installed.add(manifest);

        return loaderBytes;
    }

    private static String resolveProfilePath(Path dataDir, Path instanceDir) {
        Path profilesDir = dataDir.resolve("profiles");
        if (instanceDir.startsWith(profilesDir)) {
            return profilesDir.relativize(instanceDir).toString().replace('\\', '/');
        }
        return instanceDir.getFileName().toString();
    }

    private Path writeInstallManifest(Path instanceDir, InstallOptions opt,
                                      List<Path> installed, McArtifacts.Sha1Result loader,
                                      Consumer<String> progress) throws IOException {
        Path manifest = instanceDir.resolve("vida").resolve("install.json");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 2L);
        root.put("installedAt", Instant.now().toString());
        root.put("installerVersion", InstallerMain.version());
        root.put("launcherKind", kind().cliName());
        root.put("instancePath", instanceDir.toString().replace('\\', '/'));
        root.put("minecraftVersion", opt.minecraftVersion());
        root.put("loaderVersion", opt.loaderVersion());
        root.put("agentsMode", "sqliteJavaArgs");

        Map<String, Object> loaderMap = new LinkedHashMap<>();
        loaderMap.put("sha1", loader.sha1Hex());
        loaderMap.put("size", loader.sizeBytes());
        root.put("loader", loaderMap);

        List<Object> files = new ArrayList<>();
        for (Path f : installed) {
            files.add(instanceDir.relativize(f).toString().replace('\\', '/'));
        }
        files.add(instanceDir.relativize(manifest).toString().replace('\\', '/'));
        root.put("files", files);

        String json = JsonTree.write(root);
        progress.accept("Writing install manifest → " + manifest);
        if (!opt.dryRun()) {
            Files.createDirectories(manifest.getParent());
            InstallerSupport.writeAtomically(manifest, json);
        }
        return manifest;
    }
}
