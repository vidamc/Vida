/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.tasks;

import static org.assertj.core.api.Assertions.*;

import dev.vida.core.VersionRange;
import dev.vida.gradle.VidaExtension;
import dev.vida.gradle.VidaPlugin;
import dev.vida.manifest.ManifestParser;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

final class GenerateManifestTaskTest {

    @Test
    void task_writes_parseable_manifest() throws Exception {
        File projDir = Files.createTempDirectory("vida-plugin-test").toFile();
        projDir.deleteOnExit();

        Project p = ProjectBuilder.builder().withProjectDir(projDir).withName("demo").build();
        p.setVersion("1.0.0");
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        VidaExtension ext = (VidaExtension) p.getExtensions().getByName(VidaPlugin.EXTENSION_NAME);
        ext.getMod().getEntrypoint().set("com.ejemplo.Demo");
        ext.getMod().getAuthors().set(List.of("Ana", "Bob"));
        ext.getMod().dependency("vida", "^0.1");

        GenerateManifestTask task = (GenerateManifestTask)
                p.getTasks().getByName(VidaPlugin.TASK_GENERATE_MANIFEST);

        // ProjectBuilder не выполняет @TaskAction-ы автоматически — вызываем сами.
        invokeActions(task);

        File out = task.getOutput().get().getAsFile();
        assertThat(out).exists();
        String json = Files.readString(out.toPath());
        assertThat(json).contains("\"demo\"").contains("\"1.0.0\"");

        var r = ManifestParser.parse(json);
        assertThat(r.isOk()).as("parseable: %s", json).isTrue();
        assertThat(r.unwrap().id()).isEqualTo("demo");
        assertThat(r.unwrap().entrypoints().main()).containsExactly("com.ejemplo.Demo");
        assertThat(r.unwrap().dependencies().required())
                .containsEntry("vida", VersionRange.parse("^0.1"));
    }

    @Test
    void injects_default_vida_dependency_when_not_declared() throws Exception {
        File projDir = Files.createTempDirectory("vida-plugin-test-inject").toFile();
        projDir.deleteOnExit();

        Project p = ProjectBuilder.builder().withProjectDir(projDir).withName("solo").build();
        p.setVersion("1.0.0");
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        VidaExtension ext = (VidaExtension) p.getExtensions().getByName(VidaPlugin.EXTENSION_NAME);
        ext.getMod().getEntrypoint().set("x.Y");
        ext.getInjectDefaultVidaDependency().set(true);
        ext.getDefaultVidaDependencyRange().set("^2.0.0");

        GenerateManifestTask task = (GenerateManifestTask)
                p.getTasks().getByName(VidaPlugin.TASK_GENERATE_MANIFEST);
        invokeActions(task);

        var r = ManifestParser.parse(Files.readString(task.getOutput().get().getAsFile().toPath()));
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap().dependencies().required())
                .containsEntry("vida", VersionRange.parse("^2.0.0"));
    }

    @Test
    void inject_off_leaves_dependencies_empty_when_user_set_none() throws Exception {
        File projDir = Files.createTempDirectory("vida-plugin-test-noinject").toFile();
        projDir.deleteOnExit();

        Project p = ProjectBuilder.builder().withProjectDir(projDir).withName("solo").build();
        p.setVersion("1.0.0");
        p.getPluginManager().apply(VidaPlugin.PLUGIN_ID);

        VidaExtension ext = (VidaExtension) p.getExtensions().getByName(VidaPlugin.EXTENSION_NAME);
        ext.getMod().getEntrypoint().set("x.Y");
        ext.getInjectDefaultVidaDependency().set(false);

        GenerateManifestTask task = (GenerateManifestTask)
                p.getTasks().getByName(VidaPlugin.TASK_GENERATE_MANIFEST);
        invokeActions(task);

        var r = ManifestParser.parse(Files.readString(task.getOutput().get().getAsFile().toPath()));
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap().dependencies().required()).isEmpty();
    }

    private static void invokeActions(Task t) throws Exception {
        for (var action : t.getActions()) {
            action.execute(t);
        }
    }
}
