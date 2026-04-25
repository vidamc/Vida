/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import dev.vida.vifada.MorphMethodResolution;
import dev.vida.vifada.internal.MorphApplier;
import dev.vida.vifada.internal.MorphDescriptor;
import dev.vida.vifada.internal.MorphParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 * Публичная точка входа Vifada: принимает байты целевого класса и
 * список морфов в виде {@link MorphSource}, возвращает
 * {@link TransformReport} с применёнными морфами и/или ошибками.
 *
 * <p>Трансформация один-в-один: сколько бы морфов ни шло в списке, всё
 * применяется за один проход {@link ClassReader}→{@link ClassNode}→
 * {@link ClassWriter}.
 */
@ApiStatus.Stable
public final class Transformer {

    private Transformer() {}

    /**
     * Применяет морфы к целевому классу.
     *
     * @param targetBytes исходный байткод целевого класса
     * @param morphs      байткод всех морфов (в любом порядке — аппликатор
     *                    сам отсортирует по {@code priority} и применит те,
     *                    у которых {@code target} совпадает с текущим классом)
     */
    public static TransformReport transform(byte[] targetBytes, Collection<MorphSource> morphs) {
        return transform(targetBytes, morphs, null, null);
    }

    /**
     * @param morphTargetClassInternal внутреннее имя класса в Mojmap (как в {@code @VifadaMorph#target}),
     *                                   если байткод использует обфусцированное {@code ClassNode#name}
     * @param methodResolution          резолв Mojmap → obf для имён методов; может быть {@code null}
     */
    public static TransformReport transform(
            byte[] targetBytes,
            Collection<MorphSource> morphs,
            String morphTargetClassInternal,
            MorphMethodResolution methodResolution) {
        Objects.requireNonNull(targetBytes, "targetBytes");
        Objects.requireNonNull(morphs, "morphs");

        // Читаем цель.
        ClassNode target = new ClassNode(dev.vida.vifada.internal.AsmNames.API);
        try {
            new ClassReader(targetBytes).accept(target, ClassReader.SKIP_FRAMES);
        } catch (RuntimeException ex) {
            return new TransformReport(null, List.of(),
                    List.of(new VifadaError.AsmFailure("<target>",
                            "failed to read target bytes: " + ex.getMessage())),
                    List.of());
        }

        // Парсим все морфы.
        List<MorphDescriptor> descriptors = new ArrayList<>(morphs.size());
        List<VifadaError> errors = new ArrayList<>();
        List<String> applied = new ArrayList<>();
        List<MorphSkip> skipped = new ArrayList<>();

        String matchKey = morphTargetClassInternal != null ? morphTargetClassInternal : target.name;

        for (MorphSource src : morphs) {
            Result<MorphDescriptor, VifadaError> r =
                    MorphParser.parse(src.internalName(), src.bytes());
            if (r.isErr()) {
                errors.add(r.unwrapErr());
                continue;
            }
            MorphDescriptor d = r.unwrap();
            if (!d.targetInternal.equals(matchKey)) {
                skipped.add(new MorphSkip(
                        d.morphInternal,
                        "target mismatch: morph declares " + d.targetInternal + " but transforming "
                                + matchKey));
                continue;
            }
            descriptors.add(d);
        }

        if (descriptors.isEmpty()) {
            return new TransformReport(targetBytes, List.of(), errors, skipped);
        }

        // Применяем.
        List<VifadaError> applyErrors =
                MorphApplier.apply(target, descriptors, morphTargetClassInternal, methodResolution);
        errors.addAll(applyErrors);
        for (MorphDescriptor d : descriptors) applied.add(d.morphInternal);

        // Записываем. Используем собственный ClassWriter с мягкой резолвцией
        // общего предка, чтобы COMPUTE_FRAMES не падал на классах, которых
        // нет в системном ClassLoader (иерархия моддинга).
        byte[] out;
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    try {
                        return super.getCommonSuperClass(type1, type2);
                    } catch (Throwable t) {
                        // Не смогли — безопасный фолбэк.
                        return "java/lang/Object";
                    }
                }
            };
            target.accept(cw);
            out = cw.toByteArray();
        } catch (RuntimeException ex) {
            errors.add(new VifadaError.AsmFailure("<target>",
                    "failed to write transformed class: " + ex.getMessage()));
            return new TransformReport(null, applied, errors, skipped);
        }

        return new TransformReport(out, applied, errors, skipped);
    }
}
