/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import dev.vida.installer.InstallOptions;
import dev.vida.installer.InstallReport;
import dev.vida.installer.InstallerMain;
import dev.vida.installer.launchers.InstallMode;
import dev.vida.installer.launchers.InstallerSupport;
import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.launchers.LauncherHandler;
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
 * Общая реализация установки Vida в Prism Launcher или MultiMC.
 *
 * <p>Отличия двух лаунчеров скрыты в двух абстрактных методах:
 * {@link #detectDataDirs()} и {@link #supportsAgents()}. Всё остальное
 * полностью одинаково.
 */
public abstract class PrismLikeHandler implements LauncherHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PrismLikeHandler.class);

    @Override
    public Set<InstallMode> supportedModes() {
        return EnumSet.of(InstallMode.CREATE_NEW_PROFILE);
    }

    @Override
    public List<InstanceRef> listInstances(Path dataDir) throws IOException {
        return PrismInstanceScanner.list(dataDir);
    }

    /**
     * Prism ≥ 7.0 поддерживает компонент {@code +agents}; MultiMC — нет.
     * Возвращаемое значение определяет, как прописывается {@code -javaagent}:
     * через component patch или через {@code JvmArgs} в {@code instance.cfg}.
     */
    protected abstract boolean supportsAgents();

    @Override
    public InstallReport install(InstallOptions opt, Consumer<String> progress) {
        Consumer<String> p = progress != null ? progress : msg -> {};
        List<Path> installed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long loaderBytes = 0L;

        p.accept("Starting Vida install (" + kind().displayName() + ") → " + opt.installDir());

        try {
            loaderBytes = doInstall(opt, installed, warnings, p);
            p.accept("Install complete.");
            p.accept("Open " + kind().displayName() + " — new instance \""
                    + resolveDisplayName(opt) + "\" is ready.");
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

    // ------------------------------------------------------------------
    //  core flow
    // ------------------------------------------------------------------

    private long doInstall(InstallOptions opt, List<Path> installed,
                           List<String> warnings, Consumer<String> progress) throws IOException {
        Path dataDir = opt.installDir().toAbsolutePath().normalize();
        if (!Files.isDirectory(dataDir)) {
            throw new IOException(kind().displayName() + " data dir does not exist: " + dataDir
                    + ". Launch " + kind().displayName() + " at least once first.");
        }

        String safeName = resolveInstanceName(opt);
        String displayName = resolveDisplayName(opt);
        PrismInstanceLayout layout = new PrismInstanceLayout(
                dataDir, safeName, opt.minecraftVersion(), opt.loaderVersion());

        preflight(layout, opt);

        progress.accept("Creating instance directory tree → " + layout.instanceDir());
        if (!opt.dryRun()) {
            Files.createDirectories(layout.instanceDir());
            Files.createDirectories(layout.patchesDir());
            Files.createDirectories(layout.librariesDir());
            Files.createDirectories(layout.modsDir());
            Files.createDirectories(layout.vidaDir());
        }

        // 1) Вытаскиваем loader.jar в libraries/ (MMC-hint: "local").
        progress.accept("Extracting loader → " + layout.loaderJar());
        McArtifacts.Sha1Result loader =
                InstallerSupport.extractEmbeddedLoader(layout.loaderJar(), opt.dryRun());
        long loaderBytes = loader.sizeBytes();
        progress.accept("  wrote " + loaderBytes + " bytes, sha1=" + loader.sha1Hex());
        if (!opt.dryRun()) installed.add(layout.loaderJar());

        // 2) instance.cfg.
        progress.accept("Writing instance.cfg");
        PrismInstanceCfg cfg = new PrismInstanceCfg(displayName);
        if (!supportsAgents()) {
            // MultiMC / pre-7.0 Prism: инжектируем через JvmArgs.
            String jvmArg = "-javaagent:" + layout.loaderJar().toAbsolutePath();
            cfg.withJvmArgs(jvmArg);
            warnings.add("Using JvmArgs-based -javaagent for " + kind().displayName()
                    + " (no +agents support).");
        }
        if (!opt.dryRun()) {
            InstallerSupport.writeAtomically(layout.instanceCfg(), cfg.render());
            installed.add(layout.instanceCfg());
        }

        // 3) mmc-pack.json.
        progress.accept("Writing mmc-pack.json");
        PrismMmcPack pack = new PrismMmcPack(opt.minecraftVersion(), opt.loaderVersion());
        if (!opt.dryRun()) {
            InstallerSupport.writeAtomically(layout.mmcPackJson(), pack.render());
            installed.add(layout.mmcPackJson());
        }

        // 4) patches/dev.vida.loader.json.
        progress.accept("Writing component patch → " + layout.componentPatch());
        PrismComponentPatch patch = new PrismComponentPatch(
                opt.minecraftVersion(), opt.loaderVersion(),
                layout.libraryMavenCoord(), supportsAgents());
        if (!opt.dryRun()) {
            InstallerSupport.writeAtomically(layout.componentPatch(), patch.render(Instant.now()));
            installed.add(layout.componentPatch());
        }

        // 5) install.json — audit-inventory.
        Path manifest = writeInstallManifest(layout, opt, installed, loader, progress);
        if (manifest != null) installed.add(manifest);

        return loaderBytes;
    }

    private void preflight(PrismInstanceLayout layout, InstallOptions opt) throws IOException {
        Path instanceDir = layout.instanceDir();
        if (Files.isRegularFile(instanceDir)) {
            throw new IOException(instanceDir + " exists and is a file");
        }
        if (Files.isDirectory(instanceDir) && !opt.overwrite()) {
            throw new IOException("Instance \"" + layout.instanceName()
                    + "\" already exists at " + instanceDir
                    + " — use overwrite=true to replace, or pick a different instance name.");
        }
    }

    private static String resolveInstanceName(InstallOptions opt) {
        if (opt.instanceName() != null && !opt.instanceName().isBlank()) {
            return PrismInstanceLayout.sanitizeInstanceName(opt.instanceName());
        }
        return PrismInstanceLayout.defaultInstanceName(
                opt.minecraftVersion(), opt.loaderVersion());
    }

    /**
     * Display-name, который попадёт в {@code instance.cfg} → поле {@code name=}.
     * Пользовательский ввод не санитизируется (лаунчеры принимают почти любой
     * Unicode в display-name), только trim'ится.
     */
    private static String resolveDisplayName(InstallOptions opt) {
        if (opt.instanceName() != null && !opt.instanceName().isBlank()) {
            return opt.instanceName().trim();
        }
        return "Vida " + opt.minecraftVersion() + " (" + opt.loaderVersion() + ")";
    }

    // ------------------------------------------------------------------
    //  install.json
    // ------------------------------------------------------------------

    private Path writeInstallManifest(PrismInstanceLayout layout, InstallOptions opt,
                                      List<Path> installed, McArtifacts.Sha1Result loader,
                                      Consumer<String> progress) throws IOException {
        Path manifestFile = layout.vidaInstallJson();
        Path root = layout.instanceDir();

        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("schema", 2L);
        tree.put("installedAt", Instant.now().toString());
        tree.put("installerVersion", InstallerMain.version());
        tree.put("launcherKind", kind().cliName());
        tree.put("instanceName", layout.instanceName());
        tree.put("minecraftVersion", layout.minecraftVersion());
        tree.put("loaderVersion", layout.loaderVersion());
        tree.put("agentsMode", supportsAgents() ? "components" : "jvmArgs");

        Map<String, Object> loaderMap = new LinkedHashMap<>();
        loaderMap.put("path", root.relativize(layout.loaderJar()).toString().replace('\\', '/'));
        loaderMap.put("sha1", loader.sha1Hex());
        loaderMap.put("size", loader.sizeBytes());
        tree.put("loader", loaderMap);

        List<Object> files = new ArrayList<>();
        for (Path f : installed) {
            files.add(root.relativize(f).toString().replace('\\', '/'));
        }
        files.add(root.relativize(manifestFile).toString().replace('\\', '/'));
        tree.put("files", files);

        String json = JsonTree.write(tree);
        progress.accept("Writing install manifest → " + manifestFile);
        if (!opt.dryRun()) {
            Files.createDirectories(manifestFile.getParent());
            InstallerSupport.writeAtomically(manifestFile, json);
        }
        return manifestFile;
    }
}
