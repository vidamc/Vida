/*
 * :resolver — бэктрекинговый резолвер зависимостей модов.
 *
 * Чистый домен: не зависит от :discovery, работает поверх абстрактных
 * `Provider` / `Universe`. Адаптер к `ModManifest` живёт здесь же и
 * подключает `:manifest` как api-зависимость для удобства пользователей.
 */

plugins {
    id("vida.library-conventions")
    id("vida.maven-publish")
}

description = "Dependency resolver: backtracking search over SemVer ranges with provides/incompatibilities."

dependencies {
    api(project(":core"))
    api(project(":manifest"))
    testRuntimeOnly(libs.logback.classic)
}
