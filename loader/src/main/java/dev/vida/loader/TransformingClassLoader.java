/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * Базовый URL-classloader, который прогоняет каждый загружаемый класс
 * через {@link VidaClassTransformer} перед вызовом {@code defineClass}.
 *
 * <p>Это тот путь трансформации, который работает <b>без</b> Java-агента:
 * если мы не можем прицепиться к системному процессу через
 * {@code Instrumentation}, мы просто загружаем игру и моды своим
 * собственным classloader'ом, и здесь у нас полный контроль над тем,
 * какие байты попадут в JVM.
 *
 * <p>Поведение делегации — стандартное parent-first, за одним исключением:
 * классы из whitelist-префиксов (см. {@link #isSelfClass(String)}) — Vida
 * API и её рантайм — всегда резолвятся родительским загрузчиком, чтобы
 * гарантировать единый identity морф-аннотаций, {@code CallbackInfo} и т. п.
 */
@ApiStatus.Preview("loader")
public class TransformingClassLoader extends URLClassLoader {

    private final VidaClassTransformer transformer;
    private final String name;

    /** Для {@link ClassLoader#registerAsParallelCapable()} — выставляется при init. */
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public TransformingClassLoader(String name, URL[] urls, ClassLoader parent,
                                   VidaClassTransformer transformer) {
        super(Objects.requireNonNull(name, "name"), urls, parent);
        this.name = name;
        this.transformer = Objects.requireNonNull(transformer, "transformer");
    }

    public VidaClassTransformer transformer() { return transformer; }
    public String loaderName()                { return name; }

    // ---------------------------------------------------------------- load

    @Override
    protected Class<?> loadClass(String binaryName, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(binaryName)) {
            Class<?> c = findLoadedClass(binaryName);
            if (c == null) {
                if (isSelfClass(binaryName)) {
                    // Vida-собственные API — всегда через parent, никогда не трогаем байты.
                    c = super.loadClass(binaryName, false);
                } else {
                    try {
                        c = findClass(binaryName);
                    } catch (ClassNotFoundException ignore) {
                        c = super.loadClass(binaryName, false);
                    }
                }
            }
            if (resolve) resolveClass(c);
            return c;
        }
    }

    @Override
    protected Class<?> findClass(String binaryName) throws ClassNotFoundException {
        String internal = binaryName.replace('.', '/');
        String resourceName = internal + ".class";
        URL url = findResource(resourceName);
        if (url == null) throw new ClassNotFoundException(binaryName);

        byte[] bytes;
        try (InputStream is = url.openStream()) {
            bytes = is.readAllBytes();
        } catch (IOException e) {
            throw new ClassNotFoundException(binaryName, e);
        }
        byte[] transformed = transformer.transformClassfile(internal, bytes);
        return defineClass(binaryName, transformed, 0, transformed.length);
    }

    /**
     * Классы, которые всегда должны приходить из родительского загрузчика,
     * чтобы не было дублирования identity между loader'ом и модом.
     */
    protected boolean isSelfClass(String binaryName) {
        return binaryName.startsWith("dev.vida.core.")
                || binaryName.startsWith("dev.vida.vifada.")
                || binaryName.startsWith("dev.vida.loader.")
                || binaryName.startsWith("dev.vida.manifest.")
                || binaryName.startsWith("dev.vida.discovery.")
                || binaryName.startsWith("dev.vida.resolver.")
                || binaryName.startsWith("dev.vida.cartografia.")
                || binaryName.startsWith("dev.vida.config.")
                || binaryName.startsWith("java.")
                || binaryName.startsWith("javax.")
                || binaryName.startsWith("jdk.")
                || binaryName.startsWith("sun.")
                || binaryName.startsWith("org.slf4j.");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name + ", urls=" + getURLs().length + ")";
    }
}
