/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.loader.profile.PlatformProfileDescriptor;
import java.util.Optional;

/**
 * Instantiates and installs a {@link PlatformBridge} from an optional platform profile descriptor.
 *
 * <p>If {@linkplain PlatformProfileDescriptor#platformBridgeClass()} is absent, uses
 * {@link VanillaBridge}. Loads the FQN via {@link ClassLoader#getSystemClassLoader()} first (game
 * classpath), then the defining class loader of {@link VanillaBridge}.
 */
@ApiStatus.Internal
public final class PlatformBridgeSupport {

    private static final Log LOG = Log.of(PlatformBridgeSupport.class);

    private PlatformBridgeSupport() {}

    /**
     * Installs a bridge unless one is already present (tests may pre-install a mock).
     */
    public static void installFromProfile(Optional<PlatformProfileDescriptor> profile) {
        if (VanillaBridge.current() != null) {
            return;
        }
        String fqcn = profile.flatMap(PlatformProfileDescriptor::platformBridgeClass)
                .orElse(VanillaBridge.class.getName());
        PlatformBridge bridge = instantiate(fqcn);
        VanillaBridge.install(bridge);
        LOG.info("Vida: PlatformBridge installed ({})", fqcn);
    }

    private static PlatformBridge instantiate(String fqcn) {
        try {
            Class<?> c = tryLoadInitialized(fqcn);
            if (c == null) {
                LOG.warn("Vida: PlatformBridge class '{}' not found; using VanillaBridge", fqcn);
                return new VanillaBridge();
            }
            Object o = c.getDeclaredConstructor().newInstance();
            if (o instanceof PlatformBridge pb) {
                return pb;
            }
            LOG.warn("Vida: '{}' is not a PlatformBridge; using VanillaBridge", fqcn);
            return new VanillaBridge();
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOG.warn("Vida: failed to construct PlatformBridge '{}': {} — using VanillaBridge",
                    fqcn, ex.toString());
            return new VanillaBridge();
        }
    }

    /** Resolve class with initialization so instance construction sees static state. */
    private static Class<?> tryLoadInitialized(String fqcn) {
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        try {
            return Class.forName(fqcn, true, sys);
        } catch (ClassNotFoundException ignored) {
            // fall through
        }
        ClassLoader cl = VanillaBridge.class.getClassLoader();
        if (cl != null && cl != sys) {
            try {
                return Class.forName(fqcn, true, cl);
            } catch (ClassNotFoundException ignored) {
                // fall through
            }
        }
        return null;
    }
}
