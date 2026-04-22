/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.atlauncher;

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
 * Handler для ATLauncher.
 *
 * <p>ATLauncher при каждом запуске инстанса перезаписывает {@code instance.json}
 * и потому <b>создание instance'а с нуля из внешнего процесса бесполезно</b>
 * — лаунчер сотрёт наши поля на следующем launch'е. Поэтому единственный
 * поддерживаемый режим — {@link InstallMode#PATCH_EXISTING_INSTANCE}.
 *
 * <p>Процедура:
 * <ol>
 *   <li>Пользователь уже создал instance в ATLauncher (Fabric/Forge на нужной MC-версии).</li>
 *   <li>Мы принимаем путь к этому instance'у в {@link InstallOptions#targetInstance()}.</li>
 *   <li>Копируем loader.jar в {@code <instance>/vida/vida-loader-&lt;ver&gt;.jar} (путь
 *       без пробелов — ATLauncher splitting javaArguments по пробелу).</li>
 *   <li>Добавляем {@code -javaagent:&lt;abs path&gt;} в {@code launcher.javaArguments}
 *       через {@link ATLauncherJsonPatcher}.</li>
 *   <li>Пишем {@code <instance>/vida/install.json} — audit inventory.</li>
 *   <li>Показываем пользователю инструкцию: перезапустить ATLauncher, чтобы
 *       он перечитал instance.json (нет file-watcher'а).</li>
 * </ol>
 */
public final class ATLauncherHandler implements LauncherHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ATLauncherHandler.class);

    @Override
    public LauncherKind kind() { return LauncherKind.ATLAUNCHER; }

    @Override
    public Set<InstallMode> supportedModes() {
        return EnumSet.of(InstallMode.PATCH_EXISTING_INSTANCE);
    }

    @Override
    public List<Path> detectDataDirs() {
        Path[] candidates = OsPaths.system().atlauncherCandidates();
        List<Path> out = new ArrayList<>();
        for (Path p : candidates) {
            if (Files.isDirectory(p) && Files.isDirectory(p.resolve("instances"))) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public List<InstanceRef> listInstances(Path dataDir) throws IOException {
        return ATLauncherInstanceScanner.list(dataDir);
    }

    @Override
    public InstallReport install(InstallOptions opt, Consumer<String> progress) {
        Consumer<String> p = progress != null ? progress : msg -> {};
        List<Path> installed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long loaderBytes = 0L;

        p.accept("Starting Vida install (ATLauncher) → " + opt.targetInstance());

        try {
            loaderBytes = doInstall(opt, installed, warnings, p);
            p.accept("Install complete.");
            warnings.add("ATLauncher has no file-watcher — restart it to reload the instance.");
            p.accept("NOTE: restart ATLauncher so it re-reads instance.json.");
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
            throw new IOException("ATLauncher requires a target instance "
                    + "(pass --instance <path> or pick one in GUI).");
        }
        instanceDir = instanceDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(instanceDir)) {
            throw new IOException("Instance dir does not exist: " + instanceDir);
        }
        Path instanceJson = instanceDir.resolve("instance.json");
        if (!Files.isRegularFile(instanceJson)) {
            throw new IOException(
                    "Not an ATLauncher instance (missing instance.json): " + instanceDir);
        }

        Path vidaDir = instanceDir.resolve("vida");
        Path loaderJar = vidaDir.resolve("vida-loader-" + opt.loaderVersion() + ".jar");

        if (loaderJar.toString().indexOf(' ') >= 0) {
            throw new IOException("Instance path contains whitespace, which ATLauncher "
                    + "cannot handle in javaArguments: " + loaderJar);
        }

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
            ATLauncherJsonPatcher.Result r = ATLauncherJsonPatcher.patch(
                    instanceJson, loaderJar.toString());
            installed.add(instanceJson);
            if (r.alreadyAgent()) {
                progress.accept("  replaced existing Vida agent path.");
            } else {
                progress.accept("  appended -javaagent to launcher.javaArguments.");
            }
            if (r.previousArgs() != null && !r.previousArgs().isBlank()) {
                progress.accept("  previous javaArguments preserved: " + r.previousArgs());
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
        root.put("agentsMode", "jvmArgs");

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
