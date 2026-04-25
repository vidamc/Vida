/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Вставляет тело метода морфа в указанную точку целевого метода.
 *
 * <h2>Сигнатура инъекции</h2>
 * Инъекционный метод морфа должен:
 * <ul>
 *   <li>иметь возвращаемый тип {@code void};</li>
 *   <li>принимать последним параметром {@link CallbackInfo};</li>
 *   <li>иметь ровно те же параметры, что и целевой метод, плюс
 *       {@code CallbackInfo} в конце — либо не иметь параметров, если
 *       целевой метод их тоже не имеет.</li>
 * </ul>
 *
 * <h2>Пример</h2>
 * <pre>{@code
 * @VifadaInject(method = "tick()V", at = @VifadaAt(InjectionPoint.HEAD))
 * public void onTick(CallbackInfo ci) { ... }
 * }</pre>
 */
@ApiStatus.Stable
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface VifadaInject {

    /**
     * Имя + дескриптор целевого метода в формате {@code name(argTypes)ret},
     * например {@code "tick()V"} или {@code "doSomething(Ljava/lang/String;)I"}.
     */
    String method();

    /** Точка инъекции. */
    VifadaAt at();

    /**
     * Если {@code true}, отсутствие целевого метода молча игнорируется, и
     * инъекция просто пропускается. По умолчанию {@code false} — в этом
     * случае вернётся {@link VifadaError.TargetMethodNotFound}.
     */
    boolean requireTarget() default true;
}
