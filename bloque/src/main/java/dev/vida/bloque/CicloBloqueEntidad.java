/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;

/**
 * Опциональный контракт для {@link ContextoBloqueEntidad}: тик, сохранение и
 * синхронизация с клиентом. Платформенный мост к Minecraft должен вызывать эти
 * методы из соответствующих хуков BlockEntity (server tick, save, sync packet),
 * чтобы моддеру не собирать цепочку вручную из разрозненных API.
 *
 * <p>Контекст блок-сущности может реализовать этот интерфейс вместе с
 * {@link ContextoBloqueEntidad}; методы по умолчанию пустые — переопределяйте
 * только нужное.
 */
@ApiStatus.Preview("cicloBloqueEntidad")
public interface CicloBloqueEntidad {

    /** Серверный игровой тик для этой позиции. */
    default void enTickServidor() {}

    /** Сохранение на диск (вызов до/вместе с сериализацией моста). */
    default void alGuardar() {}

    /** Отправка или применение состояния на клиенте (пакет Tejido / копия мира). */
    default void alSincronizarCliente() {}
}
