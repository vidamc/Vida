/*
 * :red — публичный сетевой API Vida.
 *
 * Пакеты клиент↔сервер, авто-сериализация record'ов, versioned codecs и
 * back-pressure очередь отправки.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida networking API: PaqueteCliente/PaqueteServidor, auto record codecs, back-pressure."

dependencies {
    api(project(":core"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
