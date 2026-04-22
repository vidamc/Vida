/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;

/**
 * Контекст, передаваемый инъекционному методу. Позволяет прервать
 * выполнение целевого метода вызовом {@link #cancel()}.
 *
 * <p>Экземпляр создаётся сгенерированным trampoline'ом внутри целевого
 * метода и не переиспользуется между инъекциями.
 */
@ApiStatus.Preview("vifada")
public class CallbackInfo {

    private final String methodName;
    private boolean cancelled;

    public CallbackInfo(String methodName) {
        this.methodName = methodName;
    }

    /** Имя целевого метода — для диагностики и логов. */
    public final String methodName() { return methodName; }

    /** Прерывает выполнение целевого метода после возврата из инъекции. */
    public final void cancel() { this.cancelled = true; }

    /** Было ли запрошено прерывание. */
    public final boolean isCancelled() { return cancelled; }
}
