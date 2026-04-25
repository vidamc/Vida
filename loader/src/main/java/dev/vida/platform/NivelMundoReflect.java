/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.bloque.registro.EtiquetaBloque;
import dev.vida.core.Identifier;
import dev.vida.mundo.Coordenada;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Выборка блока и проверки тегов через vanilla {@code Level}/{@code BlockState} и закэшированные
 * {@link MethodHandle}.
 */
final class NivelMundoReflect {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private static volatile MethodHandle ctorBlockPos;
    private static volatile MethodHandle levelGetBlockState;
    private static volatile MethodHandle blockStateGetBlock;
    private static volatile Object singletonRegistryBlock;
    private static volatile MethodHandle registryBlockGetKey;
    private static volatile MethodHandle resourceKeyLocation;
    private static volatile MethodHandle resourceLocationNamespace;
    private static volatile MethodHandle resourceLocationPath;
    private static volatile MethodHandle tagKeyCreate;
    private static volatile Object registryKeyBlock;
    private static volatile MethodHandle resourceLocationFromNsPath;
    private static volatile MethodHandle blockStateIsTagKey;
    private static volatile MethodHandle levelHasChunk;

    private NivelMundoReflect() {}

    /** Для биомов и блоков — один конструктор {@code BlockPos}. */
    static Object blockPosXYZ(int x, int y, int z) throws Throwable {
        return nuevoBlockPos(x, y, z);
    }

    static Optional<Identifier> bloqueRegistradoEn(Object level, Coordenada c) {
        if (level == null || c == null) {
            return Optional.empty();
        }
        try {
            Object pos = nuevoBlockPos(c.x(), c.y(), c.z());
            Object state = nivelGetBlockState(level, pos);
            if (state == null) {
                return Optional.empty();
            }
            Object block = blockStateGetBlockInvoke(state);
            if (block == null) {
                return Optional.empty();
            }
            Object registry = obtenerRegistryBlock();
            Optional<?> optKey = invocarRegistryGetKey(registry, block);
            if (optKey == null || optKey.isEmpty()) {
                return Optional.empty();
            }
            Object rk = optKey.get();
            Object rl = invokeLocationDesdeResourceKey(rk);
            String ns = (String) invokeRlNs(rl);
            String path = (String) invokeRlPath(rl);
            return Optional.of(Identifier.of(ns, path));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    static boolean bloqueTieneEtiqueta(Object level, Coordenada c, EtiquetaBloque etiqueta) {
        if (level == null || etiqueta == null || c == null) {
            return false;
        }
        try {
            Object pos = nuevoBlockPos(c.x(), c.y(), c.z());
            Object state = nivelGetBlockState(level, pos);
            if (state == null) {
                return false;
            }
            Object tag = crearTagBloque(etiqueta);
            MethodHandle is = obtenerBlockStateIs(state);
            if (is == null || tag == null) {
                return false;
            }
            Object out = is.invoke(state, tag);
            return Boolean.TRUE.equals(out);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean tieneChunkSiDisponible(Object level, int chunkX, int chunkZ) {
        if (level == null) {
            return false;
        }
        MethodHandle mh = nivelHasChunk();
        if (mh == null) {
            return true;
        }
        try {
            Object r = mh.invoke(level, chunkX, chunkZ);
            return Boolean.TRUE.equals(r);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static Object nuevoBlockPos(int x, int y, int z) throws Throwable {
        MethodHandle mh = ctorBlockPos;
        if (mh == null) {
            synchronized (NivelMundoReflect.class) {
                if (ctorBlockPos == null) {
                    Class<?> bp = Class.forName("net.minecraft.core.BlockPos");
                    ctorBlockPos = LOOKUP.findConstructor(bp, MethodType.methodType(void.class, int.class, int.class, int.class));
                }
                mh = ctorBlockPos;
            }
        }
        return mh.invoke(x, y, z);
    }

    private static Object nivelGetBlockState(Object level, Object pos) throws Throwable {
        MethodHandle mh = levelGetBlockState;
        if (mh == null) {
            synchronized (NivelMundoReflect.class) {
                if (levelGetBlockState == null) {
                    Class<?> bp = Class.forName("net.minecraft.core.BlockPos");
                    Method m = level.getClass().getMethod("getBlockState", bp);
                    levelGetBlockState = LOOKUP.unreflect(m);
                }
                mh = levelGetBlockState;
            }
        }
        return mh.invoke(level, pos);
    }

    private static Object blockStateGetBlockInvoke(Object state) throws Throwable {
        MethodHandle mh = blockStateGetBlock;
        if (mh == null) {
            synchronized (NivelMundoReflect.class) {
                if (blockStateGetBlock == null) {
                    Method m = state.getClass().getMethod("getBlock");
                    blockStateGetBlock = LOOKUP.unreflect(m);
                }
                mh = blockStateGetBlock;
            }
        }
        return mh.invoke(state);
    }

    private static Object obtenerRegistryBlock() throws ReflectiveOperationException {
        if (singletonRegistryBlock != null) {
            return singletonRegistryBlock;
        }
        synchronized (NivelMundoReflect.class) {
            if (singletonRegistryBlock != null) {
                return singletonRegistryBlock;
            }
            Class<?> builtIn = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            singletonRegistryBlock = builtIn.getField("BLOCK").get(null);
            return singletonRegistryBlock;
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<?> invocarRegistryGetKey(Object registry, Object block) throws Throwable {
        MethodHandle mh = registryBlockGetKey;
        if (mh == null) {
            synchronized (NivelMundoReflect.class) {
                if (registryBlockGetKey == null) {
                    Method m = registry.getClass().getMethod("getKey", Object.class);
                    registryBlockGetKey = LOOKUP.unreflect(m);
                }
                mh = registryBlockGetKey;
            }
        }
        return (Optional<?>) mh.invoke(registry, block);
    }

    private static Object invokeLocationDesdeResourceKey(Object resourceKey) throws Throwable {
        MethodHandle mh = resourceKeyLocation;
        if (mh == null) {
            synchronized (NivelMundoReflect.class) {
                if (resourceKeyLocation == null) {
                    Method m = resourceKey.getClass().getMethod("location");
                    resourceKeyLocation = LOOKUP.unreflect(m);
                }
                mh = resourceKeyLocation;
            }
        }
        return mh.invoke(resourceKey);
    }

    private static Object invokeRlNs(Object rl) throws Throwable {
        MethodHandle mh = resourceLocationNamespace;
        if (mh == null) {
            synchronized (NivelMundoReflect.class) {
                if (resourceLocationNamespace == null) {
                    Class<?> rlCl = Class.forName("net.minecraft.resources.ResourceLocation");
                    Method m = rlCl.getMethod("getNamespace");
                    resourceLocationNamespace = LOOKUP.unreflect(m);
                }
                mh = resourceLocationNamespace;
            }
        }
        return mh.invoke(rl);
    }

    private static Object invokeRlPath(Object rl) throws Throwable {
        MethodHandle mh = resourceLocationPath;
        if (mh == null) {
            synchronized (NivelMundoReflect.class) {
                if (resourceLocationPath == null) {
                    Class<?> rlCl = Class.forName("net.minecraft.resources.ResourceLocation");
                    Method m = rlCl.getMethod("getPath");
                    resourceLocationPath = LOOKUP.unreflect(m);
                }
                mh = resourceLocationPath;
            }
        }
        return mh.invoke(rl);
    }

    private static Object crearTagBloque(EtiquetaBloque etiqueta) throws Throwable {
        if (tagKeyCreate == null || registryKeyBlock == null || resourceLocationFromNsPath == null) {
            synchronized (NivelMundoReflect.class) {
                if (tagKeyCreate == null) {
                    Class<?> tagKeyCl = Class.forName("net.minecraft.tags.TagKey");
                    Class<?> resKeyCl = Class.forName("net.minecraft.resources.ResourceKey");
                    Class<?> rlCl = Class.forName("net.minecraft.resources.ResourceLocation");
                    Class<?> registriesCl = Class.forName("net.minecraft.core.registries.Registries");
                    registryKeyBlock = registriesCl.getField("BLOCK").get(null);
                    Method rlFactory = rlCl.getMethod("fromNamespaceAndPath", String.class, String.class);
                    resourceLocationFromNsPath = LOOKUP.unreflect(rlFactory);
                    Method m = tagKeyCl.getMethod("create", resKeyCl, rlCl);
                    tagKeyCreate = LOOKUP.unreflect(m);
                }
            }
        }
        Identifier id = etiqueta.id();
        Object rl = resourceLocationFromNsPath.invoke(id.namespace(), id.path());
        return tagKeyCreate.invoke(registryKeyBlock, rl);
    }

    private static MethodHandle obtenerBlockStateIs(Object state) throws ReflectiveOperationException {
        MethodHandle mh = blockStateIsTagKey;
        if (mh != null) {
            return mh;
        }
        synchronized (NivelMundoReflect.class) {
            if (blockStateIsTagKey != null) {
                return blockStateIsTagKey;
            }
            Class<?> tagKeyCl = Class.forName("net.minecraft.tags.TagKey");
            Method m = state.getClass().getMethod("is", tagKeyCl);
            blockStateIsTagKey = LOOKUP.unreflect(m);
            return blockStateIsTagKey;
        }
    }

    private static MethodHandle nivelHasChunk() {
        MethodHandle mh = levelHasChunk;
        if (mh != null) {
            return mh;
        }
        synchronized (NivelMundoReflect.class) {
            if (levelHasChunk != null) {
                return levelHasChunk;
            }
            try {
                Class<?> lvl = Class.forName("net.minecraft.world.level.Level");
                Method m = lvl.getMethod("hasChunk", int.class, int.class);
                levelHasChunk = LOOKUP.unreflect(m);
            } catch (ReflectiveOperationException e) {
                levelHasChunk = null;
            }
            return levelHasChunk;
        }
    }

    static void resetForTests() {
        synchronized (NivelMundoReflect.class) {
            ctorBlockPos = null;
            levelGetBlockState = null;
            blockStateGetBlock = null;
            singletonRegistryBlock = null;
            registryBlockGetKey = null;
            resourceKeyLocation = null;
            resourceLocationNamespace = null;
            resourceLocationPath = null;
            tagKeyCreate = null;
            registryKeyBlock = null;
            resourceLocationFromNsPath = null;
            blockStateIsTagKey = null;
            levelHasChunk = null;
        }
    }
}
