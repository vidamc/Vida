/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.cartografia.ClassMapping;
import dev.vida.cartografia.FieldMapping;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.MethodMapping;
import dev.vida.cartografia.Namespace;
import dev.vida.core.ApiStatus;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import org.objectweb.asm.Type;

/**
 * Рефлексия по именам Mojmap при обфусцированном клиенте: поля/методы/классы
 * резолвятся через {@link MappingTree} из {@code client_mappings.txt}.
 */
@ApiStatus.Internal
public final class McObfReflect {

    private McObfReflect() {}

    public static Optional<MappingTree> mappingTree() {
        return VidaRuntime.maybeCurrent().flatMap(e -> e.clientMappings());
    }

    /**
     * Поле экземпляра по mojmap-имени (например {@code list}, {@code options}),
     * поднимаясь по реальной иерархии классов загрузчика игры.
     */
    public static Object getInstanceFieldMojmap(Object instance, String mojmapFieldName) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(mojmapFieldName, "mojmapFieldName");
        Optional<MappingTree> tree = mappingTree();
        if (tree.isEmpty()) {
            return getInstanceFieldLegacy(instance, mojmapFieldName);
        }
        MappingTree t = tree.get();
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass()) {
            String obfInternal = c.getName().replace('.', '/');
            ClassMapping cm = t.classByName(Namespace.OBF, obfInternal);
            if (cm == null) {
                continue;
            }
            for (FieldMapping fm : cm.fields()) {
                if (!mojmapFieldName.equals(fm.name(Namespace.MOJMAP))) {
                    continue;
                }
                try {
                    Field f = c.getDeclaredField(fm.name(Namespace.OBF));
                    f.setAccessible(true);
                    return f.get(instance);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Object getInstanceFieldLegacy(Object instance, String fieldName) {
        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(instance);
            } catch (NoSuchFieldException ignored) {
                // siguiente
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Загрузить класс: при наличии дерева — по mojmap internal/binary имени,
     * иначе {@link Class#forName(String, boolean, ClassLoader)} с исходной строкой.
     */
    public static Class<?> classForMojmap(ClassLoader loader, String mojmapBinaryName)
            throws ClassNotFoundException {
        Objects.requireNonNull(mojmapBinaryName, "mojmapBinaryName");
        ClassLoader cl = loader == null ? ClassLoader.getSystemClassLoader() : loader;
        Optional<MappingTree> tree = mappingTree();
        if (tree.isEmpty()) {
            return Class.forName(mojmapBinaryName, false, cl);
        }
        String internal = mojmapBinaryName.replace('.', '/');
        ClassMapping cm = tree.get().classByName(Namespace.MOJMAP, internal);
        if (cm == null) {
            return Class.forName(mojmapBinaryName, false, cl);
        }
        String obf = cm.name(Namespace.OBF);
        if (obf == null) {
            return Class.forName(mojmapBinaryName, false, cl);
        }
        return Class.forName(obf.replace('/', '.'), false, cl);
    }

    /**
     * Объявленный метод на {@code runtimeOwner} или суперклассах,
     * совпадающий по mojmap имени и JVM-дескриптору сигнатуры (в пространстве mojmap).
     */
    public static Method declaredMethodMojmap(Class<?> runtimeOwner, String mojmapName, String mojmapDescriptor) {
        Objects.requireNonNull(runtimeOwner, "runtimeOwner");
        Objects.requireNonNull(mojmapName, "mojmapName");
        Objects.requireNonNull(mojmapDescriptor, "mojmapDescriptor");
        Optional<MappingTree> tree = mappingTree();
        if (tree.isEmpty()) {
            return declaredMethodLegacy(runtimeOwner, mojmapName, mojmapDescriptor);
        }
        MappingTree t = tree.get();
        String obfSig;
        try {
            obfSig = t.remapDescriptor(Namespace.MOJMAP, Namespace.OBF, mojmapDescriptor);
        } catch (RuntimeException ex) {
            obfSig = mojmapDescriptor;
        }
        ClassLoader cl = runtimeOwner.getClassLoader();
        for (Class<?> c = runtimeOwner; c != null; c = c.getSuperclass()) {
            String obfInternal = c.getName().replace('.', '/');
            ClassMapping cm = t.classByName(Namespace.OBF, obfInternal);
            if (cm == null) {
                continue;
            }
            for (MethodMapping mm : cm.methods()) {
                if (!mojmapName.equals(mm.name(Namespace.MOJMAP))) {
                    continue;
                }
                if (!obfSig.equals(mm.sourceDescriptor())) {
                    continue;
                }
                try {
                    Method m = resolveMethod(c, mm.sourceName(), mm.sourceDescriptor(), cl);
                    m.setAccessible(true);
                    return m;
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Method declaredMethodLegacy(Class<?> runtimeOwner, String name, String mojmapDescriptor) {
        try {
            Class<?>[] params = parameterClasses(mojmapDescriptor, runtimeOwner.getClassLoader());
            for (Class<?> c = runtimeOwner; c != null; c = c.getSuperclass()) {
                try {
                    Method m = c.getDeclaredMethod(name, params);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {
                    // siguiente clase
                }
            }
        } catch (ClassNotFoundException ignored) {
            // fall through
        }
        return null;
    }

    /**
     * Статический метод на {@code declaringClass}.
     */
    public static Method staticMethodMojmap(Class<?> declaringClass, String mojmapName, String mojmapDescriptor) {
        Method m = declaredMethodMojmap(declaringClass, mojmapName, mojmapDescriptor);
        if (m != null && !Modifier.isStatic(m.getModifiers())) {
            return null;
        }
        return m;
    }

    private static Method resolveMethod(Class<?> owner, String obfName, String obfDescriptor, ClassLoader cl)
            throws NoSuchMethodException, ClassNotFoundException {
        Class<?>[] params = parameterClasses(obfDescriptor, cl);
        Method m = owner.getDeclaredMethod(obfName, params);
        return m;
    }

    static Class<?>[] parameterClasses(String methodDescriptor, ClassLoader cl) throws ClassNotFoundException {
        Type[] args = Type.getArgumentTypes(methodDescriptor);
        Class<?>[] out = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = asmTypeToClass(args[i], cl);
        }
        return out;
    }

    static Class<?> asmTypeToClass(Type t, ClassLoader cl) throws ClassNotFoundException {
        return switch (t.getSort()) {
            case Type.VOID -> void.class;
            case Type.BOOLEAN -> boolean.class;
            case Type.CHAR -> char.class;
            case Type.BYTE -> byte.class;
            case Type.SHORT -> short.class;
            case Type.INT -> int.class;
            case Type.FLOAT -> float.class;
            case Type.LONG -> long.class;
            case Type.DOUBLE -> double.class;
            case Type.ARRAY -> Class.forName(t.getDescriptor().replace('/', '.'), false, cl);
            case Type.OBJECT -> Class.forName(t.getInternalName().replace('/', '.'), false, cl);
            default -> throw new ClassNotFoundException("unsupported ASM sort: " + t.getSort());
        };
    }

    /**
     * Конструктор по списку типов параметров (уже резолвнутых в обф-классы).
     */
    public static Constructor<?> declaredConstructor(Class<?> clazz, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Constructor<?> c = clazz.getDeclaredConstructor(parameterTypes);
        c.setAccessible(true);
        return c;
    }
}
