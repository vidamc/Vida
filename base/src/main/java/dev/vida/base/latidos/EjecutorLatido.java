/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер метода-подписчика: задаёт стратегию {@link Ejecutor}, в которой
 * нужно вызывать обработчик при авто-регистрации подписок через
 * {@link LatidoRegistrador}.
 *
 * <p>Reflection-биндер {@link LatidoRegistrador#registrarEnObjeto(LatidoBus, Object)}
 * сканирует методы, помеченные этой аннотацией, и автоматически подписывает
 * их на соответствующий канал событий.
 *
 * <h2>Применение</h2>
 * <pre>{@code
 *   public class MiMod {
 *       @EjecutorLatido(kind = Kind.HILO_PRINCIPAL)
 *       public void alCargarMundo(CargaMundoLatido ev) { ... }
 *   }
 *
 *   // авто-регистрация:
 *   LatidoRegistrador.registrarEnObjeto(bus, new MiMod(), susurro, hp);
 * }</pre>
 *
 * <p>Для привязки к каналу {@link Latido} биндер ищет поле
 * {@code public static final Latido<E> TIPO} на классе параметра метода
 * (по конвенции Vida каждый тип события объявляет свой ключ).
 *
 * @see LatidoRegistrador
 * @see Ejecutor
 */
@ApiStatus.Preview("base")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EjecutorLatido {

    /** Тип исполнителя. */
    Kind kind() default Kind.SINCRONO;

    /** Этикетка для {@link Kind#SUSURRO}: идентификатор очереди back-pressure. */
    String etiqueta() default "vida/latido";

    /** Приоритет {@link Kind#SUSURRO}: отображается 1:1 на {@link dev.vida.susurro.Prioridad}. */
    Prioridad prioridad() default Prioridad.NORMAL;

    /** Приоритет подписки в шине {@link LatidoBus}. */
    PrioridadBus prioridadBus() default PrioridadBus.NORMAL;

    /** Фаза подписки в шине {@link LatidoBus}. */
    FaseBus fase() default FaseBus.PRINCIPAL;

    enum Kind {
        /** Синхронно, в потоке {@link LatidoBus#emitir}. */
        SINCRONO,
        /** В пуле {@link dev.vida.susurro.Susurro}. */
        SUSURRO,
        /** На следующем пульсе {@link dev.vida.susurro.HiloPrincipal}. */
        HILO_PRINCIPAL
    }

    /** Зеркало {@link dev.vida.susurro.Prioridad}, чтобы аннотация не тянула классы из susurro в проявленные типы. */
    enum Prioridad {
        ALTA, NORMAL, BAJA;

        public dev.vida.susurro.Prioridad aSusurro() {
            return switch (this) {
                case ALTA -> dev.vida.susurro.Prioridad.ALTA;
                case NORMAL -> dev.vida.susurro.Prioridad.NORMAL;
                case BAJA -> dev.vida.susurro.Prioridad.BAJA;
            };
        }
    }

    /**
     * Зеркало {@link dev.vida.base.latidos.Prioridad} для использования
     * в аннотации без конфликта имён с вложенным {@link Prioridad}.
     */
    enum PrioridadBus {
        URGENTE, ALTA, NORMAL, BAJA, MONITOR;

        public dev.vida.base.latidos.Prioridad aLatidos() {
            return dev.vida.base.latidos.Prioridad.valueOf(name());
        }
    }

    /** Зеркало {@link dev.vida.base.latidos.Fase}. */
    enum FaseBus {
        ANTES, PRINCIPAL, DESPUES;

        public dev.vida.base.latidos.Fase aLatidos() {
            return dev.vida.base.latidos.Fase.valueOf(name());
        }
    }
}
