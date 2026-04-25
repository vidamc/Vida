/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.InjectionPoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/** Разрешение слотов LVT для {@link dev.vida.vifada.VifadaLocal}. */
@ApiStatus.Internal
public final class LocalSlotResolver {

    private LocalSlotResolver() {}

    /**
     * Находит JVM slot для привязки на точке {@link InjectionPoint#HEAD}.
     *
     * <p>На {@link InjectionPoint#RETURN} привязка не поддерживается (ошибка на этапе применения).
     */
    public static int resolveHeadSlot(MethodNode targetMethod, VifadaLocalBinding binding) {
        LabelNode startLabel = firstLabel(targetMethod);
        if (startLabel == null) {
            return -1;
        }
        List<LocalVariableNode> matches = new ArrayList<>();
        if (targetMethod.localVariables != null) {
            for (LocalVariableNode lv : targetMethod.localVariables) {
                if (lv.start != startLabel) {
                    continue;
                }
                String want = binding.descriptor();
                if (!want.isEmpty() && !want.equals(lv.desc)) {
                    continue;
                }
                matches.add(lv);
            }
        }
        matches.sort(Comparator.comparingInt(lv -> lv.index));
        if (binding.ordinal() >= matches.size()) {
            return -1;
        }
        return matches.get(binding.ordinal()).index;
    }

    private static LabelNode firstLabel(MethodNode m) {
        if (m.instructions == null) {
            return null;
        }
        for (AbstractInsnNode ins = m.instructions.getFirst(); ins != null; ins = ins.getNext()) {
            if (ins instanceof LabelNode ln) {
                return ln;
            }
        }
        return null;
    }
}
