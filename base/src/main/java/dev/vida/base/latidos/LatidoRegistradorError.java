/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Типизированные ошибки, возникающие при автоматической регистрации
 * подписчиков через {@link LatidoRegistrador}.
 *
 * <p>Каждый подкласс описывает конкретную причину сбоя — неправильная
 * сигнатура метода, отсутствие поля {@code Latido}, недоступный executor
 * и т.д. Все ошибки содержат {@link #metodo() ссылку на проблемный метод}
 * для удобства диагностики.
 */
@ApiStatus.Preview("base")
public sealed class LatidoRegistradorError extends RuntimeException {

    private final Method metodo;

    LatidoRegistradorError(Method metodo, String message) {
        super(message);
        this.metodo = Objects.requireNonNull(metodo, "metodo");
    }

    /** Метод, вызвавший ошибку. */
    public Method metodo() { return metodo; }

    private static String ubicacion(Method m) {
        return m.getDeclaringClass().getName() + "#" + m.getName();
    }

    private static String prefijo(String anotacion, Method m) {
        return anotacion + " " + ubicacion(m);
    }

    /**
     * Метод, помеченный {@code @EjecutorLatido}, имеет не ровно один параметр.
     */
    public static final class FirmaInvalida extends LatidoRegistradorError {
        private final int parametros;

        public FirmaInvalida(Method m, int parametros) {
            this(m, "@EjecutorLatido", parametros);
        }

        public FirmaInvalida(Method m, String anotacion, int parametros) {
            super(m, prefijo(anotacion, m)
                    + " должен иметь ровно 1 параметр, но имеет " + parametros);
            this.parametros = parametros;
        }

        public int parametros() { return parametros; }
    }

    /**
     * Не найден {@code static final Latido<E>} на классе параметра и
     * в классе-владельце, совместимый с типом параметра.
     */
    public static final class LatidoNoEncontrado extends LatidoRegistradorError {
        private final Class<?> tipoEvento;

        public LatidoNoEncontrado(Method m, Class<?> tipoEvento) {
            super(m, "@EjecutorLatido " + ubicacion(m)
                    + ": не найден Latido<" + tipoEvento.getSimpleName()
                    + "> ни на " + tipoEvento.getName()
                    + ", ни в " + m.getDeclaringClass().getName());
            this.tipoEvento = tipoEvento;
        }

        public Class<?> tipoEvento() { return tipoEvento; }
    }

    /**
     * Найдено несколько совместимых полей {@code Latido} — неоднозначность.
     */
    public static final class LatidoAmbiguo extends LatidoRegistradorError {
        private final Class<?> tipoEvento;
        private final int cantidad;

        public LatidoAmbiguo(Method m, Class<?> tipoEvento, int cantidad) {
            super(m, "@EjecutorLatido " + ubicacion(m)
                    + ": найдено " + cantidad + " совместимых Latido-полей для "
                    + tipoEvento.getSimpleName() + " — невозможно выбрать одно");
            this.tipoEvento = tipoEvento;
            this.cantidad = cantidad;
        }

        public Class<?> tipoEvento() { return tipoEvento; }
        public int cantidad() { return cantidad; }
    }

    /**
     * Указан {@link EjecutorLatido.Kind#SUSURRO} или
     * {@link EjecutorLatido.Kind#HILO_PRINCIPAL}, но соответствующий
     * исполнитель не передан в биндер.
     */
    public static final class EjecutorFaltante extends LatidoRegistradorError {
        private final EjecutorLatido.Kind kind;

        public EjecutorFaltante(Method m, EjecutorLatido.Kind kind) {
            this(m, "@EjecutorLatido", kind);
        }

        public EjecutorFaltante(Method m, String anotacion, EjecutorLatido.Kind kind) {
            super(m, prefijo(anotacion, m) + " требует "
                    + kind + ", но соответствующий объект не передан в registrarEnObjeto(...)");
            this.kind = kind;
        }

        public EjecutorLatido.Kind kind() { return kind; }
    }

    /**
     * Тип параметра не совместим с {@code Latido.claseEvento()}.
     */
    public static final class TipoIncompatible extends LatidoRegistradorError {
        private final Class<?> esperado;
        private final Class<?> actual;

        public TipoIncompatible(Method m, Class<?> esperado, Class<?> actual) {
            this(m, "@EjecutorLatido", esperado, actual);
        }

        public TipoIncompatible(Method m, String anotacion, Class<?> esperado, Class<?> actual) {
            super(m, prefijo(anotacion, m) + ": тип параметра " + actual.getName()
                    + " не совместим с Latido<" + esperado.getName() + ">");
            this.esperado = esperado;
            this.actual = actual;
        }

        public Class<?> esperado() { return esperado; }
        public Class<?> actual() { return actual; }
    }

    /**
     * На одном методе нельзя одновременно использовать две shortcut-аннотации.
     */
    public static final class AnotacionesConflictivas extends LatidoRegistradorError {
        public AnotacionesConflictivas(Method m) {
            super(m, ubicacion(m)
                    + " не может одновременно иметь @EjecutorLatido и @OyenteDeTick");
        }
    }

    /**
     * Неверный requested TPS для {@link OyenteDeTick}.
     */
    public static final class TpsInvalido extends LatidoRegistradorError {
        private final int tps;

        public TpsInvalido(Method m, int tps) {
            super(m, "@OyenteDeTick " + ubicacion(m) + ": tps вне [1..20]: " + tps);
            this.tps = tps;
        }

        public int tps() { return tps; }
    }
}
