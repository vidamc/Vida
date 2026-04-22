/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.loader.internal.BrandingEscultor;
import dev.vida.vifada.TransformReport;
import dev.vida.vifada.Transformer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;
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

    private final MorphIndex index;
    private final Consumer<LoaderError> errorSink;
    private final LongAdder transformed = new LongAdder();
    private final LongAdder skipped     = new LongAdder();
    private final LongAdder errors      = new LongAdder();

    public VidaClassTransformer(MorphIndex index, Consumer<LoaderError> errorSink) {
        this.index = Objects.requireNonNull(index, "index");
        this.errorSink = errorSink == null ? e -> {} : errorSink;
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
        boolean branded = false;
        if (BrandingEscultor.mightMatch(workingBuf)) {
            byte[] patched = BrandingEscultor.tryPatch(workingBuf);
            if (patched != null) {
                workingBuf = patched;
                branded = true;
            }
        }

        // No-morph branch — это 99.99% всех вызовов; НИКАКИХ counter-ops.
        if (!index.hasMorphs(className)) {
            return branded ? workingBuf : null;
        }
        byte[] morphed = applyMorphs(className, workingBuf);
        if (morphed != null) return morphed;
        return branded ? workingBuf : null;
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
        if (!index.hasMorphs(internalName)) return working;
        byte[] out = applyMorphs(internalName, working);
        return out != null ? out : working;
    }

    private byte[] applyMorphs(String internalName, byte[] buf) {
        TransformReport report = Transformer.transform(buf, index.forTarget(internalName));
        if (report.hasErrors()) {
            errors.increment();
            errorSink.accept(new LoaderError.VifadaFailed(internalName, List.copyOf(report.errors())));
            if (report.bytes() == null) return null;
        }
        transformed.increment();
        return report.bytes();
    }
}
