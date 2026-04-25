/*
 * :susurro — managed thread-pool Vida.
 *
 * Главный facade — Susurro; Tarea<T> — обёртка над CompletableFuture;
 * HiloPrincipal — маршалинг результата на main-thread (через «пульс» tick).
 *
 * Держим минимум зависимостей: только core (Log, ApiStatus).
 */
plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Vida managed thread-pool: Susurro, Tarea, HiloPrincipal."

dependencies {
    api(project(":core"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
