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
