rootProject.name = "plantilla-demo"

pluginManagement {
    plugins {
        id("dev.vida.mod") version providers.gradleProperty("vida.plugin.version").orElse("1.0.0").get()
    }
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}
