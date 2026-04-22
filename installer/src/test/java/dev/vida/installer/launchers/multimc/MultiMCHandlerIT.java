/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.multimc;

import static org.assertj.core.api.Assertions.*;

import dev.vida.installer.InstallOptions;
import dev.vida.installer.InstallReport;
import dev.vida.installer.InstallerCore;
import dev.vida.installer.launchers.LauncherKind;
import dev.vida.installer.mc.JsonTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MultiMCHandlerIT {

    @SuppressWarnings("unchecked")
    @Test
    void multimc_install_uses_jvm_args_not_agents(@TempDir Path data) throws IOException {
        InstallOptions opt = InstallOptions.builder()
                .launcherKind(LauncherKind.MULTIMC)
                .installDir(data)
                .minecraftVersion("1.21.1")
                .loaderVersion("0.1.0")
                .build();

        InstallReport rep = new InstallerCore(msg -> {}).install(opt);
        assertThat(rep.isOk()).as("errors: %s", rep.errors()).isTrue();

        Path inst = data.resolve("instances/vida-1.21.1-0.1.0");

        String cfg = Files.readString(inst.resolve("instance.cfg"), StandardCharsets.UTF_8);
        assertThat(cfg)
                .contains("OverrideJavaArgs=true")
                .contains("JvmArgs=-javaagent:")
                .contains("loader-0.1.0.jar");

        String patchJson = Files.readString(inst.resolve("patches/dev.vida.loader.json"),
                StandardCharsets.UTF_8);
        Map<String, Object> patch = (Map<String, Object>) JsonTree.parse(patchJson);
        assertThat(patch).doesNotContainKey("+agents");
        assertThat(patch).containsKey("+mavenFiles");

        assertThat(rep.warnings())
                .anyMatch(w -> w.contains("JvmArgs-based"));
    }
}
