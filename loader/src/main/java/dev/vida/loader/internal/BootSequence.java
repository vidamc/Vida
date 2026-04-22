/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.base.DefaultModContext;
import dev.vida.base.LatidoGlobal;
import dev.vida.base.ModMetadata;
import dev.vida.base.VidaMod;
import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.config.Ajustes;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.discovery.DiscoveryError;
import dev.vida.discovery.DiscoveryReport;
import dev.vida.discovery.ModCandidate;
import dev.vida.discovery.ModScanner;
import dev.vida.discovery.ScanOptions;
import dev.vida.discovery.ZipReader;
import dev.vida.loader.BootOptions;
import dev.vida.loader.BootReport;
import dev.vida.loader.JuegoLoader;
import dev.vida.loader.LoaderError;
import dev.vida.loader.ModLoader;
import dev.vida.loader.MorphIndex;
import dev.vida.loader.VidaClassTransformer;
import dev.vida.loader.VidaEnvironment;
import dev.vida.platform.VanillaBridge;
import dev.vida.loader.fuente.FuenteContenidoMod;
import dev.vida.loader.fuente.FuentePrototipoParser;
import dev.vida.manifest.ModManifest;
import dev.vida.resolver.ManifestAdapter;
import dev.vida.resolver.Provider;
import dev.vida.resolver.Resolution;
import dev.vida.resolver.Resolver;
import dev.vida.resolver.ResolverError;
import dev.vida.resolver.ResolverOptions;
import dev.vida.resolver.Universe;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipException;

/**
 * Оркестрация бутстрапа: discovery → parse → resolve → collect morphs →
 * classloaders → environment. Независима от источника запуска (агент /
 * программно).
 */
@ApiStatus.Internal
public final class BootSequence {

    private static final Log LOG = Log.of(BootSequence.class);

    private BootSequence() {}

    public static BootReport run(BootOptions options, ClassLoader parent,
                                 java.lang.instrument.Instrumentation inst) {
        long startNs = System.nanoTime();
        Instant startedAt = Instant.now();
        List<LoaderError> errors = new ArrayList<>();

        if (options == null) {
            errors.add(new LoaderError.InvalidOptions("BootOptions must not be null"));
            return new BootReport(null, errors, Duration.ofNanos(System.nanoTime() - startNs));
        }

        // ---- 1. Discovery -----------------------------------------------
        List<ModCandidate> candidates = new ArrayList<>();
        discoverAll(options, errors, candidates);

        // ---- 2. Resolve -------------------------------------------------
        Resolution resolution = resolveDependencies(options, candidates, errors);

        List<ModManifest> resolvedMods = new ArrayList<>();
        Map<String, ModCandidate> byId = new LinkedHashMap<>();
        for (ModCandidate c : candidates) byId.putIfAbsent(c.id(), c);
        if (resolution != null) {
            for (Provider p : resolution.selected().values()) {
                ModCandidate c = byId.get(p.id());
                if (c != null) resolvedMods.add(c.manifest());
            }
        }

        Map<String, FuenteContenidoMod> fuenteDataDriven =
                collectFuenteDataDriven(resolvedMods, byId, errors);

        if (options.strict() && !errors.isEmpty()) {
            return new BootReport(null, errors, Duration.ofNanos(System.nanoTime() - startNs));
        }

        // ---- 3. Collect morphs -----------------------------------------
        MorphIndex morphs = collectMorphs(resolution, byId, errors);

        // ---- 3b. Load obf→deobf class name mappings -------------------
        Map<String, String> obfToDeobf = loadClassMappings(options);

        // ---- 4. ClassLoaders -------------------------------------------
        Consumer<LoaderError> sink = e -> { synchronized (errors) { errors.add(e); } };
        VidaClassTransformer transformer = new VidaClassTransformer(morphs, sink, obfToDeobf);
        JuegoLoader juego = new JuegoLoader(toUrls(options.gameJars(), errors),
                parent == null ? ClassLoader.getSystemClassLoader() : parent, transformer);
        Map<String, ModLoader> modLoaders = buildModLoaders(resolvedMods, byId, juego, transformer, errors);

        // ---- 5. Register with instrumentation if available -------------
        //
        // canRetransform=false: мы применяем морфы/Escultor'ы ровно один раз
        // в момент define-time, и не нуждаемся в retransformClasses. Это
        // сообщает JVM, что оригинальные байты классов можно не хранить —
        // экономит metaspace и выключает более медленный class-define path,
        // что напрямую убирает микро-фризы на горячем пути игры (стриминг
        // чанков, пакеты, сущности). См. также манифест агента в loader/build.gradle.kts.
        if (inst != null) {
            try {
                inst.addTransformer(transformer, false);
                LOG.info("Vida class transformer attached to Instrumentation");
            } catch (RuntimeException ex) {
                errors.add(new LoaderError.BootFailure(
                        "failed to attach transformer: " + ex.getMessage()));
            }
        }

        // ---- 6. Shared platform infrastructure -------------------------
        // Re-use existing bus if already installed (e.g. in tests that call boot() twice).
        LatidoBus latidos = LatidoGlobal.maybeCurrent()
                .orElseGet(LatidoBus::enMemoria);
        CatalogoManejador catalogos = new CatalogoManejador();

        // Install global bus for Vifada morphs (accessible without :loader dependency).
        if (LatidoGlobal.maybeCurrent().isEmpty()) {
            LatidoGlobal.instalar(latidos);
        }

        // Устанавливаем мост Vida↔vanilla, чтобы платформенные морфы
        // (MinecraftTickMorph, GuiRenderMorph) имели куда обращаться. Мост
        // уважает уже установленный экземпляр (для тестов, подменяющих его
        // на мок). Bridge активен даже при пустом списке модов — это даёт
        // «чистому» Vida рабочую шину тиков/HUD без единого мода.
        if (VanillaBridge.current() == null) {
            VanillaBridge.install(new VanillaBridge());
        }

        // ---- 7. Invoke mod entrypoints ---------------------------------
        invokeEntrypoints(resolvedMods, modLoaders, latidos, catalogos, options, errors);

        VidaEnvironment.Builder eb = VidaEnvironment.builder()
                .options(options)
                .startedAt(startedAt)
                .resolvedMods(List.copyOf(resolvedMods))
                .morphs(morphs)
                .juegoLoader(juego)
                .transformer(transformer)
                .instrumentation(inst)
                .latidos(latidos)
                .catalogos(catalogos);
        for (var e : modLoaders.entrySet()) eb.addModLoader(e.getKey(), e.getValue());
        for (var e : fuenteDataDriven.entrySet()) eb.addFuenteDataDriven(e.getKey(), e.getValue());
        VidaEnvironment env = eb.build();

        return new BootReport(env, List.copyOf(errors),
                Duration.ofNanos(System.nanoTime() - startNs));
    }

    // ================================================================

    private static void discoverAll(BootOptions options, List<LoaderError> errors,
                                    List<ModCandidate> out) {
        if (!options.skipDiscovery() && options.modsDir() != null) {
            if (Files.isDirectory(options.modsDir())) {
                DiscoveryReport r = ModScanner.scan(options.modsDir(), ScanOptions.defaults());
                out.addAll(r.all());
                for (DiscoveryError de : r.errors()) {
                    errors.add(discoveryToLoader(de));
                }
            } else {
                errors.add(new LoaderError.IoFailure(options.modsDir().toString(),
                        "modsDir is not a directory"));
            }
        }
        for (Path extra : options.extraSources()) {
            if (Files.isDirectory(extra)) {
                DiscoveryReport r = ModScanner.scan(extra, ScanOptions.defaults());
                out.addAll(r.all());
                for (DiscoveryError de : r.errors()) errors.add(discoveryToLoader(de));
            } else if (Files.isRegularFile(extra)) {
                // одиночный JAR: временная папка-«виртуалка» не нужна — просто
                // используем OnDisk через ModScanner на родительской папке,
                // но проще собрать кандидата вручную.
                discoverSingle(extra, errors, out);
            } else {
                errors.add(new LoaderError.IoFailure(extra.toString(),
                        "extra source does not exist"));
            }
        }
    }

    private static void discoverSingle(Path jar, List<LoaderError> errors, List<ModCandidate> out) {
        // Используем ModScanner на временной папке-представлении:
        // для простоты запускаем скан на родительской директории и
        // фильтруем по совпадению пути — но более надёжно сделать так:
        // переиспользуем «OnDisk» через ModScanner.scan. Поскольку
        // ModScanner.scan принимает директорию, собираем кандидата
        // вручную через его публичный API.
        dev.vida.discovery.ModSource src = new dev.vida.discovery.ModSource.OnDisk(jar);
        try (ZipReader z = src.open()) {
            if (!z.contains("vida.mod.json")) {
                errors.add(new LoaderError.BadManifest(jar.toString(), "vida.mod.json missing"));
                return;
            }
            byte[] bytes = z.read("vida.mod.json");
            var mr = dev.vida.manifest.ManifestParser.parse(
                    new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            if (mr.isErr()) {
                errors.add(new LoaderError.BadManifest(jar.toString(),
                        mr.unwrapErr().toString()));
                return;
            }
            byte[] sha = new byte[32];
            out.add(new dev.vida.discovery.ModCandidate(
                    src, mr.unwrap(), sha, List.of(), 0));
        } catch (ZipException ze) {
            errors.add(new LoaderError.BadManifest(jar.toString(),
                    "not a zip: " + ze.getMessage()));
        } catch (IOException ioe) {
            errors.add(new LoaderError.IoFailure(jar.toString(), ioe.getMessage()));
        }
    }

    private static LoaderError discoveryToLoader(DiscoveryError e) {
        // Интерфейс гарантирует source() + message(); внутренняя структура
        // diskovery-ошибок для логики loader'а не важна — достаточно оттранслировать
        // их в BadManifest/IoFailure по семейству.
        return switch (e) {
            case DiscoveryError.ManifestMissing mm
                    -> new LoaderError.BadManifest(mm.source(), mm.message());
            case DiscoveryError.ManifestParse mp
                    -> new LoaderError.BadManifest(mp.source(), mp.message());
            case DiscoveryError.NestingTooDeep nt
                    -> new LoaderError.BadManifest(nt.source(), nt.message());
            default -> new LoaderError.IoFailure(e.source(), e.message());
        };
    }

    private static Resolution resolveDependencies(BootOptions options,
                                                  List<ModCandidate> candidates,
                                                  List<LoaderError> errors) {
        Universe.Builder ub = Universe.builder();
        java.util.LinkedHashSet<String> rootIds = new java.util.LinkedHashSet<>(candidates.size());
        for (ModCandidate c : candidates) {
            Provider p = ManifestAdapter.toProvider(c.manifest(), c);
            ub.add(p);
            rootIds.add(c.id());
        }

        // Синтетические платформенные провайдеры: vida, minecraft (если
        // передана версия), java. Они попадают в Universe, но не в roots —
        // резолвер выбирает их только когда кто-то их требует.
        for (Provider synthetic : SyntheticProviders.build(options, rootIds)) {
            ub.add(synthetic);
        }

        if (candidates.isEmpty()) return null;

        var rr = Resolver.resolve(rootIds, ub.build(), ResolverOptions.DEFAULTS);
        if (rr.isErr()) {
            errors.add(new LoaderError.ResolutionFailed(rr.unwrapErr()));
            return null;
        }
        return rr.unwrap();
    }

    private static MorphIndex collectMorphs(Resolution resolution,
                                            Map<String, ModCandidate> byId,
                                            List<LoaderError> errors) {
        MorphIndex.Builder b = MorphIndex.builder();

        // Платформенные морфы (MinecraftTickMorph, GuiRenderMorph) регистрируем
        // всегда — даже если резолюция не удалась или кандидатов нет. Они
        // нужны для платформенных событий (LatidoPulso, LatidoRenderHud) и не
        // зависят от наличия модов.
        PlatformMorphs.register(b);

        if (resolution == null) return b.build();
        for (Provider p : resolution.selected().values()) {
            ModCandidate c = byId.get(p.id());
            if (c == null) continue;
            try (ZipReader z = c.source().open()) {
                MorphCollector.collect(z, b);
            } catch (IOException ioe) {
                errors.add(new LoaderError.IoFailure(c.source().id(),
                        "morph scan failed: " + ioe.getMessage()));
            }
        }
        return b.build();
    }

    private static Map<String, FuenteContenidoMod> collectFuenteDataDriven(
            List<ModManifest> resolved,
            Map<String, ModCandidate> byId,
            List<LoaderError> errors) {
        Map<String, FuenteContenidoMod> out = new LinkedHashMap<>();
        for (ModManifest manifest : resolved) {
            ModCandidate candidate = byId.get(manifest.id());
            if (candidate == null) {
                continue;
            }
            try (ZipReader zip = candidate.source().open()) {
                var parsed = FuentePrototipoParser.leer(manifest, zip);
                if (parsed.isErr()) {
                    errors.add(new LoaderError.DataDrivenFailure(
                            candidate.source().id(),
                            parsed.unwrapErr().toString()));
                    continue;
                }
                out.put(manifest.id(), parsed.unwrap());
            } catch (IOException ex) {
                errors.add(new LoaderError.DataDrivenFailure(
                        candidate.source().id(),
                        ex.getMessage()));
            }
        }
        return out;
    }

    private static URL[] toUrls(List<Path> paths, List<LoaderError> errors) {
        List<URL> urls = new ArrayList<>(paths.size());
        for (Path p : paths) {
            try {
                urls.add(p.toUri().toURL());
            } catch (MalformedURLException ex) {
                errors.add(new LoaderError.IoFailure(p.toString(), "bad URL: " + ex.getMessage()));
            }
        }
        return urls.toArray(URL[]::new);
    }

    private static Map<String, ModLoader> buildModLoaders(List<ModManifest> resolved,
                                                          Map<String, ModCandidate> byId,
                                                          JuegoLoader juego,
                                                          VidaClassTransformer transformer,
                                                          List<LoaderError> errors) {
        Map<String, ModLoader> out = new LinkedHashMap<>();
        for (ModManifest m : resolved) {
            ModCandidate c = byId.get(m.id());
            if (c == null) continue;
            if (!(c.source() instanceof dev.vida.discovery.ModSource.OnDisk od)) {
                // Вложенные моды сейчас пропускаем — их нужно сначала распаковывать.
                continue;
            }
            URL[] urls;
            try {
                urls = new URL[] { od.path().toUri().toURL() };
            } catch (MalformedURLException ex) {
                errors.add(new LoaderError.IoFailure(od.path().toString(), ex.getMessage()));
                continue;
            }
            out.put(m.id(), new ModLoader(m.id(), urls, juego, transformer));
        }
        return out;
    }

    /**
     * Инстанцирует entrypoint-классы каждого резолвнутого мода и вызывает
     * {@link VidaMod#iniciar(dev.vida.base.ModContext)}.
     *
     * <p>Порядок: {@code preLaunch} → {@code main} → {@code client} → {@code server}.
     * Ошибки при инициализации отдельного мода добавляются в {@code errors} и не
     * прерывают инициализацию оставшихся модов.
     */
    private static void invokeEntrypoints(List<ModManifest> resolved,
                                          Map<String, ModLoader> modLoaders,
                                          LatidoBus latidos,
                                          CatalogoManejador catalogos,
                                          BootOptions options,
                                          List<LoaderError> errors) {
        for (ModManifest manifest : resolved) {
            ModLoader loader = modLoaders.get(manifest.id());
            if (loader == null) continue;

            List<String> all = new ArrayList<>();
            all.addAll(manifest.entrypoints().preLaunch());
            all.addAll(manifest.entrypoints().main());
            all.addAll(manifest.entrypoints().client());
            all.addAll(manifest.entrypoints().server());

            for (String className : all) {
                invokeOne(className, manifest, loader, latidos, catalogos, options, errors);
            }
        }
    }

    private static void invokeOne(String className,
                                  ModManifest manifest,
                                  ModLoader loader,
                                  LatidoBus latidos,
                                  CatalogoManejador catalogos,
                                  BootOptions options,
                                  List<LoaderError> errors) {
        try {
            Class<?> cls = loader.loadClass(className);
            Object instance = cls.getDeclaredConstructor().newInstance();
            if (!(instance instanceof VidaMod mod)) {
                errors.add(new LoaderError.BootFailure(
                        "entrypoint '" + className + "' in mod '" + manifest.id()
                        + "' does not implement VidaMod"));
                return;
            }

            List<String> authorNames = manifest.authors().stream()
                    .map(dev.vida.manifest.ModAuthor::name)
                    .toList();
            ModMetadata meta = new ModMetadata(
                    manifest.id(),
                    manifest.version(),
                    manifest.name(),
                    manifest.description().orElse(""),
                    authorNames);

            AjustesTipados ajustes = AjustesTipados.sobre(Ajustes.empty());

            Path dataDir = resolveDataDir(options, manifest.id());
            if (dataDir != null) {
                try { Files.createDirectories(dataDir); } catch (IOException ignored) {}
            } else {
                dataDir = Path.of(System.getProperty("user.dir", "."), ".vida", manifest.id());
            }

            Log modLog = Log.of("vida.mod." + manifest.id());
            dev.vida.base.ModContext ctx = new DefaultModContext(
                    meta, latidos, catalogos, ajustes, modLog, dataDir);

            mod.iniciar(ctx);
            LOG.info("Vida: mod '{}' entrypoint '{}' iniciated", manifest.id(), className);

        } catch (ClassNotFoundException ex) {
            errors.add(new LoaderError.BootFailure(
                    "entrypoint class '" + className + "' not found in mod '" + manifest.id() + "'"));
        } catch (ReflectiveOperationException ex) {
            errors.add(new LoaderError.BootFailure(
                    "failed to instantiate entrypoint '" + className + "' in mod '" + manifest.id()
                    + "': " + ex.getMessage()));
        } catch (Exception ex) {
            errors.add(new LoaderError.BootFailure(
                    "exception in iniciar() of '" + className + "' [" + manifest.id() + "]: "
                    + ex.getMessage()));
        }
    }

    private static Map<String, String> loadClassMappings(BootOptions options) {
        Optional<String> mcVer = options.minecraftVersion();
        if (mcVer.isEmpty()) {
            // Try auto-detected version from classpath
            Optional<dev.vida.core.Version> detected =
                    SyntheticProviders.resolveMinecraftVersion(options);
            if (detected.isPresent()) {
                mcVer = Optional.of(detected.get().toString());
            }
        }
        if (mcVer.isEmpty()) return Map.of();

        // Resolve .minecraft root from modsDir (modsDir is .minecraft/mods)
        Path gameDir = null;
        if (options.modsDir() != null) {
            gameDir = options.modsDir().getParent();
        }
        if (gameDir == null) {
            try {
                gameDir = Path.of(System.getProperty("user.dir", "."));
            } catch (SecurityException ignored) {
                return Map.of();
            }
        }
        return MappingLoader.loadClassMap(mcVer.get(), gameDir);
    }

    private static Path resolveDataDir(BootOptions options, String modId) {
        if (options.modsDir() != null) {
            return options.modsDir().getParent().resolve("config").resolve("vida").resolve(modId);
        }
        if (options.cacheDir() != null) {
            return options.cacheDir().resolve("mods").resolve(modId);
        }
        return null;
    }

}
