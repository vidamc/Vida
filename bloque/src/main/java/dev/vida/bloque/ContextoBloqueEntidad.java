/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;

/**
 * Маркер «контекст BlockEntity» — любой POJO, хранящий per-position state
 * блока. Методы {@link #serializar()} и {@link #deserializar(byte[])}
 * соединяют его с save-форматом vanilla (реализация моста — за
 * {@code vida-mundo}).
 *
 * <p>Vida намеренно не навязывает конкретный формат (NBT, JSON, binary);
 * мод сам решает, как сериализоваться. Простейший случай — POD record,
 * который пишется в {@code DataOutputStream} и читается обратно.
 */
@ApiStatus.Preview("bloque")
public interface ContextoBloqueEntidad {

    /**
     * Сериализует состояние в байты. Должен быть детерминистичным и
     * корректным для идентичного восстановления {@link #deserializar(byte[])}.
     *
     * @return бинарное представление состояния
     */
    byte[] serializar();

    /**
     * Восстанавливает состояние из байтов. Метод вызывается сразу после
     * создания инстанса фабрикой {@link BloqueEntidad#crearContexto()}.
     *
     * @param datos байты, возвращённые {@link #serializar()} в предыдущем запуске
     */
    void deserializar(byte[] datos);
}
