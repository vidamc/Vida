/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import static org.assertj.core.api.Assertions.*;

import dev.vida.gradle.tasks.GenerateManifestTask;
import dev.vida.gradle.tasks.RemapJarTask;
import dev.vida.gradle.tasks.RunVidaTask;
import dev.vida.gradle.tasks.ValidateManifestTask;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

/**
 * Проверки применения плагина в in-memory Project.
 *
 * <p>Работают без гонки процессов: {@link ProjectBuilder} даёт полноценный
 * {@link Project} без запуска отдельной JVM.
 */
final class VidaPluginTest {

    private static Project mkProject() {
        Project p = ProjectBuilder.builder().build();
        p.setVersion("1.2.3");
        return p;
    }

    @Test
    void plugin_applies_cleanly() {
        Project p = mkProject();
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        assertThat(p.getPluginManager().hasPlugin("java-library")).isTrue();
        assertThat(p.getExtensions().findByName(VidaPlugin.EXTENSION_NAME))
                .isInstanceOf(VidaExtension.class);
    }

    @Test
    void registers_all_tasks() {
        Project p = mkProject();
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        assertThat(p.getTasks().findByName(VidaPlugin.TASK_GENERATE_MANIFEST))
                .isInstanceOf(GenerateManifestTask.class);
        assertThat(p.getTasks().findByName(VidaPlugin.TASK_VALIDATE_MANIFEST))
                .isInstanceOf(ValidateManifestTask.class);
        assertThat(p.getTasks().findByName(VidaPlugin.TASK_REMAP_JAR))
                .isInstanceOf(RemapJarTask.class);
        assertThat(p.getTasks().findByName(VidaPlugin.TASK_RUN))
                .isInstanceOf(RunVidaTask.class);
    }

    @Test
    void default_id_and_version_come_from_project() {
        Project p = ProjectBuilder.builder().withName("miaventura").build();
        p.setVersion("0.4.2");
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        VidaExtension ext = (VidaExtension) p.getExtensions().getByName(VidaPlugin.EXTENSION_NAME);
        assertThat(ext.getMod().getId().get()).isEqualTo("miaventura");
        assertThat(ext.getMod().getVersion().get()).isEqualTo("0.4.2");
        assertThat(ext.getMod().getDisplayName().get()).isEqualTo("miaventura");
    }

    @Test
    void infer_platform_profile_from_semantic_121_and_calendar_26() {
        assertThat(VidaPlugin.inferPlatformProfileFromGameVersion("1.21.11"))
                .isEqualTo("legacy-121/1.21.11");
        assertThat(VidaPlugin.inferPlatformProfileFromGameVersion("26.1.0"))
                .isEqualTo("calendar-26/26.1.0");
        assertThat(VidaPlugin.inferPlatformProfileFromGameVersion("23.45")).isNull();
    }

    @Test
    void dsl_captures_configuration() {
        Project p = mkProject();
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        VidaExtension ext = (VidaExtension) p.getExtensions().getByName(VidaPlugin.EXTENSION_NAME);
        ext.mod(m -> {
            m.getId().set("demo");
            m.getEntrypoint().set("com.ejemplo.Demo");
            m.getAuthors().set(java.util.List.of("Ana"));
            m.dependency("vida", "^0.1");
        });
        ext.minecraft(mc -> {
            mc.getVersion().set("1.21.1");
        });
        ext.run(r -> {
            r.getMainClass().set("net.minecraft.client.main.Main");
            r.getStrict().set(false);
        });

        assertThat(ext.getMod().getId().get()).isEqualTo("demo");
        assertThat(ext.getMod().getEntrypoint().get()).isEqualTo("com.ejemplo.Demo");
        assertThat(ext.getMod().getAuthors().get()).containsExactly("Ana");
        assertThat(ext.getMod().getDependencies().get()).containsEntry("vida", "^0.1");
        assertThat(ext.getMinecraft().getVersion().get()).isEqualTo("1.21.1");
        assertThat(ext.getRun().getStrict().get()).isFalse();
    }

    @Test
    void tasks_are_in_vida_group() {
        Project p = mkProject();
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        for (String name : new String[]{
                VidaPlugin.TASK_GENERATE_MANIFEST,
                VidaPlugin.TASK_VALIDATE_MANIFEST,
                VidaPlugin.TASK_REMAP_JAR,
                VidaPlugin.TASK_RUN}) {
            assertThat(p.getTasks().getByName(name).getGroup())
                    .as("group for " + name)
                    .isEqualTo(VidaPlugin.TASK_GROUP);
        }
    }
}
