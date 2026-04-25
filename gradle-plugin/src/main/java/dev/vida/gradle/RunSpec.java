/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * Настройки задачи {@code vidaRun}.
 *
 * <pre>{@code
 * vida.run {
 *     mainClass.set("net.minecraft.client.Main")
 *     jvmArgs.addAll("-Xmx4G", "-XX:+UseZGC")
 *     args.addAll("--accessToken", "dev", "--username", "Dev")
 *     workingDir.set(layout.projectDirectory.dir("run"))
 *     accessDeniedIds.addAll("cheat_mod") // -Dvida.accessDenied=… при vidaRun
 * }
 * }</pre>
 */
public interface RunSpec {

    /** main-класс Minecraft'а. По умолчанию {@code net.minecraft.client.main.Main}. */
    Property<String> getMainClass();

    /** JVM-аргументы. */
    ListProperty<String> getJvmArgs();

    /** Program-аргументы. */
    ListProperty<String> getArgs();

    /** Рабочая директория. По умолчанию — {@code build/run}. */
    DirectoryProperty getWorkingDir();

    /** Использовать строгий режим {@code VidaPremain} (fail-fast при ошибках бутстрапа). */
    Property<Boolean> getStrict();

    /** Подключать ли Vida как Java Agent (иначе — через программный {@code VidaBoot}). */
    Property<Boolean> getAgent();

    /**
     * Id модов, запрещённых политикой резолвера при этом запуске (передаётся как
     * {@code -Dvida.accessDenied=id1,id2}).
     */
    ListProperty<String> getAccessDeniedIds();

    /**
     * Режим разработки: включает отслеживание изменений классов и сброс
     * {@link dev.vida.base.catalogo.CatalogoManejador} (без полного перезапуска JVM).
     *
     * <p>Передаёт {@code -Dvida.dev.hotReload=true} и путь наблюдения для watcher.
     */
    Property<Boolean> getHotReload();
}
