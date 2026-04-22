/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class PrismInstanceCfgTest {

    @Test
    void default_fields_present() {
        String rendered = new PrismInstanceCfg("Vida 1.21.1").render();
        assertThat(rendered)
                .contains("InstanceType=OneSix")
                .contains("name=Vida 1.21.1")
                .contains("OverrideJavaArgs=false");
    }

    @Test
    void with_jvm_args_flips_override_flag_and_adds_args() {
        String rendered = new PrismInstanceCfg("X")
                .withJvmArgs("-javaagent:/opt/vida/loader.jar")
                .render();
        assertThat(rendered)
                .contains("OverrideJavaArgs=true")
                .contains("JvmArgs=-javaagent:/opt/vida/loader.jar");
    }

    @Test
    void set_overrides_values() {
        String rendered = new PrismInstanceCfg("X")
                .set("iconKey", "forge")
                .render();
        assertThat(rendered).contains("iconKey=forge");
    }

    @Test
    void keys_are_ordered_deterministically() {
        String a = new PrismInstanceCfg("A").render();
        String b = new PrismInstanceCfg("A").render();
        assertThat(a).isEqualTo(b);
    }
}
