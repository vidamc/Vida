/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Константы ASM-дескрипторов для аннотаций и типов Vifada. */
@ApiStatus.Internal
public final class AsmNames {

    private AsmNames() {}

    /** Целевая версия ASM API — совместима с классами вплоть до Java 21. */
    public static final int API = Opcodes.ASM9;

    public static final String VIFADA_MORPH_DESC     = "Ldev/vida/vifada/VifadaMorph;";
    public static final String VIFADA_INJECT_DESC    = "Ldev/vida/vifada/VifadaInject;";
    public static final String VIFADA_OVERWRITE_DESC = "Ldev/vida/vifada/VifadaOverwrite;";
    public static final String VIFADA_SHADOW_DESC    = "Ldev/vida/vifada/VifadaShadow;";
    public static final String VIFADA_AT_DESC        = "Ldev/vida/vifada/VifadaAt;";

    public static final String VIFADA_MULTI_DESC = "Ldev/vida/vifada/VifadaMulti;";
    public static final String VIFADA_LOCAL_DESC = "Ldev/vida/vifada/VifadaLocal;";
    public static final String VIFADA_REDIRECT_DESC = "Ldev/vida/vifada/VifadaRedirect;";

    public static final String INJECTION_POINT_DESC  = "Ldev/vida/vifada/InjectionPoint;";
    public static final String CALLBACK_INFO_INTERNAL = "dev/vida/vifada/CallbackInfo";
    public static final String CALLBACK_INFO_DESC    = "L" + CALLBACK_INFO_INTERNAL + ";";

    public static final Type CALLBACK_INFO_TYPE = Type.getObjectType(CALLBACK_INFO_INTERNAL);
}
