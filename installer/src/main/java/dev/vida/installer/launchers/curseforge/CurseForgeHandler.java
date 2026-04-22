/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.curseforge;

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
 * Handler для CurseForge App.
 *
 * <p>CurseForge App хранит инстансы в {@code <base>/Instances/<name>/}
 * и описывает каждый инстанс через {@code minecraftinstance.json}.
 * Поле {@code javaArgsOverride} — недокументированное, но рабочее —
 * позволяет инжектить {@code -javaagent}.
 *
 * <p>Так же, как ATLauncher, CurseForge перезаписывает внутренние
 * конфиги при обновлениях, поэтому создание инстанса с нуля
 * нецелесообразно — поддерживается только PATCH.
 */
public final class CurseForgeHandler implements LauncherHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CurseForgeHandler.class);

    @Override
    public LauncherKind kind() { return LauncherKind.CURSEFORGE; }

    @Override
    public Set<InstallMode> supportedModes() {
        return EnumSet.of(InstallMode.PATCH_EXISTING_INSTANCE);
    }

    @Override
    public List<Path> detectDataDirs() {
        OsPaths os = OsPaths.system();
        Path candidate = os.curseforgeInstances();
        if (candidate != null && Files.isDirectory(candidate)) {
            return List.of(candidate);
        }
        return List.of();
    }

    @Override
    public List<InstanceRef> listInstances(Path dataDir) throws IOException {
        return CurseForgeInstanceScanner.list(dataDir);
    }

    @Override
    public InstallReport install(InstallOptions opt, Consumer<String> progress) {
        Consumer<String> p = progress != null ? progress : msg -> {};
        List<Path> installed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long loaderBytes = 0L;

        p.accept("Starting Vida install (CurseForge) → " + opt.targetInstance());

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

    private long doInstall(InstallOptions opt, List<Path> installed,
                           List<String> warnings, Consumer<String> progress) throws IOException {
        Path instanceDir = opt.targetInstance();
        if (instanceDir == null) {
            throw new IOException("CurseForge requires a target instance "
                    + "(pass --instance <path> or pick one in GUI).");
        }
        instanceDir = instanceDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(instanceDir)) {
            throw new IOException("Instance dir does not exist: " + instanceDir);
        }

        Path instanceJson = instanceDir.resolve("minecraftinstance.json");
        if (!Files.isRegularFile(instanceJson)) {
            throw new IOException(
                    "Not a CurseForge instance (missing minecraftinstance.json): " + instanceDir);
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
        McArtifacts.Sha1Result loader = InstallerSupport.extractEmbeddedLoader(loaderJar, opt.dryRun());
        long loaderBytes = loader.sizeBytes();
        progress.accept("  wrote " + loaderBytes + " bytes, sha1=" + loader.sha1Hex());
        if (!opt.dryRun()) installed.add(loaderJar);

        progress.accept("Patching " + instanceJson);
        if (!opt.dryRun()) {
            CurseForgeJsonPatcher.Result r =
                    CurseForgeJsonPatcher.patch(instanceJson, loaderJar.toString());
            installed.add(instanceJson);
            if (r.alreadyAgent()) {
                progress.accept("  replaced existing Vida agent path.");
            } else {
                progress.accept("  set javaArgsOverride with -javaagent.");
            }
            if (r.previousArgs() != null && !r.previousArgs().isBlank()) {
                progress.accept("  previous javaArgsOverride preserved: " + r.previousArgs());
            }
        }

        Path manifest = writeInstallManifest(instanceDir, opt, installed, loader, progress);
        if (manifest != null) installed.add(manifest);

        return loaderBytes;
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
        root.put("agentsMode", "javaArgsOverride");

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
