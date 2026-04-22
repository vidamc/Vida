/**
 * Схема {@code vida.mod.json} и её парсер.
 *
 * <p>Содержит:
 * <ul>
 *   <li>{@link dev.vida.manifest.ModManifest} — результат разбора манифеста;</li>
 *   <li>{@link dev.vida.manifest.ManifestParser} — парсер на основе
 *       {@link dev.vida.manifest.json.VidaJson};</li>
 *   <li>{@link dev.vida.manifest.ManifestError} — типизированные ошибки разбора.</li>
 * </ul>
 *
 * <p>Модуль идёт без сторонних JSON-зависимостей и пригоден к использованию на
 * ранней фазе {@code VidaPremain}, когда ещё нет {@code Catalogo}/{@code Tejido}.
 */
package dev.vida.manifest;
