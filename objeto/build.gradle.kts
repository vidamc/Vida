/*
 * :objeto — публичный API Vida для предметов.
 *
 * Объединяет декларативное описание предмета (PropiedadesObjeto), набор
 * data-компонентов 1.21.1 (ComponenteObjeto), классификацию инструментов
 * (Herramienta) и реестр (RegistroObjetos, надстройка над CatalogoManejador).
 *
 * Модуль side-agnostic — никакой прямой vanilla-зависимости. Мост к
 * net.minecraft.world.item.Item живёт в vida-mundo.
 */
plugins {
    id("vida.library-conventions")
}

description = "Vida public item API: Objeto, PropiedadesObjeto, ComponenteObjeto, Herramienta."

dependencies {
    api(project(":core"))
    api(project(":base"))
    api(project(":bloque"))

    implementation(libs.slf4j.api)

    testRuntimeOnly(libs.logback.classic)
}
