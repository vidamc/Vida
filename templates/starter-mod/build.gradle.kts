plugins {
    java
    id("dev.vida.mod")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenLocal()
    mavenCentral()
}

val vidaPlatformVersion = providers.gradleProperty("vida.platform.version").get()

dependencies {
    compileOnly(platform("dev.vida:vida-bom:$vidaPlatformVersion"))
    compileOnly("dev.vida:base")
}

vida {
    injectDefaultVidaDependency.set(true)
    defaultVidaDependencyRange.set("^${vidaPlatformVersion}")
    mod {
        id.set("plantilla_demo")
        displayName.set("Plantilla demo")
        description.set("Starter template for Vida mods")
        authors.add("Tu nombre")
        license.set("Apache-2.0")
        entrypoint.set("dev.vida.template.starter.PlantillaMod")
    }
}
