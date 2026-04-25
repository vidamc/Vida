/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vigia;

import dev.vida.core.ApiStatus;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * In-memory резюме профилирования — агрегированные метрики без
 * полного JFR-дампа.
 *
 * <p>Используется для быстрого отображения результатов в чате или
 * лёгких отчётов без записи файла.
 *
 * @param duracion       длительность профилируемого интервала
 * @param muestras       общее количество собранных samples
 * @param topMetodos     top-N методов по суммарному sample-count
 * @param latidoMetricas метрики по каналам событий (Latido)
 * @param susurroActivos текущее число активных задач в Susurro
 * @param susurroPendientes текущее число ожидающих задач
 * @param susurroCompletadas кумулятивное число завершённых задач
 */
@ApiStatus.Stable
public record Resumen(
        Duration duracion,
        long muestras,
        List<MetodoMuestra> topMetodos,
        List<LatidoMetrica> latidoMetricas,
        int susurroActivos,
        int susurroPendientes,
        long susurroCompletadas) {

    public Resumen {
        Objects.requireNonNull(duracion, "duracion");
        topMetodos = List.copyOf(topMetodos);
        latidoMetricas = List.copyOf(latidoMetricas);
    }

    /**
     * Запись: один метод в top-N.
     *
     * @param metodo  полный квалифицированный путь (пакет.Класс#метод)
     * @param muestras количество samples, попавших в этот метод
     * @param porcentaje доля от общего числа samples (0.0–100.0)
     */
    @ApiStatus.Stable
    public record MetodoMuestra(String metodo, long muestras, double porcentaje) {
        public MetodoMuestra {
            Objects.requireNonNull(metodo, "metodo");
        }
    }

    /**
     * Метрика по одному каналу {@link dev.vida.base.latidos.Latido}.
     *
     * @param nombre        идентификатор Latido (namespace:path)
     * @param suscriptores  количество подписчиков
     * @param emisiones     количество emit-вызовов за период (если трекается)
     */
    @ApiStatus.Stable
    public record LatidoMetrica(String nombre, int suscriptores, long emisiones) {
        public LatidoMetrica {
            Objects.requireNonNull(nombre, "nombre");
        }
    }
}
