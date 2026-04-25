/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.curseforge;

import dev.vida.installer.mc.JsonTree;
import dev.vida.installer.mc.VidaInstallerJvm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.regex.Matcher;

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
    static Result patch(Path instanceJson, String loaderAbsPath,
                        String minecraftVersion, String loaderVersion) throws IOException {
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

        String vidaJvm = VidaInstallerJvm.spaceSeparatedInstallerJvmProps(minecraftVersion, loaderVersion);
        String base = VidaInstallerJvm.stripManagedInstallerJvmTokens(prevStr != null ? prevStr : "");

        if (!base.isBlank()) {
            if (base.contains("-javaagent:")) {
                alreadyAgent = true;
                String patched = base.replaceAll(
                        "-javaagent:\\S+",
                        Matcher.quoteReplacement(agentArg));
                patched = patched.trim() + " " + vidaJvm;
                root.put("javaArgsOverride", patched.trim());
            } else {
                root.put("javaArgsOverride", base.trim() + " " + agentArg + " " + vidaJvm);
            }
        } else {
            root.put("javaArgsOverride", agentArg + " " + vidaJvm);
        }

        String json = JsonTree.write(root);
        Path tmp = instanceJson.resolveSibling(instanceJson.getFileName() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, instanceJson, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            Files.move(tmp, instanceJson, StandardCopyOption.REPLACE_EXISTING);
        }

        return new Result(alreadyAgent, prevStr);
    }
}
