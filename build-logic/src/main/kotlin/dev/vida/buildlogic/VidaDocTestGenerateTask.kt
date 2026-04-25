package dev.vida.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Извлекает fenced-блоки {@code ```java} из {@code docs/**/*.md} и копирует в
 * generated-sources только самодостаточные compilation units с {@code package dev.vida.*}
 * (проверка компиляцией в :vida-doc-test).
 */
@CacheableTask
abstract class VidaDocTestGenerateTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val docsDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val root = docsDir.get().asFile.toPath()
        val outRoot = outputDir.get().asFile.toPath()
        if (Files.exists(outRoot)) {
            outRoot.toFile().deleteRecursively()
        }
        Files.createDirectories(outRoot)

        val generated = mutableListOf<GeneratedJava>()

        Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".md") }
                .sorted()
                .forEach { path ->
                    val text = Files.readString(path, StandardCharsets.UTF_8)
                    extractJavaBlocks(text).forEachIndexed { _, raw ->
                        val trimmed = raw.trim()
                        if (!trimmed.startsWith("package ")) {
                            return@forEachIndexed
                        }
                        val pkg = Regex("""package\s+([\w.]+)\s*;""").find(trimmed)?.groupValues?.get(1)
                            ?: return@forEachIndexed
                        if (!pkg.startsWith("dev.vida.")) {
                            return@forEachIndexed
                        }
                        generated.add(parseFullUnit(raw))
                    }
                }
        }

        for (g in generated) {
            val dest = outRoot.resolve(g.relativePath)
            Files.createDirectories(dest.parent)
            Files.writeString(dest, g.source, StandardCharsets.UTF_8)
        }
    }

    private data class GeneratedJava(val relativePath: String, val source: String)

    private fun extractJavaBlocks(md: String): List<String> {
        val blocks = mutableListOf<String>()
        val lines = md.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.trim() == "```java") {
                val body = StringBuilder()
                i++
                while (i < lines.size && lines[i].trim() != "```") {
                    body.append(lines[i]).append('\n')
                    i++
                }
                blocks.add(body.toString())
            }
            i++
        }
        return blocks
    }

    private fun parseFullUnit(raw: String): GeneratedJava {
        val text = raw.trimEnd() + "\n"
        val pkg = Regex("""package\s+([\w.]+)\s*;""").find(text)?.groupValues?.get(1)
            ?: throw IllegalStateException("package missing in snippet")
        val simple = findTopLevelPublicClass(text)
            ?: throw IllegalStateException("no top-level type in snippet (package=$pkg)")
        val relPath = pkg.replace('.', '/') + "/" + simple + ".java"
        return GeneratedJava(relPath, text)
    }

    private fun findTopLevelPublicClass(java: String): String? {
        val patterns = listOf(
            Regex("""public\s+(?:final\s+)?class\s+(\w+)"""),
            Regex("""public\s+record\s+(\w+)"""),
            Regex("""public\s+enum\s+(\w+)"""),
            Regex("""public\s+interface\s+(\w+)"""),
            Regex("""class\s+(\w+)"""),
            Regex("""record\s+(\w+)"""),
            Regex("""enum\s+(\w+)"""),
            Regex("""interface\s+(\w+)"""),
        )
        for (p in patterns) {
            val m = p.find(java) ?: continue
            return m.groupValues[1]
        }
        return null
    }
}
