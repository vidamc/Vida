/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import dev.vida.gradle.tasks.GenerateManifestTask;
import dev.vida.gradle.tasks.PackagePuertasTask;
import dev.vida.gradle.tasks.RemapJarTask;
import dev.vida.gradle.tasks.RunVidaTask;
import dev.vida.gradle.tasks.ValidateManifestTask;
import dev.vida.gradle.tasks.ValidatePuertasTask;
import java.io.File;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

/**
 * Точка входа плагина {@code dev.vida.mod}.
 *
 * <p>Поведение:
 * <ol>
 *   <li>применяет {@code java-library};</li>
 *   <li>создаёт extension {@code vida};</li>
 *   <li>регистрирует задачи {@code vidaGenerateManifest},
 *       {@code vidaValidateManifest}, {@code vidaRemapJar},
 *       {@code vidaRun};</li>
 *   <li>подключает сгенерированный {@code vida.mod.json} к resources
 *       sourceSet {@code main};</li>
 *   <li>при {@code vida.validateManifestOnBuild = true} — ставит
 *       валидацию в зависимость от {@code jar}.</li>
 * </ol>
 */
public class VidaPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "vida";
    public static final String TASK_GROUP = "vida";
    public static final String PLUGIN_ID = "dev.vida.mod";

    public static final String TASK_GENERATE_MANIFEST = "vidaGenerateManifest";
    public static final String TASK_VALIDATE_MANIFEST = "vidaValidateManifest";
    public static final String TASK_VALIDATE_PUERTAS  = "vidaValidatePuertas";
    public static final String TASK_PACKAGE_PUERTAS   = "vidaPackagePuertas";
    public static final String TASK_REMAP_JAR         = "vidaRemapJar";
    public static final String TASK_RUN               = "vidaRun";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java-library");

        VidaExtension ext = project.getExtensions().create(
                EXTENSION_NAME, VidaExtension.class, project);

        File genDir = new File(project.getLayout().getBuildDirectory().getAsFile().get(),
                "generated/vida/resources");
        File manifestFile = new File(genDir, "vida.mod.json");

        TaskProvider<GenerateManifestTask> generate = project.getTasks().register(
                TASK_GENERATE_MANIFEST, GenerateManifestTask.class, t -> {
                    t.getSchema().set(ext.getMod().getSchema());
                    t.getModId().set(ext.getMod().getId());
                    t.getModVersion().set(ext.getMod().getVersion());
                    t.getDisplayName().set(ext.getMod().getDisplayName());
                    t.getModDescription().set(ext.getMod().getDescription());
                    t.getLicense().set(ext.getMod().getLicense());
                    t.getEntrypoint().set(ext.getMod().getEntrypoint());
                    t.getEntrypointsPreLaunch().set(ext.getMod().getEntrypointsPreLaunch());
                    t.getEntrypointsMain().set(ext.getMod().getEntrypointsMain());
                    t.getEntrypointsClient().set(ext.getMod().getEntrypointsClient());
                    t.getEntrypointsServer().set(ext.getMod().getEntrypointsServer());
                    t.getAuthors().set(ext.getMod().getAuthors());
                    t.getPuertas().set(ext.getMod().getPuertas());
                    t.getEscultores().set(ext.getMod().getEscultores());
                    t.getVifadaPackages().set(ext.getMod().getVifadaPackages());
                    t.getVifadaConfig().set(ext.getMod().getVifadaConfig());
                    t.getVifadaPriority().set(ext.getMod().getVifadaPriority());
                    t.getModules().set(ext.getMod().getModules());
                    t.getDependencies().set(ext.getMod().getDependencies());
                    t.getOptionalDependencies().set(ext.getMod().getOptionalDependencies());
                    t.getIncompatibilities().set(ext.getMod().getIncompatibilities());
                    t.getInjectDefaultVidaDependency().set(ext.getInjectDefaultVidaDependency());
                    t.getDefaultVidaDependencyRange().set(ext.getDefaultVidaDependencyRange());
                    t.getOutput().set(project.getLayout().file(project.provider(() -> manifestFile)));
                });

        TaskProvider<ValidateManifestTask> validate = project.getTasks().register(
                TASK_VALIDATE_MANIFEST, ValidateManifestTask.class, t -> {
                    t.getResourcesRoot().set(project.getLayout().getProjectDirectory().dir(
                            "src/main/resources"));
                });

        File puertasDir = new File(project.getLayout().getBuildDirectory().getAsFile().get(),
                "generated/vida/puertas");
        File resourcesRoot = new File(project.getProjectDir(), "src/main/resources");

        TaskProvider<ValidatePuertasTask> validatePuertas = project.getTasks().register(
                TASK_VALIDATE_PUERTAS, ValidatePuertasTask.class, t -> {
                    t.getPuertasPaths().set(ext.getMod().getPuertas());
                    t.getResourcesRoot().set(project.getLayout().getProjectDirectory()
                            .dir("src/main/resources"));
                    t.onlyIf(task -> {
                        List<String> list = ext.getMod().getPuertas().getOrElse(List.of());
                        return !list.isEmpty() && resourcesRoot.exists();
                    });
                });

        TaskProvider<PackagePuertasTask> packagePuertas = project.getTasks().register(
                TASK_PACKAGE_PUERTAS, PackagePuertasTask.class, t -> {
                    t.getPuertasPaths().set(ext.getMod().getPuertas());
                    t.getResourcesRoot().set(project.getLayout().getProjectDirectory()
                            .dir("src/main/resources"));
                    t.getOutputDir().set(
                            project.getLayout().getBuildDirectory().dir("generated/vida/puertas"));
                    t.dependsOn(validatePuertas);
                    t.onlyIf(task -> !ext.getMod().getPuertas().getOrElse(List.of()).isEmpty());
                });

        project.getTasks().register(TASK_REMAP_JAR, RemapJarTask.class, t -> {
            t.getObfNamespace().set(ext.getMinecraft().getMappings().getObfNamespace());
            t.getNamedNamespace().set(ext.getMinecraft().getMappings().getNamedNamespace());
            t.getProguardMappings().set(ext.getMinecraft().getMappings().getProguard());
            t.getCtgMappings().set(ext.getMinecraft().getMappings().getCtg());
        });

        project.getTasks().register(TASK_RUN, RunVidaTask.class, t -> {
            t.setDescription("Run Minecraft with Vida loader attached.");
            t.getClientJar().set(ext.getMinecraft().getClientJar());
            t.getMinecraftVersion().set(ext.getMinecraft().getVersion());
            t.getPlatformProfile().set(ext.getMinecraft().getPlatformProfile());
            t.getAgent().set(ext.getRun().getAgent());
            t.getStrict().set(ext.getRun().getStrict());
            t.getVidaJvmArgs().set(ext.getRun().getJvmArgs());
            t.getVidaArgs().set(ext.getRun().getArgs());
            t.getVidaMainClass().set(ext.getRun().getMainClass());
            t.getAccessDeniedIds().set(ext.getRun().getAccessDeniedIds());
            t.getHotReload().set(ext.getRun().getHotReload());
            t.getHotReloadWatchDir().convention(project.getLayout().getBuildDirectory()
                    .dir("classes/java/main"));
            t.setWorkingDir(ext.getRun().getWorkingDir().getAsFile().getOrElse(
                    new File(project.getLayout().getBuildDirectory().getAsFile().get(), "run")));
        });

        // Встраиваем сгенерированный vida.mod.json в resources.
        project.afterEvaluate(p -> {
            ext.getMinecraft().getPlatformProfile().convention(
                    ext.getMinecraft().getVersion().map(VidaPlugin::inferPlatformProfileFromGameVersion));
            boolean autoManifest = Boolean.TRUE.equals(ext.getAutoGenerateManifest().getOrElse(true));
            validate.configure(t -> {
                if (autoManifest) {
                    t.getManifestFile().set(project.getLayout().file(project.provider(() -> manifestFile)));
                    t.dependsOn(generate);
                } else {
                    t.getManifestFile().set(p.getLayout().getProjectDirectory().file(
                            "src/main/resources/vida.mod.json"));
                }
            });
            if (autoManifest) {
                JavaPluginExtension java = p.getExtensions().getByType(JavaPluginExtension.class);
                java.getSourceSets().named("main", ss ->
                        ss.getResources().srcDir(project.provider(() -> genDir)));
                p.getTasks().named("processResources").configure(t -> t.dependsOn(generate));
            }
            if (Boolean.TRUE.equals(ext.getValidateManifestOnBuild().getOrElse(true))) {
                p.getTasks().withType(Jar.class).configureEach(j -> j.dependsOn(validate));
            }
            // .ptr: если мод указал хотя бы один файл — включаем в resources + валидацию.
            List<String> ptrs = ext.getMod().getPuertas().getOrElse(List.of());
            if (!ptrs.isEmpty()) {
                JavaPluginExtension java = p.getExtensions().getByType(JavaPluginExtension.class);
                java.getSourceSets().named("main", ss ->
                        ss.getResources().srcDir(project.provider(() -> puertasDir)));
                p.getTasks().named("processResources").configure(t -> t.dependsOn(packagePuertas));
                p.getTasks().withType(Jar.class).configureEach(j -> j.dependsOn(validatePuertas));
            }
        });
    }

    /**
     * Maps {@code vida.minecraft.version} to a default {@code platform-profiles/} id when the user
     * did not set {@code vida.minecraft.platformProfile} explicitly.
     */
    static String inferPlatformProfileFromGameVersion(String mcVersion) {
        if (mcVersion == null) {
            return null;
        }
        String t = mcVersion.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.matches("1\\.21\\.\\d+")) {
            return "legacy-121/" + t;
        }
        if (t.matches("26\\..+")) {
            return "calendar-26/" + t;
        }
        return null;
    }
}
