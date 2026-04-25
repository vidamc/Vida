/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import dev.vida.manifest.VifadaConfig;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/**
 * Корневое расширение DSL {@code vida { ... }}.
 *
 * <p>Создаётся {@link VidaPlugin}'ом и кладётся в {@code project.extensions}
 * под именем {@value VidaPlugin#EXTENSION_NAME}.
 */
public abstract class VidaExtension {

    private final ModInfoSpec mod;
    private final MinecraftSpec minecraft;
    private final RunSpec run;

    @Inject
    public VidaExtension(Project project, ObjectFactory objects) {
        this.mod = objects.newInstance(ModInfoSpec.class);
        this.minecraft = objects.newInstance(MinecraftSpec.class);
        this.run = objects.newInstance(RunSpec.class);

        // Дефолты, зависящие от project-метадаты.
        mod.getSchema().convention(1);
        mod.getId().convention(project.getName());
        mod.getDisplayName().convention(mod.getId());
        mod.getDescription().convention("");
        mod.getLicense().convention("Apache-2.0");
        mod.getVersion().convention(project.provider(() -> {
            Object v = project.getVersion();
            return v == null ? "0.0.0" : v.toString();
        }));
        mod.getEntrypointsPreLaunch().convention(List.of());
        mod.getEntrypointsMain().convention(List.of());
        mod.getEntrypointsClient().convention(List.of());
        mod.getEntrypointsServer().convention(List.of());
        mod.getVifadaPackages().convention(List.of());
        mod.getVifadaConfig().convention("");
        mod.getVifadaPriority().convention(VifadaConfig.DEFAULT_PRIORITY);
        mod.getModules().convention(List.of());

        run.getMainClass().convention("net.minecraft.client.main.Main");
        run.getWorkingDir().convention(project.getLayout().getBuildDirectory().dir("run"));
        run.getStrict().convention(true);
        run.getAgent().convention(true);
        run.getAccessDeniedIds().convention(List.of());
        run.getHotReload().convention(false);

        getAutoGenerateManifest().convention(true);
        getValidateManifestOnBuild().convention(true);
        getInjectDefaultVidaDependency().convention(true);
        getDefaultVidaDependencyRange().convention("^0.1.0");
    }

    public ModInfoSpec getMod()      { return mod; }
    public MinecraftSpec getMinecraft() { return minecraft; }
    public RunSpec getRun()          { return run; }

    /** Автоматически генерировать {@code vida.mod.json}? Если {@code false}, мод сам кладёт файл в ресурсы. */
    public abstract Property<Boolean> getAutoGenerateManifest();

    /** Автоматически валидировать {@code vida.mod.json} на каждой сборке. */
    public abstract Property<Boolean> getValidateManifestOnBuild();

    /**
     * Если {@code true} (по умолчанию), в генерируемый манифест добавляется
     * {@code dependencies.required.vida}, если моддер ещё не указал зависимость на платформу.
     */
    public abstract Property<Boolean> getInjectDefaultVidaDependency();

    /**
     * Диапазон SemVer для автоматически добавляемой зависимости {@code vida}
     * (когда {@link #getInjectDefaultVidaDependency()} включён).
     */
    public abstract Property<String> getDefaultVidaDependencyRange();

    // ---- Groovy/Kotlin DSL proxies ----

    public void mod(Action<? super ModInfoSpec> action) {
        action.execute(mod);
    }

    public void minecraft(Action<? super MinecraftSpec> action) {
        action.execute(minecraft);
    }

    public void run(Action<? super RunSpec> action) {
        action.execute(run);
    }
}
