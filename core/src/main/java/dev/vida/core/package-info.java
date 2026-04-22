/**
 * Базовые примитивы Vida.
 *
 * <p>Этот пакет содержит фундаментальные типы, от которых зависят все остальные
 * модули загрузчика и API:
 *
 * <ul>
 *   <li>{@link dev.vida.core.Identifier} — namespaced-идентификатор {@code ns:path};</li>
 *   <li>{@link dev.vida.core.Version} — версия по SemVer 2.0.0;</li>
 *   <li>{@link dev.vida.core.VersionRange} — NPM-совместимый диапазон версий;</li>
 *   <li>{@link dev.vida.core.Result} — Either-подобный контейнер успех/ошибка;</li>
 *   <li>{@link dev.vida.core.Log} — единый фасад логирования поверх SLF4J;</li>
 *   <li>{@link dev.vida.core.ApiStatus} — маркеры стабильности публичного API.</li>
 * </ul>
 *
 * <p>Модуль намеренно не тянет тяжёлых зависимостей и пригоден для использования
 * на самых ранних фазах запуска (до инициализации Cartografía и Vifada).
 */
package dev.vida.core;
