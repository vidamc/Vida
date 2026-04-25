/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.bloque.registro.EtiquetaBloque;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.mundo.Bioma;
import dev.vida.mundo.Coordenada;
import dev.vida.mundo.Dimension;
import dev.vida.mundo.Mundo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

/**
 * {@link Mundo} поверх vanilla {@code Level} (клиентский или серверный): блок, теги, биом, чанк,
 * время суток.
 */
@ApiStatus.Internal
public final class MundoNivelVanilla implements Mundo {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private static volatile MethodHandle levelDimension;
    private static volatile MethodHandle rkLocation;
    private static volatile MethodHandle rlNamespace;
    private static volatile MethodHandle rlPath;
    private static volatile MethodHandle levelGetBiome;
    private static volatile MethodHandle holderUnwrapKey;
    private static volatile MethodHandle rkLocationBiome;
    private static volatile MethodHandle levelGetDayTime;

    private final Object level;

    public MundoNivelVanilla(Object level) {
        this.level = level;
    }

    @Override
    public Identifier id() {
        try {
            Object rkDim = invokeDimension(level);
            Object rl = invokeRkLocationDim(rkDim);
            return identifierDesdeRl(rl);
        } catch (Throwable ignored) {
            return Identifier.of("minecraft", "overworld");
        }
    }

    @Override
    public Dimension dimension() {
        Identifier id = id();
        if (id.equals(Dimension.OVERWORLD.id())) {
            return Dimension.OVERWORLD;
        }
        if (id.equals(Dimension.NETHER.id())) {
            return Dimension.NETHER;
        }
        if (id.equals(Dimension.END.id())) {
            return Dimension.END;
        }
        return Dimension.de(id, true, true, false);
    }

    @Override
    public Bioma biomaEn(Coordenada coordenada) {
        if (coordenada == null) {
            return biomaPlains();
        }
        try {
            Object pos = NivelMundoReflect.blockPosXYZ(coordenada.x(), coordenada.y(), coordenada.z());
            Object holder = invokeGetBiome(level, pos);
            if (holder == null) {
                return biomaPlains();
            }
            Optional<?> opt = invokeHolderUnwrap(holder);
            if (opt == null || opt.isEmpty()) {
                return biomaPlains();
            }
            Object rkBio = opt.get();
            Object rl = invokeRkLocationBio(rkBio);
            Identifier idBio = identifierDesdeRl(rl);
            return new Bioma(idBio, 0.5f, 0.5f, Bioma.Precipitacion.NINGUNA);
        } catch (Throwable ignored) {
            return biomaPlains();
        }
    }

    @Override
    public boolean estaCargado(Coordenada coordenada) {
        if (coordenada == null) {
            return false;
        }
        if (!enRangoDeAltura(coordenada)) {
            return false;
        }
        return NivelMundoReflect.tieneChunkSiDisponible(
                level, coordenada.chunkX(), coordenada.chunkZ());
    }

    @Override
    public long tiempoDelDia() {
        try {
            Object v = invokeDayTime(level);
            if (v instanceof Number n) {
                return n.longValue();
            }
            return 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    @Override
    public Optional<Identifier> bloqueRegistradoEn(Coordenada coordenada) {
        if (coordenada == null) {
            return Optional.empty();
        }
        if (!estaCargado(coordenada)) {
            return Optional.empty();
        }
        return NivelMundoReflect.bloqueRegistradoEn(level, coordenada);
    }

    @Override
    public boolean bloqueTieneEtiqueta(Coordenada coordenada, EtiquetaBloque etiqueta) {
        if (coordenada == null || etiqueta == null) {
            return false;
        }
        if (!estaCargado(coordenada)) {
            return false;
        }
        return NivelMundoReflect.bloqueTieneEtiqueta(level, coordenada, etiqueta);
    }

    private static Bioma biomaPlains() {
        return new Bioma(
                Identifier.of("minecraft", "plains"),
                0.5f,
                0.5f,
                Bioma.Precipitacion.NINGUNA);
    }

    private static Identifier identifierDesdeRl(Object rl) throws Throwable {
        if (rl == null) {
            return Identifier.of("minecraft", "overworld");
        }
        String ns = (String) invokeRlNs(rl);
        String path = (String) invokeRlPath(rl);
        return Identifier.of(ns, path);
    }

    private static Object invokeRlNs(Object rl) throws Throwable {
        MethodHandle mh = rlNamespace;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (rlNamespace == null) {
                    Class<?> rlCl = Class.forName("net.minecraft.resources.ResourceLocation");
                    rlNamespace = LOOKUP.unreflect(rlCl.getMethod("getNamespace"));
                }
                mh = rlNamespace;
            }
        }
        return mh.invoke(rl);
    }

    private static Object invokeRlPath(Object rl) throws Throwable {
        MethodHandle mh = rlPath;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (rlPath == null) {
                    Class<?> rlCl = Class.forName("net.minecraft.resources.ResourceLocation");
                    rlPath = LOOKUP.unreflect(rlCl.getMethod("getPath"));
                }
                mh = rlPath;
            }
        }
        return mh.invoke(rl);
    }

    private static Object invokeDimension(Object level) throws Throwable {
        MethodHandle mh = levelDimension;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (levelDimension == null) {
                    levelDimension = LOOKUP.unreflect(level.getClass().getMethod("dimension"));
                }
                mh = levelDimension;
            }
        }
        return mh.invoke(level);
    }

    private static Object invokeRkLocationDim(Object rk) throws Throwable {
        MethodHandle mh = rkLocation;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (rkLocation == null) {
                    rkLocation = LOOKUP.unreflect(rk.getClass().getMethod("location"));
                }
                mh = rkLocation;
            }
        }
        return mh.invoke(rk);
    }

    private static Object invokeGetBiome(Object level, Object pos) throws Throwable {
        MethodHandle mh = levelGetBiome;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (levelGetBiome == null) {
                    Class<?> bp = Class.forName("net.minecraft.core.BlockPos");
                    levelGetBiome = LOOKUP.unreflect(level.getClass().getMethod("getBiome", bp));
                }
                mh = levelGetBiome;
            }
        }
        return mh.invoke(level, pos);
    }

    private static Optional<?> invokeHolderUnwrap(Object holder) throws Throwable {
        MethodHandle mh = holderUnwrapKey;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (holderUnwrapKey == null) {
                    holderUnwrapKey = LOOKUP.unreflect(holder.getClass().getMethod("unwrapKey"));
                }
                mh = holderUnwrapKey;
            }
        }
        Object o = mh.invoke(holder);
        return (Optional<?>) o;
    }

    private static Object invokeRkLocationBio(Object rk) throws Throwable {
        MethodHandle mh = rkLocationBiome;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (rkLocationBiome == null) {
                    rkLocationBiome = LOOKUP.unreflect(rk.getClass().getMethod("location"));
                }
                mh = rkLocationBiome;
            }
        }
        return mh.invoke(rk);
    }

    private static Object invokeDayTime(Object level) throws Throwable {
        MethodHandle mh = levelGetDayTime;
        if (mh == null) {
            synchronized (MundoNivelVanilla.class) {
                if (levelGetDayTime == null) {
                    levelGetDayTime = LOOKUP.unreflect(level.getClass().getMethod("getDayTime"));
                }
                mh = levelGetDayTime;
            }
        }
        return mh.invoke(level);
    }

    static void resetForTests() {
        synchronized (MundoNivelVanilla.class) {
            levelDimension = null;
            rkLocation = null;
            rlNamespace = null;
            rlPath = null;
            levelGetBiome = null;
            holderUnwrapKey = null;
            rkLocationBiome = null;
            levelGetDayTime = null;
        }
        NivelMundoReflect.resetForTests();
    }
}
