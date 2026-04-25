/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.escultores;

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
 */
@ApiStatus.Stable
public final class BrandingEscultor implements Escultor {

    public static final BrandingEscultor INSTANCE = new BrandingEscultor();

    private static final Log LOG = Log.of(BrandingEscultor.class);

    private static final byte[] MARKER_FMT =
            "Minecraft %s (".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private static final byte[] MARKER_INDY = {
            'M','i','n','e','c','r','a','f','t',' ', 0x01, ' ','('
    };

    private static final String REPLACEMENT_FMT  = "Minecraft %1$s (vida)";
    private static final String REPLACEMENT_INDY = "Minecraft \u0001 (vida)";
    private static final String INDY_DESC_PATCHED = "(Ljava/lang/String;)Ljava/lang/String;";

    private BrandingEscultor() {}

    @Override
    public String nombre() {
        return "BrandingEscultor";
    }

    @Override
    public boolean mightMatch(String nombreClaseInternal, byte[] archivoClase) {
        return archivoClase != null
                && (containsBytes(archivoClase, MARKER_FMT)
                    || containsBytes(archivoClase, MARKER_INDY));
    }

    /** Статический доступ для горячего пути без виртуального вызова интерфейса. */
    public static boolean mightMatch(byte[] classfile) {
        return INSTANCE.mightMatch("", classfile);
    }

    @Override
    public byte[] tryPatch(String nombreClaseInternal, byte[] archivoClase) {
        return aplicarPatch(archivoClase);
    }

    /** Статический вход для загрузчика и тестов. */
    public static byte[] tryPatch(byte[] classfile) {
        return INSTANCE.tryPatch("", classfile);
    }

    private static byte[] aplicarPatch(byte[] classfile) {
        try {
            ClassReader cr = new ClassReader(classfile);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            boolean changedFmt  = patchLdcStrings(cn);
            boolean changedIndy = patchIndyConcat(cn);
            if (!changedFmt && !changedIndy) return null;

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

    /** Visible for tests. */
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

    private static boolean patchIndyConcat(ClassNode cn) {
        boolean changed = false;
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode insn : mn.instructions) {
                if (!(insn instanceof InvokeDynamicInsnNode idyn)) continue;
                if (!"makeConcatWithConstants".equals(idyn.bsm.getName())) continue;
                if (idyn.bsmArgs == null || idyn.bsmArgs.length == 0) continue;
                if (!(idyn.bsmArgs[0] instanceof String template)) continue;
                if (!isF3ConcatTemplate(template)) continue;

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

    /** Visible for tests. */
    static boolean isF3ConcatTemplate(String t) {
        return t != null
                && t.startsWith("Minecraft \u0001 (")
                && t.endsWith(")");
    }

    private static int countPlaceholders(String template) {
        int n = 0;
        for (int i = 0; i < template.length(); i++) {
            if (template.charAt(i) == '\u0001') n++;
        }
        return n;
    }

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
