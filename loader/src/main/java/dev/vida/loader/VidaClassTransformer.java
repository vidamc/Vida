/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.escultores.BrandingEscultor;
import dev.vida.escultores.Escultor;
import dev.vida.vifada.MorphSource;
import dev.vida.vifada.MorphMethodResolution;
import dev.vida.vifada.TransformReport;
import dev.vida.vifada.Transformer;
import dev.vida.vifada.VifadaMorphTraceHtml;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * {@link ClassFileTransformer}, который по каждому классу проверяет
 * {@link MorphIndex} и, если для него есть морфы, применяет их через
 * {@link Transformer}.
 *
 * <p>Транформер читает в основном Map → для классов без морфов стоимость
 * — один hashmap lookup и один startsWith-check, никаких atomic ops. Для
 * классов с морфами — полный проход ClassReader/Writer один раз.
 *
 * <p><b>Hot-path contract.</b> Главный (skip-)путь:
 * <ol>
 *   <li>три {@code String.startsWith} — отсеиваем свои внутренние классы;</li>
 *   <li>{@link MorphIndex#hasMorphs(String)} — один {@code HashMap.get};</li>
 *   <li>возврат {@code null} (no-op для JVM).</li>
 * </ol>
 * Этот путь по ≈50-100 нс на класс, без CAS. Инкременты счётчиков на пути
 * «ничего не делать» убраны намеренно: при типичном игровом рантайме
 * загружаются десятки тысяч классов, и CAS по {@link LongAdder}/{@code AtomicLong}
 * на этом горячем пути создавал заметные пики задержки (микро-фризы при
 * исследовании мира). Сейчас счётчики инкрементятся только в момент реальной
 * трансформации или ошибки — то есть редко.
 *
 * <p>Счётчики используют {@link LongAdder} вместо {@code AtomicLong}: они
 * оптимизированы под «много писателей / редкие читатели» и полностью
 * избегают contention в многопоточной загрузке классов.
 */
@ApiStatus.Preview("loader")
public final class VidaClassTransformer implements ClassFileTransformer {

    /**
     * Если {@code true} (системное свойство {@code vida.loader.profileEscultorNanos}),
     * при каждом проходе Escultor по классу считается длительность в наносекундах.
     * По умолчанию {@code false}: при старте остаются только счётчики попаданий,
     * без двойного {@code nanoTime} на каждый Escultor (см. ТЗ — быстрый старт).
     */
    private static final boolean PROFILE_ESCULTOR_NANOS =
            Boolean.parseBoolean(System.getProperty("vida.loader.profileEscultorNanos", "false"));

    private final MorphIndex index;
    private final Consumer<LoaderError> errorSink;
    private final Map<String, String> obfToDeobf;
    private final MorphMethodResolution morphMethodResolution;
    private final TransformBytecodeCache bytecodeCache;
    private final String mappingFingerprint;
    private final LongAdder transformed = new LongAdder();
    private final LongAdder skipped     = new LongAdder();
    private final LongAdder errors      = new LongAdder();
    private final LongAdder cacheHits   = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();

    private volatile List<Escultor> modEscultores = List.of();
    private final EscultorRegistroMetricas escultorMetricas = new EscultorRegistroMetricas();

    public VidaClassTransformer(MorphIndex index, Consumer<LoaderError> errorSink) {
        this(index, errorSink, Map.of(), null, null, null);
    }

    /**
     * @param obfToDeobf obfuscated internal name → deobfuscated internal name.
     *                   Used to resolve morph targets when Minecraft classes are
     *                   loaded under their obfuscated names at runtime.
     */
    public VidaClassTransformer(MorphIndex index, Consumer<LoaderError> errorSink,
                                Map<String, String> obfToDeobf) {
        this(index, errorSink, obfToDeobf, null, null, null);
    }

    /**
     * @param morphMethodResolution резолв имён методов Mojmap → obf (из client_mappings)
     */
    public VidaClassTransformer(
            MorphIndex index,
            Consumer<LoaderError> errorSink,
            Map<String, String> obfToDeobf,
            MorphMethodResolution morphMethodResolution) {
        this(index, errorSink, obfToDeobf, morphMethodResolution, null, null);
    }

    /**
     * @param bytecodeCache кеш успешных трансформаций (или {@code null})
     * @param mappingFingerprint SHA-256 hex текста client_mappings — участвует в ключе кеша
     */
    public VidaClassTransformer(
            MorphIndex index,
            Consumer<LoaderError> errorSink,
            Map<String, String> obfToDeobf,
            MorphMethodResolution morphMethodResolution,
            TransformBytecodeCache bytecodeCache,
            String mappingFingerprint) {
        this.index = Objects.requireNonNull(index, "index");
        this.errorSink = errorSink == null ? e -> {} : errorSink;
        this.obfToDeobf = obfToDeobf == null ? Map.of() : obfToDeobf;
        this.morphMethodResolution = morphMethodResolution;
        this.bytecodeCache = bytecodeCache;
        this.mappingFingerprint = mappingFingerprint;
    }

    /**
     * Escultor'ы из {@code vida.mod.json}, загруженные мод-класслодером.
     * Вызывается до {@code invokeEntrypoints}, но после регистрации transformer
     * часть классов уже могла загрузиться без них.
     */
    public void instalarEscultoresMod(List<Escultor> escultores) {
        this.modEscultores = escultores == null ? List.of() : List.copyOf(escultores);
    }

    public EscultorRegistroMetricas escultorMetricas() {
        return escultorMetricas;
    }

    /** Индекс, из которого работает этот трансформер. */
    public MorphIndex index() { return index; }

    /** Количество классов, в которых реально применились морфы. */
    public long transformedCount() { return transformed.sum(); }

    /**
     * Количество классов, которые явно попали на skip-ветку с инкрементом
     * (null className, null buffer). Классы без морфов в это число НЕ
     * попадают — их и так большинство, мы их не считаем, чтобы не трогать
     * CAS на hot-path.
     */
    public long skippedCount()     { return skipped.sum(); }
    public long errorCount()       { return errors.sum(); }

    /** Попадания дискового кеша трансформаций (только если кеш включён). */
    public long transformCacheHits() { return cacheHits.sum(); }

    /** Промахи кеша при включённом кеше. */
    public long transformCacheMisses() { return cacheMisses.sum(); }

    // ====================================================================

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer)
            throws IllegalClassFormatException {
        // className == null — это hidden/lambda-класс; classfileBuffer == null
        // бывает при ретрансформации без backing-байтов. Оба случая редки,
        // поэтому инкремент счётчика здесь погоды не делает.
        if (className == null || classfileBuffer == null) {
            skipped.increment();
            return null;
        }
        // Фильтр: свои собственные классы трансформировать нельзя, иначе
        // будет class-circularity (мы трансформируем сами себя).
        if (className.startsWith("dev/vida/loader/")
                || className.startsWith("dev/vida/vifada/")
                || className.startsWith("dev/vida/core/")) {
            return null;
        }

        // Built-in branding Escultor: быстрый байтовый pre-check, и только
        // если он сработал — полноценный ASM-парсинг. Подавляющее
        // большинство классов никогда не проходит pre-check, так что это
        // добавляет единицы наносекунд на класс — незаметно для hot-path.
        byte[] workingBuf = classfileBuffer;
        boolean trabajo = false;
        if (BrandingEscultor.mightMatch(workingBuf)) {
            byte[] patched = BrandingEscultor.tryPatch(workingBuf);
            if (patched != null) {
                workingBuf = patched;
                trabajo = true;
            }
        }

        EscultoresResult esc = aplicarEscultores(className, workingBuf);
        workingBuf = esc.bytes();
        trabajo |= esc.modificado();

        // Resolve: если className обфусцирован, ищем деобфусцированный эквивалент.
        String morphKey = resolveMorphKey(className);

        // No-morph branch — это 99.99% всех вызовов; НИКАКИХ counter-ops.
        if (morphKey == null) {
            return trabajo ? workingBuf : null;
        }
        byte[] morphed = applyMorphs(morphKey, workingBuf);
        if (morphed != null) return morphed;
        return trabajo ? workingBuf : null;
    }

    /**
     * Публичный путь, который используют кастомные ClassLoader'ы
     * ({@link TransformingClassLoader}), вызывая его до {@code defineClass}.
     *
     * @return трансформированные байты либо исходные, если морфов нет
     */
    public byte[] transformClassfile(String internalName, byte[] buf) {
        byte[] working = buf;
        if (BrandingEscultor.mightMatch(working)) {
            byte[] branded = BrandingEscultor.tryPatch(working);
            if (branded != null) working = branded;
        }
        EscultoresResult esc = aplicarEscultores(internalName, working);
        working = esc.bytes();
        String morphKey = resolveMorphKey(internalName);
        if (morphKey == null) return working;
        byte[] out = applyMorphs(morphKey, working);
        return out != null ? out : working;
    }

    private record EscultoresResult(byte[] bytes, boolean modificado) {}

    private EscultoresResult aplicarEscultores(String internalName, byte[] buf) {
        byte[] wb = buf;
        boolean cambio = false;
        for (Escultor e : modEscultores) {
            long t0 = PROFILE_ESCULTOR_NANOS ? System.nanoTime() : 0L;
            try {
                if (!e.mightMatch(internalName, wb)) {
                    escultorMetricas.registro(e.nombre(),
                            PROFILE_ESCULTOR_NANOS ? System.nanoTime() - t0 : 0L, false);
                    continue;
                }
                byte[] patch = e.tryPatch(internalName, wb);
                escultorMetricas.registro(e.nombre(),
                        PROFILE_ESCULTOR_NANOS ? System.nanoTime() - t0 : 0L, patch != null);
                if (patch != null) {
                    wb = patch;
                    cambio = true;
                }
            } catch (RuntimeException ex) {
                escultorMetricas.registro(e.nombre(),
                        PROFILE_ESCULTOR_NANOS ? System.nanoTime() - t0 : 0L, false);
                errorSink.accept(new LoaderError.BootFailure(
                        "Escultor '" + e.nombre() + "' failed on " + internalName + ": " + ex));
            }
        }
        return new EscultoresResult(wb, cambio);
    }

    /**
     * Returns the MorphIndex key if any morphs are registered for this class,
     * checking both the raw name and the deobfuscated equivalent. Returns
     * {@code null} if no morphs match.
     */
    private String resolveMorphKey(String internalName) {
        if (index.hasMorphs(internalName)) return internalName;
        String deobf = obfToDeobf.get(internalName);
        if (deobf != null && index.hasMorphs(deobf)) return deobf;
        return null;
    }

    private byte[] applyMorphs(String internalName, byte[] buf) {
        List<MorphSource> sources = index.forTarget(internalName);
        String cacheKey = null;
        if (bytecodeCache != null && !sources.isEmpty()) {
            cacheKey = TransformBytecodeCache.computeKey(
                    internalName, buf, sources, mappingFingerprint);
            byte[] hit = bytecodeCache.get(cacheKey);
            if (hit != null) {
                cacheHits.increment();
                transformed.increment();
                return hit;
            }
            cacheMisses.increment();
        }

        TransformReport report = Transformer.transform(
                buf, sources, internalName, morphMethodResolution);
        VifadaMorphTraceHtml.registrar(internalName, report);
        if (report.hasErrors()) {
            errors.increment();
            errorSink.accept(new LoaderError.VifadaFailed(internalName, List.copyOf(report.errors())));
            if (report.bytes() == null) return null;
        }
        transformed.increment();
        byte[] out = report.bytes();
        if (bytecodeCache != null && cacheKey != null && out != null && !report.hasErrors()) {
            bytecodeCache.put(cacheKey, out);
        }
        return out;
    }
}
