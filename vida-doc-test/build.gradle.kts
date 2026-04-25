/*
 * Компилирует примеры из markdown (docs/) как часть проверки контракта док ↔ API.
 */

plugins {
    id("vida.java-conventions")
    id("vida.test-conventions")
    id("vida.doc-test")
}

description = "Compiles Java snippets embedded in docs/ (vidaDocTest)."

dependencies {
    val docTestApi = listOf(
        ":core",
        ":manifest",
        ":config",
        ":cartografia",
        ":discovery",
        ":resolver",
        ":vifada",
        ":loader",
        ":base",
        ":bloque",
        ":objeto",
        ":susurro",
        ":puertas",
        ":vigia",
        ":entidad",
        ":mundo",
        ":render",
        ":red",
        ":installer",
    )
    docTestApi.forEach { testImplementation(project(it)) }
}

tasks.named<Test>("test") {
    // Сниппеты проверяются компиляцией; пустой test-класс не считается ошибкой.
    filter { isFailOnNoMatchingTests = false }
}
