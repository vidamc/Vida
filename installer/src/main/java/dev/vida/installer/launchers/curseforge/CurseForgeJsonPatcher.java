/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.curseforge;

import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Патчер {@code minecraftinstance.json} для CurseForge App.
 *
 * <p>CurseForge хранит каждый инстанс в отдельной папке с файлом
 * {@code minecraftinstance.json}. Поле {@code javaArgsOverride} —
 * недокументированное, но стабильно работающее — позволяет инжектить
 * {@code -javaagent} без редактирования настроек лаунчера.
 */
final class CurseForgeJsonPatcher {

    private CurseForgeJsonPatcher() {}

    record Result(boolean alreadyAgent, String previousArgs) {}

    /**
     * Патчит {@code minecraftinstance.json}: добавляет/заменяет
     * {@code -javaagent:<loaderPath>} в {@code javaArgsOverride}.
     */
    @SuppressWarnings("unchecked")
    static Result patch(Path instanceJson, String loaderAbsPath) throws IOException {
        String raw = Files.readString(instanceJson, StandardCharsets.UTF_8);
        Object tree = JsonTree.parse(raw);
        if (!(tree instanceof Map<?, ?> mapRaw)) {
            throw new IOException("minecraftinstance.json root is not an object");
        }
        Map<String, Object> root = (Map<String, Object>) mapRaw;

        String agentArg = "-javaagent:" + loaderAbsPath;
        Object prev = root.get("javaArgsOverride");
        String prevStr = prev instanceof String s ? s : null;
        boolean alreadyAgent = false;

        if (prevStr != null && !prevStr.isBlank()) {
            if (prevStr.contains("-javaagent:")) {
                alreadyAgent = true;
                String patched = prevStr.replaceAll(
                        "-javaagent:\\S+",
                        agentArg.replace("\\", "\\\\"));
                root.put("javaArgsOverride", patched);
            } else {
                root.put("javaArgsOverride", prevStr.trim() + " " + agentArg);
            }
        } else {
            root.put("javaArgsOverride", agentArg);
        }

        String json = JsonTree.write(root);
        Files.writeString(instanceJson, json, StandardCharsets.UTF_8);

        return new Result(alreadyAgent, prevStr);
    }
}
