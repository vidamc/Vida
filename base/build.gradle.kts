/*
 * :base — публичный API Vida для модов.
 *
 * Это тот единственный модуль, от которого зависят обычные моды.
 * Здесь — lifecycle-интерфейсы, система событий (Latidos), реестры
 * (Catálogo) и типизированные настройки (Ajustes).
 *
 * Цели дизайна:
 *   * Spanish-кодинг имён в духе Vida (Latido, Oyente, Catálogo…).
 *   * Никаких зависимостей на :loader — API не тянет за собой рантайм.
 *   * Имеет in-memory реализации (DefaultLatidoBus, DefaultCatalogo и т.п.)
 *     → пригоден для юнит-тестов модов без запуска всей Vida.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida public modding API: lifecycle, events (Latidos), registries (Catalogo), typed settings (Ajustes)."

dependencies {
    api(project(":core"))
    api(project(":manifest"))
    api(project(":config"))
    api(project(":susurro"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
