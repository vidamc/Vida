/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Встроенный Escultor: на лету заменяет строку первой строки F3-оверлея
 * Minecraft, так что в F3 отображается {@code Minecraft <ver> (vida)}.
 *
 * <h2>Два режима работы</h2>
 * <p>Minecraft использовал разные механизмы формирования строки:
 * <ul>
 *   <li><b>LDC / String.format (MC ≤ 1.20)</b>: формат-строка
 *       {@code "Minecraft %s (%s/%s%s)"} лежит как константа в constant pool
 *       и передаётся в {@code String.format}. Маркер: {@link #MARKER_FMT}.</li>
 *   <li><b>invokedynamic / StringConcatFactory (MC 1.21+)</b>: javac 21
 *       компилирует конкатенацию строк в {@code invokedynamic
 *       makeConcatWithConstants}, и шаблон {@code "Minecraft \u0001 (\u0001/\u0001)"}
 *       ({@code \u0001} = placeholder) хранится в аргументах bootstrap-метода.
 *       Маркер: {@link #MARKER_INDY}.</li>
 * </ul>
 *
 * <h2>Что делает патч для invokedynamic</h2>
 * <ol>
 *   <li>Меняет шаблон на {@code "Minecraft \u0001 (vida)"} (один placeholder —
 *       только версия Minecraft).</li>
 *   <li>Обновляет дескриптор {@code INVOKEDYNAMIC} с N аргументов на 1
 *       ({@code (Ljava/lang/String;)Ljava/lang/String;}).</li>
 *   <li>Вставляет {@code (N-1)} инструкций {@code POP} перед {@code INVOKEDYNAMIC},
 *       чтобы сбросить лишние аргументы со стека. Код их вычисления остаётся,
 *       результаты немедленно отбрасываются — сайд-эффектов нет (все аргументы
 *       — иммутабельные строки).</li>
 * </ol>
 *
 * <h2>Hot-path cost</h2>
 * <p>На каждый загружаемый класс — только быстрый байтовый скан ({@link #mightMatch}).
 * Для ~99.99% классов ни один маркер не найден, и {@code mightMatch} возвращает
 * {@code false} за &lt; 1 мкс. Полный парсинг ASM запускается лишь для DebugHud —
 * однократно.
 */
@ApiStatus.Internal
public final class BrandingEscultor {

    private static final Log LOG = Log.of(BrandingEscultor.class);

    /** Маркер LDC-пути (MC ≤ 1.20): начало {@code String.format}-строки. */
    private static final byte[] MARKER_FMT =
            "Minecraft %s (".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    /**
     * Маркер invokedynamic-пути (MC 1.21+): начало шаблона
     * {@code StringConcatFactory}. {@code 0x01} = Unicode U+0001 (placeholder),
     * который javac/JDK хранит в constant pool буквально как байт 0x01.
     */
    private static final byte[] MARKER_INDY = {
            'M','i','n','e','c','r','a','f','t',' ', 0x01, ' ','('
    };

    /** Замена для LDC-пути: позиционный {@code %1$s} — только версия. */
    private static final String REPLACEMENT_FMT  = "Minecraft %1$s (vida)";

    /** Замена для invokedynamic-пути: один placeholder — только версия. */
    private static final String REPLACEMENT_INDY = "Minecraft \u0001 (vida)";

    /** Дескриптор после патча: один String → String. */
    private static final String INDY_DESC_PATCHED = "(Ljava/lang/String;)Ljava/lang/String;";

    private BrandingEscultor() {}

    /**
     * Быстрый байтовый скан: присутствует ли хотя бы один из маркеров
     * ({@link #MARKER_FMT} или {@link #MARKER_INDY}) в raw classfile-байтах?
     * Если нет — класс точно не DebugHud, и {@link #tryPatch} вызывать не нужно.
     */
    public static boolean mightMatch(byte[] classfile) {
        if (classfile == null) return false;
        return containsBytes(classfile, MARKER_FMT)
                || containsBytes(classfile, MARKER_INDY);
    }

    /**
     * Парсит класс, ищет F3-строку в любом поддерживаемом формате и заменяет
     * её на Vida-брендинг.
     *
     * @return модифицированные байты, либо {@code null}, если нужная строка
     *         не найдена (редкий ложноположительный скан).
     */
    public static byte[] tryPatch(byte[] classfile) {
        try {
            ClassReader cr = new ClassReader(classfile);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            boolean changedFmt  = patchLdcStrings(cn);
            boolean changedIndy = patchIndyConcat(cn);
            if (!changedFmt && !changedIndy) return null;

            // ClassWriter(0): не пересчитываем frames/maxs.
            //   • LDC-патч: меняем только константу — стек и frames не трогаем.
            //   • indy-патч: вставляем POP'ы в прямолинейный (non-branch) код;
            //     StackMapTable-записи нужны только для branch-targets, а POP'ы
            //     вставляются до INVOKEDYNAMIC, которая не является branch-target'ом.
            //     max_stack уменьшается или остаётся тем же — пересчёт не нужен.
            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            byte[] out = cw.toByteArray();
            LOG.info("Vida branding: patched F3 string in {} ({} bytes)", cn.name, out.length);
            return out;
        } catch (RuntimeException ex) {
            LOG.warn("Vida branding: failed to patch classfile ({}), leaving untouched",
                    ex.toString());
            return null;
        }
    }

    // ---------------------------------------------------------------- LDC path

    private static boolean patchLdcStrings(ClassNode cn) {
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn : mn.instructions) {
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                    if (isF3FormatString(s)) {
                        ldc.cst = REPLACEMENT_FMT;
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    /**
     * Совпадает ли строка с ожидаемым {@code String.format}-форматом DebugHud?
     * Отлавливает варианты {@code "Minecraft %s (%s/%s%s)"}, {@code "...%s, %s)"},
     * {@code "...%s/%s/%s)"}, но не логи вида {@code "Starting Minecraft %s"}.
     */
    static boolean isF3FormatString(String s) {
        if (s == null || s.length() < 15) return false;
        if (!s.startsWith("Minecraft %s (")) return false;
        if (!s.endsWith(")")) return false;
        int open = s.indexOf('(');
        int close = s.lastIndexOf(')');
        if (open < 0 || close <= open) return false;
        String inside = s.substring(open + 1, close);
        int count = 0, idx = 0;
        while ((idx = inside.indexOf("%s", idx)) != -1) {
            count++;
            idx += 2;
        }
        return count >= 2;
    }

    // ---------------------------------------------------- invokedynamic path

    private static boolean patchIndyConcat(ClassNode cn) {
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn : mn.instructions) {
                if (!(insn instanceof InvokeDynamicInsnNode idyn)) continue;
                if (!"makeConcatWithConstants".equals(idyn.bsm.getName())) continue;
                if (idyn.bsmArgs == null || idyn.bsmArgs.length == 0) continue;
                if (!(idyn.bsmArgs[0] instanceof String template)) continue;
                if (!isF3ConcatTemplate(template)) continue;

                // Сбрасываем лишние аргументы (все кроме первого — версии).
                // Стек (вершина сверху): ..., version, arg2, ..., argN (argN on top).
                // Каждый POP убирает argN, потом argN-1, …, arg2.
                int extraArgs = countPlaceholders(template) - 1;
                for (int i = 0; i < extraArgs; i++) {
                    mn.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                }

                idyn.bsmArgs[0] = REPLACEMENT_INDY;
                idyn.desc = INDY_DESC_PATCHED;
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Является ли шаблон {@code StringConcatFactory} DebugHud-строкой?
     * Примеры: {@code "Minecraft \u0001 (\u0001/\u0001)"},
     * {@code "Minecraft \u0001 (\u0001/\u0001, \u0001)"}.
     */
    static boolean isF3ConcatTemplate(String t) {
        return t != null
                && t.startsWith("Minecraft \u0001 (")
                && t.endsWith(")");
    }

    /** Считает количество placeholder-символов {@code U+0001} в шаблоне. */
    private static int countPlaceholders(String template) {
        int n = 0;
        for (int i = 0; i < template.length(); i++) {
            if (template.charAt(i) == '\u0001') n++;
        }
        return n;
    }

    // ---------------------------------------------------------- byte scanner

    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        int nLen = needle.length;
        int hLen = haystack.length;
        if (hLen < nLen) return false;
        byte first = needle[0];
        int end = hLen - nLen;
        outer:
        for (int i = 0; i <= end; i++) {
            if (haystack[i] != first) continue;
            for (int j = 1; j < nLen; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
