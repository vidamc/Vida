import java.io.File

/*
 * :installer — кросс-платформенный инсталлятор Vida.
 */
plugins {
    id("vida.java-conventions")
    id("vida.test-conventions")
    application
}

description = "Vida installer: Swing GUI + headless CLI."

dependencies {
    implementation(project(":core"))
    implementation(project(":manifest"))
    implementation(project(":puertas"))
    implementation(libs.slf4j.api)
    implementation(libs.sqlite.jdbc)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("dev.vida.installer.InstallerMain")
    applicationName = "vida-installer"
}

// ----------------------------------------------------------------------
//  «Вшивание» loader.jar в ресурсы installer'а.
// ----------------------------------------------------------------------
val embedRoot = layout.buildDirectory.dir("generated/installer-resources")

val embedLoader by tasks.registering(Copy::class) {
    description = "Embed :loader:agentJar (fat javaagent) into installer resources as /loader/loader.jar"
    group = "build"
    // ВАЖНО: берём именно agentJar — полностью само-достаточный fat-jar с
    // shadow'нутыми :core / :manifest / :discovery / :resolver / :vifada /
    // ASM / SLF4J. Обычный :loader:jar не стартует как -javaagent, т.к.
    // JVM не видит его транзитивные зависимости (см. loader/build.gradle.kts,
    // блок 'agentJar').
    dependsOn(":loader:agentJar")
    from(project(":loader").tasks.named("agentJar"))
    into(embedRoot.map { it.dir("loader") })
    rename { _ -> "loader.jar" }
}

sourceSets.named("main") {
    resources.srcDir(embedRoot)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(embedLoader)
}

// sourcesJar от `vida.java-conventions` видит embedRoot как resource srcDir;
// без явной зависимости Gradle ругается implicit-dependency. Ничего
// «интересного» для исходников там нет — просто исключаем *.jar.
tasks.named<Jar>("sourcesJar") {
    dependsOn(embedLoader)
    exclude("loader/*.jar")
}

// ----------------------------------------------------------------------
//  Fat jar (runnable `java -jar ...`).
//
//  Он — ПЕРВИЧНЫЙ артефакт установщика и получает «чистое» имя
//  `installer-<ver>.jar` (без `-all` суффикса). Обычный `jar`-таск, который
//  Gradle создаёт по умолчанию (тонкий jar без зависимостей и без
//  `Main-Class`), переносится под classifier `thin`, чтобы пользователь
//  не мог случайно дважды кликнуть на неработающий файл — на такой
//  "зависший" double-click уже жаловались в issue-треде.
// ----------------------------------------------------------------------
tasks.named<Jar>("jar") {
    archiveClassifier.set("thin")
}

val fatJar by tasks.registering(Jar::class) {
    description = "Build an all-in-one executable jar for the installer."
    group = "build"
    // Главный, runnable-артефакт → без classifier.
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class"              to "dev.vida.installer.InstallerMain",
            "Implementation-Title"    to "Vida Installer",
            "Implementation-Version"  to project.version,
            // Moжно держать очень скромный heap — Swing-installer'у 128 MiB
            // хватает с головой. Пользователи больше не видят «процесс
            // на 400 МБ» в Task Manager после double-click.
            "Vida-Heap-Hint"          to "128m",
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(embedLoader, tasks.named("compileJava"), tasks.named("processResources"))
    dependsOn(configurations.runtimeClasspath)
    inputs.files(configurations.runtimeClasspath).withPropertyName("runtimeClasspath")
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
                "module-info.class")
    }
}

tasks.named("assemble") {
    dependsOn(fatJar)
}

// ----------------------------------------------------------------------
//  jpackage-native-bundle.
//
//  На dev-машине без jpackage-утилиты task упадёт с понятной ошибкой —
//  это не заглушка, а ожидаемое поведение.
// ----------------------------------------------------------------------
val nativeDir = layout.buildDirectory.dir("native")

val vidaNativePackage by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Build a native installer (.exe/.msi/.dmg/.deb/.rpm) via jpackage."
    dependsOn(fatJar)

    // Тип пакета можно зафиксировать через -PvidaInstallerType=deb|rpm|msi|exe|dmg|pkg|app-image.
    // Иначе jpackage выбирает платформенное значение по умолчанию.
    val overrideType = (project.findProperty("vidaInstallerType") as String?)?.trim().orEmpty()
    // Кастомный суффикс имени пакета (например, `-linux-x64`) — удобно для матричной сборки в CI.
    val nameSuffix = (project.findProperty("vidaInstallerNameSuffix") as String?)?.trim().orEmpty()

    doFirst {
        val fatJarFile = fatJar.get().archiveFile.get().asFile
        val outDir = nativeDir.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()

        val javaHome = System.getProperty("java.home")
        val binDir = File(javaHome, "bin")
        val jpackageName = if (System.getProperty("os.name").lowercase().contains("win"))
            "jpackage.exe" else "jpackage"
        val jpackageBin = File(binDir, jpackageName)
        if (!jpackageBin.exists()) {
            throw GradleException("jpackage not found at " + jpackageBin.absolutePath
                    + ". Run on a full JDK 21 (not a JRE).")
        }

        val baseName = "VidaInstaller" + if (nameSuffix.isNotEmpty()) "-$nameSuffix" else ""

        executable = jpackageBin.absolutePath
        val argList = mutableListOf(
            "--input", fatJarFile.parentFile.absolutePath,
            "--main-jar", fatJarFile.name,
            "--main-class", "dev.vida.installer.InstallerMain",
            "--name", baseName,
            "--app-version", project.version.toString().substringBefore('-'),
            "--vendor", "The Vida Project Authors",
            "--copyright", "Copyright 2026 The Vida Project Authors.",
            "--description", "Vida Installer",
            "--dest", outDir.absolutePath,
        )
        if (overrideType.isNotEmpty()) {
            argList += listOf("--type", overrideType)
        }
        // Win-specific: добавить shortcut + меню.
        if (System.getProperty("os.name").lowercase().contains("win")) {
            argList += listOf("--win-shortcut", "--win-menu", "--win-menu-group", "Vida")
        }
        // macOS: дружелюбный identifier — нужен для подписи и Gatekeeper.
        if (System.getProperty("os.name").lowercase().contains("mac")) {
            argList += listOf("--mac-package-identifier", "tech.beforemine.vida.installer")
        }
        args = argList
    }
}
