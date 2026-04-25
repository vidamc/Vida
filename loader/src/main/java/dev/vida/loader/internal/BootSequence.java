/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.base.DefaultModContext;
import dev.vida.base.LatidoGlobal;
import dev.vida.base.ModMetadata;
import dev.vida.base.ModulosInstaladosGlobal;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.eventos.FaseCicloMod;
import dev.vida.base.latidos.eventos.LatidoArranque;
import dev.vida.base.latidos.eventos.LatidoFaseCiclo;
import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.config.Ajustes;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.discovery.DiscoveryError;
import dev.vida.discovery.DiscoveryReport;
import dev.vida.discovery.ModCandidate;
import dev.vida.discovery.ModSource;
import dev.vida.discovery.ModScanner;
import dev.vida.discovery.ScanOptions;
import dev.vida.discovery.ZipReader;
import dev.vida.loader.BootOptions;
import dev.vida.loader.BootReport;
import dev.vida.loader.JuegoLoader;
import dev.vida.loader.LoaderError;
import dev.vida.loader.ModLoader;
import dev.vida.loader.MorphIndex;
import dev.vida.loader.TransformBytecodeCache;
import dev.vida.loader.VidaClassTransformer;
import dev.vida.loader.VidaEnvironment;
import dev.vida.loader.profile.PlatformProfileDescriptor;
import dev.vida.loader.profile.PlatformProfileLoader;
import dev.vida.cima.CimaJuegoGlobal;
import dev.vida.platform.CimaJuegoCarga;
import dev.vida.platform.PlatformBridgeSupport;
import dev.vida.vifada.MorphMethodResolution;
import dev.vida.escultores.Escultor;
import dev.vida.fuente.FuenteContenidoMod;
import dev.vida.fuente.FuentePrototipoParser;
import dev.vida.manifest.EscultorDeclaracion;
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
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarFile;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
        ClientEntrypointScheduler.resetForNewBootSession();

        if (options == null) {
            errors.add(new LoaderError.InvalidOptions("BootOptions must not be null"));
            return new BootReport(null, errors, Duration.ofNanos(System.nanoTime() - startNs));
        }

        PlatformProfileLoader.ResolveResult profileRes = PlatformProfileLoader.resolve(options);
        if (profileRes.failureMessage().isPresent()) {
            errors.add(new LoaderError.BootFailure(profileRes.failureMessage().get()));
            return new BootReport(null, errors, Duration.ofNanos(System.nanoTime() - startNs));
        }
        Optional<PlatformProfileDescriptor> platformProfile = profileRes.descriptor();
        platformProfile.ifPresent(d -> LOG.info("Vida: platform profile {}", d.profileId()));
        platformProfile.ifPresent(p -> p.minimumJavaVersion().ifPresent(min -> {
            int feat = Runtime.version().feature();
            if (feat < min) {
                errors.add(new LoaderError.BootFailure(
                        "platform profile requires Java " + min + " or newer (runtime feature version is "
                                + feat + ")"));
            }
        }));
        if (!errors.isEmpty()) {
            return new BootReport(null, errors, Duration.ofNanos(System.nanoTime() - startNs));
        }

        platformProfile.ifPresent(p -> verifyClientJarShaAgainstProfile(options, p, errors));
        if (!errors.isEmpty()) {
            return new BootReport(null, errors, Duration.ofNanos(System.nanoTime() - startNs));
        }

        // ---- 1. Discovery -----------------------------------------------
        List<ModCandidate> candidates = new ArrayList<>();
        discoverAll(options, errors, candidates);

        // ---- 2. Resolve -------------------------------------------------
        Resolution resolution = resolveDependencies(options, candidates, errors, platformProfile);

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
        MorphIndex morphs = collectMorphs(resolution, byId, errors, platformProfile);

        // ---- 3b. Load obf→deobf class name mappings -------------------
        MappingLoader.ClientMappingTables mappingTables =
                loadClientMappingTables(options, platformProfile);
        platformProfile.flatMap(PlatformProfileDescriptor::mappingMode)
                .ifPresent(mode -> LOG.info("Vida: profile mappingMode {}", mode));

        // ---- 4. ClassLoaders -------------------------------------------
        Consumer<LoaderError> sink = e -> { synchronized (errors) { errors.add(e); } };
        MorphMethodResolution methodRes = mappingTables.tree() != null
                ? new CartografiaMorphMethodResolution(mappingTables.tree())
                : null;
        TransformBytecodeCache bytecodeCache = openTransformCache(options);
        String mappingFp = mappingTables.fingerprint();
        if (platformProfile.isPresent()) {
            mappingFp = mappingFp + "|pf:" + platformProfile.get().profileId();
        }
        VidaClassTransformer transformer = new VidaClassTransformer(
                morphs,
                sink,
                mappingTables.obfToMojmapClass(),
                methodRes,
                bytecodeCache,
                mappingFp);
        JuegoLoader juego = new JuegoLoader(toUrls(options.gameJars(), errors),
                parent == null ? ClassLoader.getSystemClassLoader() : parent, transformer);
        Map<String, ModLoader> modLoaders =
                buildModLoaders(resolvedMods, byId, juego, transformer, options, errors);
        instalarEscultoresDeclarados(resolvedMods, modLoaders, transformer, errors);

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
                exposeLoaderJarToSystemClassLoaderSearch(inst);
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

        ModulosInstaladosGlobal.instalar(List.copyOf(resolvedMods));

        // Устанавливаем мост Vida↔vanilla, чтобы платформенные морфы
        // (MinecraftTickMorph, GuiRenderMorph, ServerTickMorph) имели куда обращаться. Мост
        // уважает уже установленный экземпляр (для тестов, подменяющих его
        // на мок). Bridge активен даже при пустом списке модов — это даёт
        // «чистому» Vida рабочую шину тиков/HUD без единого мода.
        PlatformBridgeSupport.installFromProfile(platformProfile);
        CimaJuegoGlobal.instalar(new CimaJuegoCarga());

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
                .catalogos(catalogos)
                .clientMappings(mappingTables.tree())
                .platformProfile(platformProfile);
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
                                                  List<LoaderError> errors,
                                                  Optional<PlatformProfileDescriptor> platformProfile) {
        Universe.Builder ub = Universe.builder();
        java.util.LinkedHashSet<String> rootIds = new java.util.LinkedHashSet<>(candidates.size());
        List<ModCandidate> cands = List.copyOf(candidates);
        List<java.util.Map.Entry<Integer, Provider>> indexed =
                java.util.stream.IntStream.range(0, cands.size())
                        .parallel()
                        .mapToObj(
                                i -> java.util.Map.entry(
                                        i, ManifestAdapter.toProvider(cands.get(i).manifest(), cands.get(i))))
                        .sorted(java.util.Comparator.comparingInt(java.util.Map.Entry::getKey))
                        .toList();
        for (var e : indexed) {
            ub.add(e.getValue());
            rootIds.add(cands.get(e.getKey()).id());
        }

        // Синтетические платформенные провайдеры: vida, minecraft (если
        // передана версия), java. Они попадают в Universe, но не в roots —
        // резолвер выбирает их только когда кто-то их требует.
        for (Provider synthetic : SyntheticProviders.build(options, rootIds, platformProfile)) {
            ub.add(synthetic);
        }

        if (candidates.isEmpty()) return null;

        ResolverOptions resolverOpts = ResolverOptions.DEFAULTS;
        if (!options.accessDeniedIds().isEmpty()) {
            resolverOpts = resolverOpts.withAccessDenied(options.accessDeniedIds());
        }
        var rr = Resolver.resolve(rootIds, ub.build(), resolverOpts);
        if (rr.isErr()) {
            errors.add(new LoaderError.ResolutionFailed(rr.unwrapErr()));
            return null;
        }
        return rr.unwrap();
    }

    private static MorphIndex collectMorphs(Resolution resolution,
                                            Map<String, ModCandidate> byId,
                                            List<LoaderError> errors,
                                            Optional<PlatformProfileDescriptor> platformProfile) {
        MorphIndex.Builder b = MorphIndex.builder();

        // Платформенные морфы (MinecraftTickMorph, GuiRenderMorph, ServerTickMorph) регистрируем
        // всегда — даже если резолюция не удалась или кандидатов нет. Они
        // нужны для платформенных событий (LatidoPulso, LatidoRenderHud) и не
        // зависят от наличия модов.
        PlatformMorphs.register(b, platformProfile);

        if (resolution == null) return b.build();
        Optional<String> activeProfileId = platformProfile.map(PlatformProfileDescriptor::profileId);
        for (Provider p : resolution.selected().values()) {
            ModCandidate c = byId.get(p.id());
            if (c == null) continue;
            if (!ModMorphGate.allowMorphsFromMod(c.manifest(), activeProfileId)) {
                LOG.warn("Vida: skipping Vifada morphs from mod '{}' (custom.vida.platformProfileIds)",
                        c.manifest().id());
                continue;
            }
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
        ConcurrentHashMap<String, FuenteContenidoMod> out = new ConcurrentHashMap<>();
        java.util.concurrent.ConcurrentLinkedQueue<LoaderError> q = new java.util.concurrent.ConcurrentLinkedQueue<>();
        resolved.parallelStream().forEach(manifest -> {
            ModCandidate candidate = byId.get(manifest.id());
            if (candidate == null) {
                return;
            }
            try (ZipReader zip = candidate.source().open()) {
                var parsed = FuentePrototipoParser.leer(manifest, zip);
                if (parsed.isErr()) {
                    q.add(new LoaderError.DataDrivenFailure(
                            candidate.source().id(),
                            parsed.unwrapErr().toString()));
                    return;
                }
                out.put(manifest.id(), parsed.unwrap());
            } catch (IOException ex) {
                q.add(new LoaderError.DataDrivenFailure(
                        candidate.source().id(),
                        ex.getMessage()));
            }
        });
        errors.addAll(q);
        Map<String, FuenteContenidoMod> ordered = new LinkedHashMap<>();
        for (ModManifest m : resolved) {
            FuenteContenidoMod v = out.get(m.id());
            if (v != null) {
                ordered.put(m.id(), v);
            }
        }
        return ordered;
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

    private static void instalarEscultoresDeclarados(
            List<ModManifest> resolved,
            Map<String, ModLoader> modLoaders,
            VidaClassTransformer transformer,
            List<LoaderError> errors) {
        record Ord(Escultor escultor, int priority, int orden) {}
        List<Ord> tmp = new ArrayList<>();
        int orden = 0;
        for (ModManifest m : resolved) {
            ModLoader ml = modLoaders.get(m.id());
            if (ml == null) {
                continue;
            }
            for (EscultorDeclaracion ed : m.escultores()) {
                try {
                    Class<?> cls = ml.loadClass(ed.className());
                    Object inst = cls.getDeclaredConstructor().newInstance();
                    if (!(inst instanceof Escultor esc)) {
                        errors.add(new LoaderError.BootFailure(
                                "escultor class '" + ed.className() + "' does not implement Escultor"
                                        + " in mod '" + m.id() + "'"));
                        continue;
                    }
                    tmp.add(new Ord(esc, ed.priority(), orden++));
                } catch (ReflectiveOperationException ex) {
                    errors.add(new LoaderError.BootFailure(
                            "failed to load escultor '" + ed.className() + "' for mod '" + m.id()
                                    + "': " + ex.getMessage()));
                }
            }
        }
        tmp.sort(Comparator.comparingInt(Ord::priority).thenComparingInt(Ord::orden));
        transformer.instalarEscultoresMod(tmp.stream().map(Ord::escultor).toList());
    }

    private static Map<String, ModLoader> buildModLoaders(List<ModManifest> resolved,
                                                          Map<String, ModCandidate> byId,
                                                          JuegoLoader juego,
                                                          VidaClassTransformer transformer,
                                                          BootOptions options,
                                                          List<LoaderError> errors) {
        Map<String, ModLoader> out = new LinkedHashMap<>();
        for (ModManifest m : resolved) {
            ModCandidate c = byId.get(m.id());
            if (c == null) continue;
            URL[] urls;
            if (c.source() instanceof ModSource.OnDisk od) {
                try {
                    urls = new URL[] {od.path().toUri().toURL()};
                } catch (MalformedURLException ex) {
                    errors.add(new LoaderError.IoFailure(od.path().toString(), ex.getMessage()));
                    continue;
                }
            } else if (c.source() instanceof ModSource.Embedded) {
                Path jar = materializeEmbeddedModJar(c, options, errors);
                if (jar == null) {
                    continue;
                }
                try {
                    urls = new URL[] {jar.toUri().toURL()};
                } catch (MalformedURLException ex) {
                    errors.add(new LoaderError.IoFailure(jar.toString(), ex.getMessage()));
                    continue;
                }
            } else {
                errors.add(new LoaderError.BootFailure(
                        "unsupported ModSource for mod '" + m.id() + "': " + c.source()));
                continue;
            }
            out.put(m.id(), new ModLoader(m.id(), urls, juego, transformer));
        }
        return out;
    }

    /**
     * Вложенный JAR ({@link ModSource.Embedded}) записывается в кеш — у {@link ModLoader}
     * должен быть стабильный {@link URL} на файл.
     */
    private static Path materializeEmbeddedModJar(ModCandidate c, BootOptions options,
                                                  List<LoaderError> errors) {
        if (!(c.source() instanceof ModSource.Embedded em)) {
            return null;
        }
        try {
            Path cacheRoot = options.cacheDir();
            if (cacheRoot == null) {
                cacheRoot = VidaCacheLayout.defaultRoot();
            } else {
                cacheRoot = cacheRoot.toAbsolutePath().normalize();
            }
            Path dir = VidaCacheLayout.embeddedModsDir(cacheRoot).resolve(c.manifest().id());
            Files.createDirectories(dir);
            String hash = sha256Hex(em.bytes());
            Path jar = dir.resolve(hash.substring(0, 16) + ".jar");
            if (!Files.isRegularFile(jar) || Files.size(jar) != em.bytes().length) {
                Files.write(jar, em.bytes());
            }
            return jar;
        } catch (IOException | RuntimeException ex) {
            errors.add(new LoaderError.IoFailure(
                    c.source().id(), "embedded mod materialize failed: " + ex.getMessage()));
            return null;
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * When {@code profile.json} sets {@code clientJar.sha256}, compares it to the file given by
     * {@code -Dvida.clientJar=&lt;path&gt;} or to the sole {@link BootOptions#gameJars()} entry.
     */
    private static void verifyClientJarShaAgainstProfile(
            BootOptions options,
            PlatformProfileDescriptor profile,
            List<LoaderError> errors) {
        if (profile.clientJarSha256Hex().isEmpty()) {
            return;
        }
        Path jar = resolveClientJarPathForShaCheck(options);
        if (jar == null) {
            LOG.warn(
                    "Vida: profile '{}' declares clientJar.sha256 but neither -Dvida.clientJar nor "
                            + "exactly one gameJar was provided — skipping SHA check",
                    profile.profileId());
            return;
        }
        try {
            if (!Files.isRegularFile(jar)) {
                errors.add(new LoaderError.IoFailure(jar.toString(), "client jar not found"));
                return;
            }
            byte[] raw = Files.readAllBytes(jar);
            String actual = sha256Hex(raw);
            String expected = profile.clientJarSha256Hex().get();
            if (!actual.equalsIgnoreCase(expected)) {
                errors.add(new LoaderError.BootFailure(
                        "client jar SHA-256 mismatch for profile " + profile.profileId()
                                + ": expected " + expected + " but got " + actual + " (" + jar + ")"));
            } else {
                LOG.info("Vida: client jar SHA-256 matches profile {} ({})", profile.profileId(), jar);
            }
        } catch (IOException ex) {
            errors.add(new LoaderError.IoFailure(jar.toString(), ex.getMessage()));
        }
    }

    private static Path resolveClientJarPathForShaCheck(BootOptions options) {
        String prop = System.getProperty("vida.clientJar");
        if (prop != null && !prop.isBlank()) {
            return Path.of(prop.trim());
        }
        List<Path> gj = options.gameJars();
        if (gj.size() == 1) {
            return gj.get(0);
        }
        return null;
    }

    /**
     * Инстанцирует entrypoint-классы каждого резолвнутого мода и вызывает
     * {@link VidaMod#iniciar(dev.vida.base.ModContext)}.
     *
     * <p>Порядок: {@code preLaunch} → {@code main} → {@code client} → {@code server}.
     * {@code client} откладывается до первого {@code Minecraft.tick()} (см.
     * {@link ClientEntrypointScheduler}), чтобы {@code iniciar} не вызывал LWJGL/OpenGL
     * из {@code premain}, когда контекста ещё нет.
     *
     * <p>Ошибки при инициализации отдельного мода добавляются в {@code errors} и не
     * прерывают инициализацию оставшихся модов (для отложенных client-entrypoint ошибки
     * появляются после первого клиентского тика и не попадают в {@link BootReport}).
     */
    private static void invokeEntrypoints(List<ModManifest> resolved,
                                          Map<String, ModLoader> modLoaders,
                                          LatidoBus latidos,
                                          CatalogoManejador catalogos,
                                          BootOptions options,
                                          List<LoaderError> errors) {
        int n = resolved.size();
        emitFase(latidos, FaseCicloMod.PREPARACION, Instant.now(), n);
        for (ModManifest manifest : resolved) {
            ModLoader loader = modLoaders.get(manifest.id());
            if (loader == null) {
                continue;
            }
            for (String className : manifest.entrypoints().preLaunch()) {
                invokeOne(className, manifest, loader, latidos, catalogos, options, errors);
            }
        }
        emitFase(latidos, FaseCicloMod.INICIALIZACION, Instant.now(), n);
        for (ModManifest manifest : resolved) {
            ModLoader loader = modLoaders.get(manifest.id());
            if (loader == null) {
                continue;
            }
            for (String className : manifest.entrypoints().main()) {
                invokeOne(className, manifest, loader, latidos, catalogos, options, errors);
            }
        }
        emitFase(latidos, FaseCicloMod.POST_INICIALIZACION, Instant.now(), n);
        for (ModManifest manifest : resolved) {
            ModLoader loader = modLoaders.get(manifest.id());
            if (loader == null) {
                continue;
            }
            for (String className : manifest.entrypoints().client()) {
                final String cn = className;
                ClientEntrypointScheduler.enqueue(
                        () -> invokeOne(cn, manifest, loader, latidos, catalogos, options, errors));
            }
            for (String className : manifest.entrypoints().server()) {
                invokeOne(className, manifest, loader, latidos, catalogos, options, errors);
            }
        }
        try {
            latidos.emitir(LatidoArranque.TIPO, new LatidoArranque(Instant.now(), n));
        } catch (RuntimeException ex) {
            LOG.warn("Vida: LatidoArranque dispatch failed ({})", ex.toString());
        }
    }

    private static void emitFase(LatidoBus bus, FaseCicloMod fase, Instant momento, int modsTotal) {
        try {
            bus.emitir(LatidoFaseCiclo.TIPO, new LatidoFaseCiclo(momento, fase, modsTotal));
        } catch (RuntimeException ex) {
            LOG.warn("Vida: LatidoFaseCiclo {} dispatch failed ({})", fase, ex.toString());
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

    private static TransformBytecodeCache openTransformCache(BootOptions options) {
        if (!Boolean.parseBoolean(System.getProperty("vida.transformCache", "true"))) {
            return null;
        }
        try {
            Path root = options.cacheDir();
            if (root == null) {
                root = VidaCacheLayout.defaultRoot();
            } else {
                root = root.toAbsolutePath().normalize();
            }
            Path dir = VidaCacheLayout.transformBytecodeDir(root);
            Files.createDirectories(dir);
            LOG.info("Vida: transform bytecode cache at {}", dir);
            return new TransformBytecodeCache(dir);
        } catch (Exception e) {
            LOG.warn("Vida: transform bytecode cache disabled: {}", e.toString());
            return null;
        }
    }

    private static MappingLoader.ClientMappingTables loadClientMappingTables(
            BootOptions options,
            Optional<PlatformProfileDescriptor> platformProfile) {
        Optional<String> mcVer = options.minecraftVersion();
        if (mcVer.isEmpty()) {
            // Try auto-detected version from classpath
            Optional<dev.vida.core.Version> detected =
                    SyntheticProviders.resolveMinecraftVersion(options, platformProfile);
            if (detected.isPresent()) {
                mcVer = Optional.of(detected.get().toString());
            }
        }
        if (mcVer.isEmpty()) {
            return MappingLoader.ClientMappingTables.empty();
        }

        final String resolvedMcVersion = mcVer.get();
        platformProfile.ifPresent(p -> {
            if (!p.gameVersion().equals(resolvedMcVersion)) {
                LOG.warn(
                        "Vida: minecraftVersion {} differs from platform profile gameVersion {}",
                        resolvedMcVersion,
                        p.gameVersion());
            }
        });

        // Resolve .minecraft root from modsDir (modsDir is .minecraft/mods)
        Path gameDir = null;
        if (options.modsDir() != null) {
            gameDir = options.modsDir().getParent();
        }
        if (gameDir == null) {
            try {
                gameDir = Path.of(System.getProperty("user.dir", "."));
            } catch (SecurityException ignored) {
                return MappingLoader.ClientMappingTables.empty();
            }
        }
        return MappingLoader.loadClientMappingTables(resolvedMcVersion, gameDir, platformProfile);
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

    /**
     * Игровые классы часто загружаются через {@code AppClassLoader}; без этого
     * морфы не могут резолвить классы из fat-agent JAR ({@link dev.vida.loader.ModClassDispatch}
     * и др.), хотя они есть в том же процессе.
     *
     * @see java.lang.instrument.Instrumentation#appendToSystemClassLoaderSearch(java.util.jar.JarFile)
     */
    private static void exposeLoaderJarToSystemClassLoaderSearch(
            java.lang.instrument.Instrumentation inst) {
        try {
            CodeSource cs = BootSequence.class.getProtectionDomain().getCodeSource();
            if (cs == null) {
                LOG.debug("Vida: BootSequence has no CodeSource; skip appendToSystemClassLoaderSearch");
                return;
            }
            URI uri = cs.getLocation().toURI();
            Path path = Paths.get(uri);
            if (!Files.isRegularFile(path)) {
                LOG.debug(
                        "Vida: loader code source is not a jar file ({}); skip appendToSystemClassLoaderSearch",
                        path);
                return;
            }
            JarFile jar = new JarFile(path.toFile(), false);
            inst.appendToSystemClassLoaderSearch(jar);
            LOG.info("Vida: appended loader jar to system class search ({})", path);
        } catch (Exception e) {
            LOG.warn("Vida: appendToSystemClassLoaderSearch failed: {}", e.toString());
        }
    }

}
