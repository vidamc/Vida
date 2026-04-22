/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.VifadaError;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Применяет список распарсенных {@link MorphDescriptor}'ов к целевому
 * {@link ClassNode} за один проход модификации.
 *
 * <p>В текущей версии поддерживается:
 * <ul>
 *   <li>{@code @VifadaOverwrite} — замена тела метода;</li>
 *   <li>{@code @VifadaInject} в точках {@link InjectionPoint#HEAD} и
 *       {@link InjectionPoint#RETURN} для методов с возвратом {@code void}
 *       и не-{@code void}; cancel поддерживается только для {@link
 *       InjectionPoint#HEAD}.</li>
 * </ul>
 *
 * <p>Операции мутируют переданный {@code target} ClassNode.
 */
@ApiStatus.Internal
public final class MorphApplier {

    private MorphApplier() {}

    /**
     * @return список ошибок (пустой при полном успехе); аппликатор
     *         продолжает работу максимально далеко, чтобы собрать как можно
     *         больше диагностики.
     */
    public static List<VifadaError> apply(ClassNode target, List<MorphDescriptor> morphs) {
        List<VifadaError> errors = new ArrayList<>();

        // Упорядочим морфы по priority, стабилизируем по morphInternal для детерминизма.
        List<MorphDescriptor> ordered = new ArrayList<>(morphs);
        ordered.sort(Comparator.<MorphDescriptor>comparingInt(d -> d.priority)
                .thenComparing(d -> d.morphInternal));

        Set<String> usedHelperNames = collectMethodKeys(target);
        int helperCounter = 0;

        for (MorphDescriptor morph : ordered) {
            // ---- Sanity: морф должен таргетить именно этот класс. ---------
            if (!morph.targetInternal.equals(target.name)) {
                errors.add(new VifadaError.WrongTarget(
                        morph.morphInternal, morph.targetInternal, target.name));
                continue;
            }

            // ---- Shadow-валидация -----------------------------------------
            for (ShadowDescriptor sh : morph.shadows) {
                if (!hasMember(target, sh)) {
                    if (!sh.silentMissing()) {
                        errors.add(new VifadaError.ShadowMissing(
                                morph.morphInternal, target.name, renderShadow(sh)));
                    }
                }
            }

            // ---- @VifadaOverwrite -----------------------------------------
            for (OverwriteDescriptor ow : morph.overwrites) {
                MethodNode targetMethod = findMethod(target, ow.targetName(), ow.targetDescriptor());
                if (targetMethod == null) {
                    if (!ow.silentMissing()) {
                        errors.add(new VifadaError.TargetMethodNotFound(
                                morph.morphInternal, target.name, ow.targetMethod()));
                    }
                    continue;
                }
                replaceBody(target, targetMethod, ow.method(), morph.morphInternal);
            }

            // ---- @VifadaInject --------------------------------------------
            for (InjectDescriptor in : morph.injects) {
                MethodNode targetMethod = findMethod(target, in.targetName(), in.targetDescriptor());
                if (targetMethod == null) {
                    if (in.requireTarget()) {
                        errors.add(new VifadaError.TargetMethodNotFound(
                                morph.morphInternal, target.name, in.targetMethod()));
                    }
                    continue;
                }
                VifadaError sigErr = validateInjectSignature(morph, in, targetMethod);
                if (sigErr != null) { errors.add(sigErr); continue; }

                if (in.at() != InjectionPoint.HEAD && in.at() != InjectionPoint.RETURN) {
                    errors.add(new VifadaError.UnsupportedAt(
                            morph.morphInternal, in.method().name + in.method().desc, in.at()));
                    continue;
                }

                // Генерируем helper-метод в целевом классе.
                String helperName = uniqueHelperName(usedHelperNames,
                        "vida$inject$" + sanitize(in.targetName()) + "$" + (helperCounter++));
                MethodNode helper = createHelper(target, helperName, in.method(), morph.morphInternal);
                target.methods.add(helper);
                usedHelperNames.add(helper.name + helper.desc);

                // Вставляем вызов helper'а в нужные места.
                insertInjectCall(target.name, targetMethod, helper, in.at());
            }
        }

        return errors;
    }

    // ===================================================================
    //                         OVERWRITE LOGIC
    // ===================================================================

    private static void replaceBody(ClassNode target, MethodNode targetMethod,
                                    MethodNode morphMethod, String morphInternal) {
        MethodNode remapped = copyMethodRemapped(morphMethod, morphInternal, target.name);
        // Сохраняем имя/дескриптор целевого метода, берём тело от морфа.
        targetMethod.instructions = remapped.instructions;
        targetMethod.tryCatchBlocks = remapped.tryCatchBlocks;
        targetMethod.localVariables = remapped.localVariables;
        targetMethod.maxLocals = Math.max(targetMethod.maxLocals, remapped.maxLocals);
        targetMethod.maxStack  = Math.max(targetMethod.maxStack,  remapped.maxStack);
    }

    // ===================================================================
    //                          INJECT LOGIC
    // ===================================================================

    private static VifadaError validateInjectSignature(MorphDescriptor morph,
                                                       InjectDescriptor in,
                                                       MethodNode targetMethod) {
        // Сигнатура injected метода: (targetArgs..., CallbackInfo) -> void.
        Type targetT = Type.getMethodType(targetMethod.desc);
        Type[] tArgs = targetT.getArgumentTypes();
        Type injectT = Type.getMethodType(in.method().desc);
        Type[] iArgs = injectT.getArgumentTypes();

        Type expectedRet = Type.VOID_TYPE;
        if (!injectT.getReturnType().equals(expectedRet)) {
            return new VifadaError.SignatureMismatch(
                    morph.morphInternal, in.method().name + in.method().desc,
                    "return=void", "return=" + injectT.getReturnType().getClassName());
        }

        if (iArgs.length != tArgs.length + 1) {
            return new VifadaError.SignatureMismatch(
                    morph.morphInternal, in.method().name + in.method().desc,
                    "args=" + tArgs.length + "+CallbackInfo",
                    "args=" + iArgs.length);
        }
        for (int i = 0; i < tArgs.length; i++) {
            if (!tArgs[i].equals(iArgs[i])) {
                return new VifadaError.SignatureMismatch(
                        morph.morphInternal, in.method().name + in.method().desc,
                        "arg[" + i + "]=" + tArgs[i].getClassName(),
                        "arg[" + i + "]=" + iArgs[i].getClassName());
            }
        }
        if (!iArgs[iArgs.length - 1].equals(AsmNames.CALLBACK_INFO_TYPE)) {
            return new VifadaError.SignatureMismatch(
                    morph.morphInternal, in.method().name + in.method().desc,
                    "last arg=CallbackInfo",
                    "last arg=" + iArgs[iArgs.length - 1].getClassName());
        }
        return null;
    }

    private static MethodNode createHelper(ClassNode target, String helperName,
                                           MethodNode source, String morphInternal) {
        MethodNode copy = copyMethodRemapped(source, morphInternal, target.name);
        copy.name = helperName;
        copy.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
        // ACC_STATIC оставляем/выставляем как у исходника: инстанс-метод
        // морфа копируется как инстанс-метод целевого (this == target).
        if ((source.access & Opcodes.ACC_STATIC) != 0) {
            copy.access |= Opcodes.ACC_STATIC;
        }
        return copy;
    }

    private static void insertInjectCall(String targetOwner, MethodNode targetMethod,
                                         MethodNode helper, InjectionPoint at) {
        boolean targetStatic = (targetMethod.access & Opcodes.ACC_STATIC) != 0;
        boolean helperStatic = (helper.access & Opcodes.ACC_STATIC) != 0;
        Type targetType = Type.getMethodType(targetMethod.desc);
        Type[] targetArgs = targetType.getArgumentTypes();

        int ciSlot = targetMethod.maxLocals;
        targetMethod.maxLocals = ciSlot + 1;

        if (at == InjectionPoint.HEAD) {
            InsnList pre = buildHeadInjection(targetOwner, targetMethod, helper,
                    targetStatic, helperStatic, targetArgs, ciSlot, targetType.getReturnType());
            targetMethod.instructions.insert(pre);
        } else { // RETURN
            InsnList pre = buildInitCi(targetMethod, ciSlot);
            targetMethod.instructions.insert(pre);

            InsnList instructions = targetMethod.instructions;
            AbstractInsnNode cur = instructions.getFirst();
            while (cur != null) {
                AbstractInsnNode next = cur.getNext();
                if (isReturnInsn(cur)) {
                    InsnList callHook = buildReturnInjection(targetOwner, helper,
                            targetStatic, helperStatic, targetArgs, ciSlot);
                    instructions.insertBefore(cur, callHook);
                }
                cur = next;
            }
        }

        bumpStackForInjection(targetMethod, targetArgs.length + 2);
    }

    private static InsnList buildInitCi(MethodNode targetMethod, int ciSlot) {
        InsnList il = new InsnList();
        il.add(new TypeInsnNode(Opcodes.NEW, AsmNames.CALLBACK_INFO_INTERNAL));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new LdcInsnNode(targetMethod.name));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                AsmNames.CALLBACK_INFO_INTERNAL,
                "<init>", "(Ljava/lang/String;)V", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, ciSlot));
        return il;
    }

    private static InsnList buildHeadInjection(String targetOwner, MethodNode targetMethod,
                                               MethodNode helper,
                                               boolean targetStatic, boolean helperStatic,
                                               Type[] targetArgs, int ciSlot,
                                               Type returnType) {
        InsnList il = buildInitCi(targetMethod, ciSlot);

        if (!helperStatic) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        pushTargetArgs(il, targetStatic, targetArgs);
        il.add(new VarInsnNode(Opcodes.ALOAD, ciSlot));

        int opcode = helperStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
        il.add(new MethodInsnNode(opcode, targetOwner, helper.name, helper.desc, false));

        LabelNode skip = new LabelNode();
        il.add(new VarInsnNode(Opcodes.ALOAD, ciSlot));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                AsmNames.CALLBACK_INFO_INTERNAL,
                "isCancelled", "()Z", false));
        il.add(new JumpInsnNode(Opcodes.IFEQ, skip));
        appendDefaultReturn(il, returnType);
        il.add(skip);
        return il;
    }

    private static InsnList buildReturnInjection(String targetOwner, MethodNode helper,
                                                 boolean targetStatic, boolean helperStatic,
                                                 Type[] targetArgs, int ciSlot) {
        InsnList il = new InsnList();
        if (!helperStatic) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        pushTargetArgs(il, targetStatic, targetArgs);
        il.add(new VarInsnNode(Opcodes.ALOAD, ciSlot));
        int opcode = helperStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
        il.add(new MethodInsnNode(opcode, targetOwner, helper.name, helper.desc, false));
        return il;
    }

    private static void pushTargetArgs(InsnList il, boolean targetStatic, Type[] targetArgs) {
        int slot = targetStatic ? 0 : 1;
        for (Type t : targetArgs) {
            il.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
            slot += t.getSize();
        }
    }

    private static void appendDefaultReturn(InsnList il, Type retType) {
        int sort = retType.getSort();
        switch (sort) {
            case Type.VOID -> il.add(new InsnNode(Opcodes.RETURN));
            case Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.CHAR, Type.INT -> {
                il.add(new InsnNode(Opcodes.ICONST_0));
                il.add(new InsnNode(Opcodes.IRETURN));
            }
            case Type.LONG -> {
                il.add(new InsnNode(Opcodes.LCONST_0));
                il.add(new InsnNode(Opcodes.LRETURN));
            }
            case Type.FLOAT -> {
                il.add(new InsnNode(Opcodes.FCONST_0));
                il.add(new InsnNode(Opcodes.FRETURN));
            }
            case Type.DOUBLE -> {
                il.add(new InsnNode(Opcodes.DCONST_0));
                il.add(new InsnNode(Opcodes.DRETURN));
            }
            case Type.OBJECT, Type.ARRAY -> {
                il.add(new InsnNode(Opcodes.ACONST_NULL));
                il.add(new InsnNode(Opcodes.ARETURN));
            }
            default -> throw new IllegalStateException("unexpected type sort: " + sort);
        }
    }

    private static boolean isReturnInsn(AbstractInsnNode n) {
        int op = n.getOpcode();
        return op == Opcodes.RETURN || op == Opcodes.IRETURN || op == Opcodes.ARETURN
                || op == Opcodes.LRETURN || op == Opcodes.FRETURN || op == Opcodes.DRETURN;
    }

    private static void bumpStackForInjection(MethodNode m, int extraStack) {
        m.maxStack = Math.max(m.maxStack, extraStack + 3);
    }

    // ===================================================================
    //                         BYTECODE COPY
    // ===================================================================

    /**
     * Клонирует {@link MethodNode} и одновременно переименовывает все
     * ссылки на {@code fromOwner} в {@code toOwner} — в инструкциях,
     * try/catch, локальных переменных и аннотациях параметров.
     */
    static MethodNode copyMethodRemapped(MethodNode src, String fromOwner, String toOwner) {
        Remapper remapper = new Remapper() {
            @Override public String map(String internalName) {
                return fromOwner.equals(internalName) ? toOwner : internalName;
            }
        };
        MethodNode copy = new MethodNode(AsmNames.API, src.access, src.name, src.desc,
                src.signature, src.exceptions == null ? null : src.exceptions.toArray(new String[0]));
        // Копируем через accept → new MethodNode с ремапом через MethodRemapper — проще сделать
        // вручную в рамках нашей задачи: нам нужно менять только owner инструкций.
        src.accept(copy);

        // Ремап инструкций вручную — MethodRemapper избыточен для нашего узкого случая.
        for (AbstractInsnNode ins : copy.instructions) {
            if (ins instanceof FieldInsnNode f) {
                if (fromOwner.equals(f.owner)) f.owner = toOwner;
                f.desc = remapper.mapDesc(f.desc);
            } else if (ins instanceof MethodInsnNode mi) {
                if (fromOwner.equals(mi.owner)) mi.owner = toOwner;
                mi.desc = remapper.mapMethodDesc(mi.desc);
            } else if (ins instanceof TypeInsnNode t) {
                if (fromOwner.equals(t.desc)) t.desc = toOwner;
            } else if (ins instanceof LdcInsnNode l) {
                if (l.cst instanceof Type tc) {
                    l.cst = Type.getType(remapper.mapDesc(tc.getDescriptor()));
                }
            }
        }
        if (copy.localVariables != null) {
            for (var lv : copy.localVariables) {
                lv.desc = remapper.mapDesc(lv.desc);
            }
        }
        if (copy.tryCatchBlocks != null) {
            for (var tc : copy.tryCatchBlocks) {
                if (tc.type != null && fromOwner.equals(tc.type)) tc.type = toOwner;
            }
        }
        return copy;
    }

    // ===================================================================
    //                             HELPERS
    // ===================================================================

    private static MethodNode findMethod(ClassNode cn, String name, String desc) {
        if (cn.methods == null) return null;
        for (MethodNode m : cn.methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) return m;
        }
        return null;
    }

    private static boolean hasMember(ClassNode cn, ShadowDescriptor sh) {
        if (sh.kind() == ShadowDescriptor.Kind.FIELD) {
            if (cn.fields == null) return false;
            for (FieldNode f : cn.fields) {
                if (f.name.equals(sh.name()) && f.desc.equals(sh.descriptor())) return true;
            }
            return false;
        }
        if (cn.methods == null) return false;
        for (MethodNode m : cn.methods) {
            if (m.name.equals(sh.name()) && m.desc.equals(sh.descriptor())) return true;
        }
        return false;
    }

    private static Set<String> collectMethodKeys(ClassNode cn) {
        Set<String> out = new HashSet<>();
        if (cn.methods != null) {
            for (MethodNode m : cn.methods) out.add(m.name + m.desc);
        }
        return out;
    }

    private static String uniqueHelperName(Set<String> used, String base) {
        String name = base;
        int i = 0;
        while (containsAny(used, name + "(")) {
            name = base + "$" + (i++);
        }
        return name;
    }

    private static boolean containsAny(Set<String> used, String prefix) {
        for (String s : used) if (s.startsWith(prefix)) return true;
        return false;
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isJavaIdentifierPart(c) ? c : '_');
        }
        return sb.toString();
    }

    private static String renderShadow(ShadowDescriptor sh) {
        return sh.kind().name().toLowerCase() + ":" + sh.name() + sh.descriptor();
    }
}
