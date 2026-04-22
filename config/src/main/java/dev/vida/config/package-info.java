/**
 * Ajustes — типизированный конфиг-движок Vida.
 *
 * <p>Модуль предоставляет:
 * <ul>
 *   <li>{@link dev.vida.config.ConfigNode} — нейтральное дерево узлов
 *       (table/array/scalar), независимое от формата.</li>
 *   <li>{@link dev.vida.config.ConfigMerger} — чистое глубокое слияние
 *       таблиц по принципу right-wins, с заменой массивов целиком.</li>
 *   <li>{@link dev.vida.config.Ajustes} — иммутабельный снимок с
 *       dotted-path доступом и типизированными геттерами
 *       (strict / optional / with-default / Result-style).</li>
 *   <li>{@link dev.vida.config.AjustesLoader} — fluent-загрузчик с
 *       поддержкой встроенных профилей ({@code [profile.&lt;name&gt;]}) и
 *       внешних overlay.</li>
 *   <li>{@link dev.vida.config.AjustesError} — типизированные ошибки
 *       разбора и доступа.</li>
 * </ul>
 *
 * <p>TOML-парсер под капотом — {@code tomlj 1.1.x}; эта зависимость изолирована
 * в пакете {@code dev.vida.config.internal} и может быть заменена в будущем.
 */
package dev.vida.config;
