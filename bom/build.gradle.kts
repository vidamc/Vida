/*
 * :bom — Bill of Materials (Java Platform) для артефактов dev.vida.
 *
 * Потребители подключают одну платформу и получают согласованные версии
 * всех модулей из одного релиза Vida.
 */
plugins {
    `java-platform`
    `maven-publish`
    id("vida.bom-maven-publish")
}

description = "Vida BOM: aligned versions for dev.vida:* runtime libraries."

base {
    archivesName.set("vida-bom")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":core"))
        api(project(":manifest"))
        api(project(":fuente"))
        api(project(":config"))
        api(project(":cartografia"))
        api(project(":discovery"))
        api(project(":resolver"))
        api(project(":vifada"))
        api(project(":loader"))
        api(project(":base"))
        api(project(":cima"))
        api(project(":bloque"))
        api(project(":objeto"))
        api(project(":susurro"))
        api(project(":puertas"))
        api(project(":escultores"))
        api(project(":entidad"))
        api(project(":mundo"))
        api(project(":render"))
        api(project(":red"))
        api(project(":vigia"))
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["javaPlatform"])
        }
    }
}
