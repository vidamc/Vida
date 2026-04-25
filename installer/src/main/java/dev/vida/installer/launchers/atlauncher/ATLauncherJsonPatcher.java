/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.atlauncher;

import dev.vida.installer.mc.JsonTree;
import dev.vida.installer.mc.VidaInstallerJvm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Редактор {@code instance.json} ATLauncher'а — добавляет {@code -javaagent}
 * в {@code launcher.javaArguments}, сохраняя все остальные поля блоба.
 *
 * <p>ATLauncher использует Gson-сериализацию {@code Instance} + вложенного
 * {@code InstanceLauncher}. Нам нужно:
 * <ol>
 *   <li>прочитать JSON как tree-model,</li>
 *   <li>убедиться, что на instance подходящая версия Minecraft/Fabric,</li>
 *   <li>дописать в {@code launcher.javaArguments} нашу строку
 *       {@code -javaagent:&lt;abs path&gt;}, избегая дублирования,</li>
 *   <li>сохранить.</li>
 * </ol>
 *
 * <p>ATLauncher парсит {@code javaArguments} простым split'ом по пробелу
 * и НЕ понимает кавычки (см. research report), поэтому пути с пробелами
 * нельзя использовать. Handler копирует loader.jar в каталог без
 * пробелов (инстанс-root/vida/) и передаёт абсолютный путь сюда.
 */
public final class ATLauncherJsonPatcher {

    /** Маркер, по которому узнаём «наш» -javaagent при повторной установке. */
    public static final String AGENT_MARKER = "/vida/vida-loader-";

    /**
     * Результат патча {@code instance.json}.
     *
     * @param previousArgs  прежнее значение {@code launcher.javaArguments} (может быть null)
     * @param newArgs       новое значение
     * @param alreadyAgent  {@code true} если маркер нашего agent'а уже был в args
     *                      и мы только обновили путь
     */
    public record Result(String previousArgs, String newArgs, boolean alreadyAgent) {}

    private ATLauncherJsonPatcher() {}

    /**
     * Патчит файл на диске. Атомарный rename через {@code .tmp}.
     */
    public static Result patch(Path instanceJson, String agentJarAbsPath,
                               String minecraftVersion, String loaderVersion) throws IOException {
        Objects.requireNonNull(instanceJson, "instanceJson");
        Objects.requireNonNull(agentJarAbsPath, "agentJarAbsPath");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(loaderVersion, "loaderVersion");

        if (agentJarAbsPath.indexOf(' ') >= 0) {
            throw new IOException(
                    "ATLauncher cannot handle whitespace in javaArguments: " + agentJarAbsPath);
        }

        String body = Files.readString(instanceJson, StandardCharsets.UTF_8);
        Object tree = JsonTree.parse(body);
        if (!(tree instanceof Map<?, ?> root)) {
            throw new IOException("instance.json is not a JSON object: " + instanceJson);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> launcher = (Map<String, Object>)
                ((Map<String, Object>) root).computeIfAbsent("launcher", k -> new LinkedHashMap<>());

        String prev = objectToStringOrNull(launcher.get("javaArguments"));
        String stripped = VidaInstallerJvm.stripManagedInstallerJvmTokens(
                prev != null ? prev : "");
        String newArg = "-javaagent:" + agentJarAbsPath;
        StringBuilder merged = new StringBuilder();
        boolean alreadyAgent = false;

        if (!stripped.isBlank()) {
            for (String tok : stripped.split(" ")) {
                if (tok.isBlank()) continue;
                if (tok.contains(AGENT_MARKER) || tok.startsWith("-javaagent:")
                        && tok.contains("vida-loader")) {
                    // заменяем старый наш agent на новый
                    alreadyAgent = true;
                    continue;
                }
                if (merged.length() > 0) merged.append(' ');
                merged.append(tok);
            }
        }
        if (merged.length() > 0) merged.append(' ');
        merged.append(newArg);
        merged.append(' ');
        merged.append(VidaInstallerJvm.spaceSeparatedInstallerJvmProps(minecraftVersion, loaderVersion));

        launcher.put("javaArguments", merged.toString());

        @SuppressWarnings("unchecked")
        String rendered = JsonTree.write((Map<String, Object>) root);
        Path tmp = instanceJson.resolveSibling(instanceJson.getFileName() + ".tmp");
        Files.writeString(tmp, rendered, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, instanceJson,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            Files.move(tmp, instanceJson,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        return new Result(prev, merged.toString(), alreadyAgent);
    }

    private static String objectToStringOrNull(Object o) {
        return o == null ? null : o.toString();
    }
}
