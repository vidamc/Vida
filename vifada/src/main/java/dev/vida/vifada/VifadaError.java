/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/** Структурированные ошибки Vifada. */
@ApiStatus.Stable
public sealed interface VifadaError {

    /** Байткод морфа невалиден или не соответствует ожиданиям парсера. */
    record BadMorph(String morphName, String reason) implements VifadaError {
        public BadMorph {
            Objects.requireNonNull(morphName, "morphName");
            Objects.requireNonNull(reason, "reason");
        }
    }

    /** На морфе отсутствует аннотация {@link VifadaMorph}. */
    record NotAMorph(String morphName) implements VifadaError {
        public NotAMorph { Objects.requireNonNull(morphName, "morphName"); }
    }

    /**
     * Целевой метод не найден в целевом классе.
     *
     * @param morphName     имя морфа
     * @param targetClass   FQN целевого класса
     * @param methodSpec    спецификация вида {@code name(args)ret}
     */
    record TargetMethodNotFound(String morphName, String targetClass, String methodSpec)
            implements VifadaError {
        public TargetMethodNotFound {
            Objects.requireNonNull(morphName);
            Objects.requireNonNull(targetClass);
            Objects.requireNonNull(methodSpec);
        }
    }

    /** Shadow-объявление не имеет соответствующего члена в целевом классе. */
    record ShadowMissing(String morphName, String targetClass, String memberSpec)
            implements VifadaError {
        public ShadowMissing {
            Objects.requireNonNull(morphName);
            Objects.requireNonNull(targetClass);
            Objects.requireNonNull(memberSpec);
        }
    }

    /**
     * Инъекционный метод не соответствует сигнатуре целевого (ожидается
     * совпадение аргументов + {@code CallbackInfo} в конце).
     */
    record SignatureMismatch(String morphName, String injectMethod,
                             String expected, String actual) implements VifadaError {
        public SignatureMismatch {
            Objects.requireNonNull(morphName);
            Objects.requireNonNull(injectMethod);
            Objects.requireNonNull(expected);
            Objects.requireNonNull(actual);
        }
    }

    /** Точка инъекции в MVP не поддерживается. */
    record UnsupportedAt(String morphName, String injectMethod, InjectionPoint at)
            implements VifadaError {
        public UnsupportedAt {
            Objects.requireNonNull(morphName);
            Objects.requireNonNull(injectMethod);
            Objects.requireNonNull(at);
        }
    }

    /** Морф объявляет target, не совпадающий с фактическим целевым классом. */
    record WrongTarget(String morphName, String declared, String actual)
            implements VifadaError {
        public WrongTarget {
            Objects.requireNonNull(morphName);
            Objects.requireNonNull(declared);
            Objects.requireNonNull(actual);
        }
    }

    /**
     * Два разных морфа с одинаковым приоритетом претендуют на одну точку инъекции в одном методе.
     *
     * @param hint например повысить различие через {@link dev.vida.vifada.VifadaMorph#priority()}
     */
    record MorphConflict(String targetClass, String slotDescription, String morphA,
                         String morphB, int priority, String hint) implements VifadaError {
        public MorphConflict {
            Objects.requireNonNull(targetClass);
            Objects.requireNonNull(slotDescription);
            Objects.requireNonNull(morphA);
            Objects.requireNonNull(morphB);
            Objects.requireNonNull(hint);
        }
    }

    /** Низкоуровневая ошибка чтения/записи байткода. */
    record AsmFailure(String morphName, String message) implements VifadaError {
        public AsmFailure {
            Objects.requireNonNull(morphName);
            Objects.requireNonNull(message);
        }
    }
}
