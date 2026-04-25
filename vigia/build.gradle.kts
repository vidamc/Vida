/*
 * :vigia — лёгкий sampling-профайлер поверх JFR.
 *
 * Модуль предоставляет:
 *   * VigiaSesion — start/stop/snapshot с записью в .jfr и in-memory Resumen.
 *   * HTML-отчёт с flame-chart, top-20 методов, breakdown по Latido/Susurro.
 *   * Команда /vida profile start|stop|dump (контракт — реализация в loader).
 *   * Интеграция с Susurro.Estadisticas и DefaultLatidoBus (метрики на канал).
 */
plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Vida profiler: lightweight JFR-based sampling profiler with HTML reports."

dependencies {
    api(project(":core"))
    api(project(":base"))
    api(project(":susurro"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
