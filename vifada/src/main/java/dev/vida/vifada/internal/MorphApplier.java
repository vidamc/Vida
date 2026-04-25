/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.MorphMethodResolution;
import dev.vida.vifada.VifadaError;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
 *   <li>{@code @VifadaInject} / {@code @VifadaMulti} — HEAD/RETURN;</li>
 *   <li>{@code @VifadaRedirect} — замена совпадающего {@link MethodInsnNode};
 *       {@code @VifadaLocal} на параметрах инъектора — только HEAD.</li>
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
    /**
     * @param morphTargetClassInternal ключ Mojmap (как в {@code @VifadaMorph#target}),
     *                                  если байткод класса обфусцирован и {@code target.name}
     *                                  не совпадает с этим ключом; иначе {@code null}
     */
    public static List<VifadaError> apply(
            ClassNode target,
            List<MorphDescriptor> morphs,
            String morphTargetClassInternal,
            MorphMethodResolution methodResolution) {
        List<VifadaError> errors = new ArrayList<>();

        // Упорядочим морфы по priority, стабилизируем по morphInternal для детерминизма.
        List<MorphDescriptor> ordered = new ArrayList<>(morphs);
        ordered.sort(Comparator.<MorphDescriptor>comparingInt(d -> d.priority)
                .thenComparing(d -> d.morphInternal));

        detectInjectConflicts(target, morphTargetClassInternal, ordered, errors);

        Map<String, MethodNode> methodResolutionCache = new HashMap<>();

        Set<String> usedHelperNames = collectMethodKeys(target);
        int helperCounter = 0;

        for (MorphDescriptor morph : ordered) {
            // ---- Sanity: морф должен таргетить именно этот класс. ---------
            String expectedClass = morphTargetClassInternal != null
                    ? morphTargetClassInternal
                    : target.name;
            if (!morph.targetInternal.equals(expectedClass)) {
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
                MethodNode targetMethod = findMethod(
                        target, ow.targetName(), ow.targetDescriptor(), methodResolution,
                        methodResolutionCache);
                if (targetMethod == null) {
                    if (!ow.silentMissing()) {
                        errors.add(new VifadaError.TargetMethodNotFound(
                                morph.morphInternal, target.name, ow.targetMethod()));
                    }
                    continue;
                }
                replaceBody(target, targetMethod, ow.method(), morph.morphInternal);
            }

            // ---- @VifadaRedirect ------------------------------------------
            for (RedirectDescriptor rd : morph.redirects) {
                MethodNode container = findMethod(target, rd.containerName(),
                        rd.containerDescriptor(), methodResolution, methodResolutionCache);
                if (container == null) {
                    if (rd.requireTarget()) {
                        errors.add(new VifadaError.TargetMethodNotFound(
                                morph.morphInternal, target.name, rd.containerMethodSpec()));
                    }
                    continue;
                }
                MethodInsnNode invoke = findInvokeSite(container,
                        rd.invokeOwner(), rd.invokeName(), rd.invokeDescriptor(), rd.ordinal());
                if (invoke == null) {
                    if (rd.requireTarget()) {
                        errors.add(new VifadaError.BadMorph(morph.morphInternal,
                                "redirect: no invocation " + rd.invokeOwner() + "." + rd.invokeName()
                                        + rd.invokeDescriptor() + " in " + rd.containerMethodSpec()));
                    }
                    continue;
                }
                VifadaError redErr = validateRedirectMorph(morph.morphInternal, invoke, rd.morphMethod());
                if (redErr != null) {
                    errors.add(redErr);
                    continue;
                }
                String helperName = uniqueHelperName(usedHelperNames,
                        "vida$redirect$" + sanitize(rd.containerName()) + "$" + (helperCounter++));
                MethodNode helper = copyMethodRemapped(rd.morphMethod(), morph.morphInternal, target.name);
                helper.name = helperName;
                helper.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
                target.methods.add(helper);
                usedHelperNames.add(helper.name + helper.desc);

                InsnList rep = new InsnList();
                rep.add(new MethodInsnNode(Opcodes.INVOKESTATIC, target.name, helper.name,
                        helper.desc, false));
                container.instructions.insertBefore(invoke, rep);
                container.instructions.remove(invoke);
                bumpStackForInjection(container,
                        Type.getArgumentTypes(invoke.desc).length + 2);
            }

            // ---- @VifadaInject --------------------------------------------
            for (InjectDescriptor in : morph.injects) {
                MethodNode targetMethod = findMethod(
                        target, in.targetName(), in.targetDescriptor(), methodResolution,
                        methodResolutionCache);
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

                if (!in.localBindings().isEmpty() && in.at() != InjectionPoint.HEAD) {
                    errors.add(new VifadaError.BadMorph(morph.morphInternal,
                            "@VifadaLocal is only supported at InjectionPoint.HEAD"));
                    continue;
                }
                if (!in.localBindings().isEmpty()) {
                    VifadaError locErr = validateLocalBindings(morph.morphInternal, targetMethod, in);
                    if (locErr != null) {
                        errors.add(locErr);
                        continue;
                    }
                }

                // Генерируем helper-метод в целевом классе.
                String helperName = uniqueHelperName(usedHelperNames,
                        "vida$inject$" + sanitize(in.targetName()) + "$" + (helperCounter++));
                MethodNode helper = createHelper(target, helperName, in.method(), morph.morphInternal);
                target.methods.add(helper);
                usedHelperNames.add(helper.name + helper.desc);

                // Вставляем вызов helper'а в нужные места.
                insertInjectCall(target.name, targetMethod, helper, in);
            }
        }

        return errors;
    }

    private static void detectInjectConflicts(
            ClassNode target,
            String morphTargetClassInternal,
            List<MorphDescriptor> ordered,
            List<VifadaError> errors) {
        String matchKey = morphTargetClassInternal != null ? morphTargetClassInternal : target.name;
        Map<String, List<MorphDescriptor>> bySlot = new LinkedHashMap<>();
        for (MorphDescriptor morph : ordered) {
            if (!morph.targetInternal.equals(matchKey)) {
                continue;
            }
            for (InjectDescriptor in : morph.injects) {
                String key = in.targetMethod() + "|" + in.at();
                bySlot.computeIfAbsent(key, k -> new ArrayList<>()).add(morph);
            }
        }
        String cls = matchKey.replace('/', '.');
        for (var e : bySlot.entrySet()) {
            List<MorphDescriptor> ms = e.getValue();
            Set<String> distinct = new LinkedHashSet<>();
            for (MorphDescriptor m : ms) {
                distinct.add(m.morphInternal);
            }
            if (distinct.size() <= 1) {
                continue;
            }
            int p0 = ms.get(0).priority;
            boolean samePri = ms.stream().allMatch(m -> m.priority == p0);
            if (!samePri) {
                continue;
            }
            List<String> names = new ArrayList<>(distinct);
            errors.add(new VifadaError.MorphConflict(
                    cls,
                    e.getKey(),
                    names.get(0).replace('/', '.'),
                    names.get(1).replace('/', '.'),
                    p0,
                    "use distinct @VifadaMorph.priority values (lower applies first)"));
        }
    }

    private static VifadaError validateLocalBindings(
            String morphInternal, MethodNode targetMethod, InjectDescriptor in) {
        for (VifadaLocalBinding b : in.localBindings()) {
            int slot = LocalSlotResolver.resolveHeadSlot(targetMethod, b);
            if (slot < 0) {
                return new VifadaError.BadMorph(morphInternal,
                        "@VifadaLocal: no matching LVT entry for parameter index "
                                + b.parameterIndex() + " ordinal=" + b.ordinal());
            }
        }
        return null;
    }

    private static MethodInsnNode findInvokeSite(
            MethodNode container,
            String owner,
            String name,
            String desc,
            int ordinal) {
        int seen = 0;
        for (AbstractInsnNode ins = container.instructions.getFirst();
             ins != null;
             ins = ins.getNext()) {
            if (!(ins instanceof MethodInsnNode mi)) {
                continue;
            }
            if (!owner.equals(mi.owner) || !name.equals(mi.name) || !desc.equals(mi.desc)) {
                continue;
            }
            if (seen++ == ordinal) {
                return mi;
            }
        }
        return null;
    }

    private static VifadaError validateRedirectMorph(
            String morphInternal, MethodInsnNode invoke, MethodNode morphMethod) {
        int op = invoke.getOpcode();
        if (op != Opcodes.INVOKESTATIC) {
            return new VifadaError.BadMorph(morphInternal,
                    "@VifadaRedirect currently supports only INVOKESTATIC call-sites");
        }
        if ((morphMethod.access & Opcodes.ACC_STATIC) == 0) {
            return new VifadaError.BadMorph(morphInternal,
                    "redirect morph method must be static for INVOKESTATIC targets");
        }
        if (!invoke.desc.equals(morphMethod.desc)) {
            return new VifadaError.SignatureMismatch(morphInternal,
                    morphMethod.name + morphMethod.desc,
                    "descriptor " + invoke.desc,
                    morphMethod.desc);
        }
        return null;
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
                                         MethodNode helper, InjectDescriptor in) {
        boolean targetStatic = (targetMethod.access & Opcodes.ACC_STATIC) != 0;
        boolean helperStatic = (helper.access & Opcodes.ACC_STATIC) != 0;
        Type targetType = Type.getMethodType(targetMethod.desc);
        Type[] targetArgs = targetType.getArgumentTypes();

        int ciSlot = targetMethod.maxLocals;
        targetMethod.maxLocals = ciSlot + 1;

        if (in.at() == InjectionPoint.HEAD) {
            InsnList pre = buildHeadInjection(targetOwner, targetMethod, helper,
                    targetStatic, helperStatic, targetArgs, ciSlot, targetType.getReturnType(), in);
            targetMethod.instructions.insert(pre);
        } else {
            InsnList pre = buildInitCi(targetMethod, ciSlot);
            targetMethod.instructions.insert(pre);

            InsnList instructions = targetMethod.instructions;
            AbstractInsnNode cur = instructions.getFirst();
            while (cur != null) {
                AbstractInsnNode next = cur.getNext();
                if (isReturnInsn(cur)) {
                    InsnList callHook = buildReturnInjection(targetOwner, targetMethod, helper,
                            targetStatic, helperStatic, targetArgs, ciSlot, in);
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
                                               Type returnType, InjectDescriptor in) {
        InsnList il = buildInitCi(targetMethod, ciSlot);

        if (!helperStatic) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        pushInjectArgs(il, targetMethod, targetStatic, targetArgs, in);
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

    private static InsnList buildReturnInjection(String targetOwner, MethodNode targetMethod,
                                                 MethodNode helper,
                                                 boolean targetStatic, boolean helperStatic,
                                                 Type[] targetArgs, int ciSlot,
                                                 InjectDescriptor in) {
        InsnList il = new InsnList();
        if (!helperStatic) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        pushInjectArgs(il, targetMethod, targetStatic, targetArgs, in);
        il.add(new VarInsnNode(Opcodes.ALOAD, ciSlot));
        int opcode = helperStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
        il.add(new MethodInsnNode(opcode, targetOwner, helper.name, helper.desc, false));
        return il;
    }

    /**
     * @param targetMethod required for HEAD local bindings; may be {@code null} for RETURN path
     *                     when bindings are empty
     */
    private static void pushInjectArgs(
            InsnList il,
            MethodNode targetMethod,
            boolean targetStatic,
            Type[] targetArgs,
            InjectDescriptor in) {
        for (int i = 0; i < targetArgs.length; i++) {
            Type t = targetArgs[i];
            int slot = slotForInjectArg(targetMethod, targetStatic, targetArgs, i, in);
            il.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
        }
    }

    private static int slotForInjectArg(
            MethodNode targetMethod,
            boolean targetStatic,
            Type[] targetArgs,
            int paramIndex,
            InjectDescriptor in) {
        for (VifadaLocalBinding b : in.localBindings()) {
            if (b.parameterIndex() == paramIndex) {
                return LocalSlotResolver.resolveHeadSlot(targetMethod, b);
            }
        }
        return defaultArgSlot(targetStatic, targetArgs, paramIndex);
    }

    private static int defaultArgSlot(boolean targetStatic, Type[] targetArgs, int paramIndex) {
        int slot = targetStatic ? 0 : 1;
        for (int j = 0; j < paramIndex; j++) {
            slot += targetArgs[j].getSize();
        }
        return slot;
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

    private static MethodNode findMethod(
            ClassNode cn,
            String name,
            String desc,
            MorphMethodResolution resolution,
            Map<String, MethodNode> cache) {
        String k = name + '\0' + desc;
        MethodNode hit = cache.get(k);
        if (hit != null) {
            return hit;
        }
        MethodNode direct = findMethodExact(cn, name, desc);
        if (direct != null) {
            cache.put(k, direct);
            return direct;
        }
        if (resolution == null) {
            return null;
        }
        String[] obf = resolution.resolveObfMethod(cn.name, name, desc);
        if (obf == null || obf.length != 2) {
            return null;
        }
        MethodNode via = findMethodExact(cn, obf[0], obf[1]);
        if (via != null) {
            cache.put(k, via);
        }
        return via;
    }

    private static MethodNode findMethodExact(ClassNode cn, String name, String desc) {
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

    /** Обратная совместимость: без Mojmap-ключа и без резолва методов. */
    public static List<VifadaError> apply(ClassNode target, List<MorphDescriptor> morphs) {
        return apply(target, morphs, null, null);
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
