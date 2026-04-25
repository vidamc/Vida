/*
 * Корневой build-скрипт.
 *
 * Ничего не публикует и ничего не собирает сам — все сборочные действия
 * делегированы подмодулям через convention-плагины `vida.*`.
 */

plugins {
    base
}

// ---------------------------------------------------------------------------
//  Версионирование через корневой version.txt
// ---------------------------------------------------------------------------
//
//  Release-please с `release-type: "simple"` обновляет именно version.txt
//  при каждом релиз-PR, поэтому здесь мы читаем значение оттуда и
//  распространяем на rootProject + все subprojects. gradle.properties
//  оставлен как fallback (на случай локальных sparse-чекаутов).
val versionFile = rootProject.layout.projectDirectory.file("version.txt").asFile
if (versionFile.exists()) {
    val fromFile = versionFile.readText().trim()
    if (fromFile.isNotEmpty()) {
        version = fromFile
        subprojects {
            version = fromFile
        }
    }
}

tasks.named("build") {
    description = "Builds every Vida module (core, manifest, …)."
    group = "build"
}

tasks.register("verifyPlatformProfiles") {
    description = "Validates platform-profiles/**/profile.json for required keys."
    group = "verification"
    val generationsDir = layout.projectDirectory.dir("platform-profiles/generations")
    inputs.dir(generationsDir)
    doLast {
        val root = generationsDir.asFile
        if (!root.isDirectory) {
            logger.lifecycle("verifyPlatformProfiles: skip (no platform-profiles/generations)")
            return@doLast
        }
        val profiles =
                root.walkTopDown().filter { it.isFile && it.name == "profile.json" }.toList()
        val generationJson = Regex("\"generation\"\\s*:\\s*\"([^\"]+)\"")
        profiles.forEach { f ->
            val text = f.readText()
            val path = f.invariantSeparatorsPath
            require(text.contains("\"profileId\"")) { "$path: missing profileId" }
            require(text.contains("\"gameVersion\"")) { "$path: missing gameVersion" }
            require(text.contains("\"generation\"")) { "$path: missing generation" }
            require(text.contains("\"mappings\"")) { "$path: missing mappings" }
            require(text.contains("\"strategy\"")) { "$path: missing mappings.strategy" }
            val gm = generationJson.find(text)
                ?: throw GradleException("$path: could not parse generation")
            val genTok = gm.groupValues[1]
            val rel = root.toPath().relativize(f.toPath()).normalize()
            require(rel.nameCount >= 3) {
                "$path: expected generations/<generation>/<drop>/profile.json"
            }
            val folderGen = rel.getName(0).toString()
            when (genTok) {
                "LEGACY_121" -> require(folderGen == "legacy-121") {
                    "$path: folder $folderGen does not match generation LEGACY_121"
                }
                "CALENDAR_26" -> require(folderGen == "calendar-26") {
                    "$path: folder $folderGen does not match generation CALENDAR_26"
                }
                else -> throw GradleException("$path: unknown generation token $genTok")
            }
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        profiles.sortedBy { it.invariantSeparatorsPath }.forEach { f -> digest.update(f.readBytes()) }
        val fp = java.util.HexFormat.of().formatHex(digest.digest())
        logger.lifecycle("verifyPlatformProfiles: OK (${profiles.size} profile(s)); tree fingerprint SHA-256 = $fp")
    }
}

// Захватываем project.version на этапе конфигурации — чтобы задача была
// configuration-cache-friendly (project.version читать при execution под
// config cache нельзя).
val vidaVersionAtConfig = project.version.toString()

tasks.register("version") {
    description = "Prints the Vida version in the form 'Vida version: X.Y.Z'."
    group = "help"
    val v = vidaVersionAtConfig
    doLast { println("Vida version: $v") }
}

// Машино-читаемый вывод для CI (без префикса) — вызывается из release.yml
// и ci.yml, чтобы навесить версию на артефакты и GitHub Release.
tasks.register("vidaDocTest") {
    description = "Compiles fenced Java snippets from docs/ (package dev.vida.* compilation units)."
    group = "verification"
    dependsOn(":vida-doc-test:compileTestJava")
}

tasks.register("printVersion") {
    description = "Prints the raw Vida version, no prefix. Used by CI."
    group = "help"
    val v = vidaVersionAtConfig
    doLast { println(v) }
}

// ---------------------------------------------------------------------------
//  Aggregated Javadoc
// ---------------------------------------------------------------------------
//
//  Каждый Java-модуль уже выставляет свой javadoc-task (из `java-library`).
//  Мы аггрегируем их источники в один javadoc-run, чтобы получить единый
//  cross-linked API-reference в build/javadoc-all/.
//
//  Артефакт используется как чистый static-bundle; куда именно его
//  деплоить (VPS, зеркало, архив) — решает внешний пайплайн, Gradle об
//  этом не знает.
tasks.register<Javadoc>("javadocAll") {
    description = "Aggregated Javadoc for every Vida JVM module."
    group = "documentation"

    val javaSubprojects = subprojects.filter { sp ->
        sp.plugins.hasPlugin("java-library") || sp.plugins.hasPlugin("java")
    }

    javaSubprojects.forEach { sp ->
        dependsOn(sp.tasks.named("classes"))
    }

    source(javaSubprojects.map { sp ->
        sp.extensions.getByType<SourceSetContainer>()["main"].allJava
    })
    classpath = files(javaSubprojects.map { sp ->
        sp.extensions.getByType<SourceSetContainer>()["main"].compileClasspath
    })

    setDestinationDir(layout.buildDirectory.dir("javadoc-all").get().asFile)
    val v = vidaVersionAtConfig
    title = "Vida $v API"
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        links("https://docs.oracle.com/en/java/javase/21/docs/api/")
        windowTitle = "Vida $v API"
    }
}
