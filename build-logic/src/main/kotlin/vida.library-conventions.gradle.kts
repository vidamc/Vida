/*
 * Конвенции для библиотечных модулей (всё, что публикуется как .jar):
 *   - javadoc-jar
 *   - репродьюсируемые билды (timestamps сброшены)
 *   - корректные маркеры API-статуса.
 */

plugins {
    id("vida.java-conventions")
    id("vida.test-conventions")
}

java {
    withJavadocJar()
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
