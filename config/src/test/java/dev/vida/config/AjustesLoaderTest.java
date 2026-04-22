/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Result;
import org.junit.jupiter.api.Test;

class AjustesLoaderTest {

    private static final String BASE = """
            # Базовый конфиг Vida
            [server]
            port = 25565
            motd = "Hello"
            whitelist = false

            [renderer]
            distance = 16
            simulation = 12
            tags = ["vida", "base"]

            [profile.debug]
            [profile.debug.server]
            motd = "DEBUG"
            port = 25580
            [profile.debug.renderer]
            distance = 32

            [profile.server_side]
            [profile.server_side.server]
            whitelist = true
            """;

    @Test
    void parsesBaseConfig() {
        Result<Ajustes, AjustesError> r = AjustesLoader.fromToml("vida.toml", BASE).build();
        assertThat(r.isOk()).as("err: %s", r.err()).isTrue();
        Ajustes a = r.unwrap();

        assertThat(a.getInt("server.port")).isEqualTo(25565);
        assertThat(a.getString("server.motd")).isEqualTo("Hello");
        assertThat(a.getInt("renderer.distance")).isEqualTo(16);
        // Профильная таблица не должна торчать в итоговом конфиге
        assertThat(a.contains("profile")).isFalse();
    }

    @Test
    void profileOverlayWorks() {
        Ajustes a = AjustesLoader.fromToml("vida.toml", BASE)
                .withProfile("debug")
                .build()
                .unwrap();
        // Ключи из profile.debug перекрывают base
        assertThat(a.getInt("server.port")).isEqualTo(25580);
        assertThat(a.getString("server.motd")).isEqualTo("DEBUG");
        assertThat(a.getInt("renderer.distance")).isEqualTo(32);
        // Ключи не покрытые профилем — сохранились
        assertThat(a.getInt("renderer.simulation")).isEqualTo(12);
        assertThat(a.getBoolean("server.whitelist")).isFalse();
        // Профильная секция по-прежнему вычищена
        assertThat(a.contains("profile")).isFalse();
    }

    @Test
    void unknownProfileFails() {
        Result<Ajustes, AjustesError> r = AjustesLoader.fromToml("vida.toml", BASE)
                .withProfile("not_there").build();
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(AjustesError.UnknownProfile.class);
    }

    @Test
    void externalOverlayApplies() {
        String overlay = """
                [server]
                motd = "from-overlay"
                """;
        Ajustes a = AjustesLoader.fromToml("vida.toml", BASE)
                .withProfile("debug")
                .overlay("override.toml", overlay)
                .build().unwrap();
        // overlay перекрывает и base, и профиль
        assertThat(a.getString("server.motd")).isEqualTo("from-overlay");
        // порт — из профиля
        assertThat(a.getInt("server.port")).isEqualTo(25580);
    }

    @Test
    void runtimeOverlayWinsOverEverything() {
        ConfigNode.Table rt = ConfigNode.Table.builder()
                .put("server", ConfigNode.Table.builder().putInt("port", 11111).build())
                .build();
        Ajustes a = AjustesLoader.fromToml("vida.toml", BASE)
                .withProfile("debug")
                .runtime(rt)
                .build().unwrap();
        assertThat(a.getInt("server.port")).isEqualTo(11111);
    }

    @Test
    void arrayReplacementSemantics() {
        String overlay = """
                [renderer]
                tags = ["only"]
                """;
        Ajustes a = AjustesLoader.fromToml("vida.toml", BASE)
                .overlay("o.toml", overlay)
                .build().unwrap();
        assertThat(a.findStringList("renderer.tags")).contains(java.util.List.of("only"));
    }

    @Test
    void syntaxErrorReturnsTypedError() {
        String broken = "not = a = valid = toml";
        Result<Ajustes, AjustesError> r = AjustesLoader.fromToml("broken.toml", broken).build();
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(AjustesError.SyntaxError.class);
    }

    @Test
    void emptyLoaderProducesEmptyAjustes() {
        Ajustes a = AjustesLoader.empty().build().unwrap();
        assertThat(a.root().isEmpty()).isTrue();
    }

    @Test
    void profileWithoutProfilesTableFails() {
        Result<Ajustes, AjustesError> r = AjustesLoader.fromToml("x.toml", "key = 1")
                .withProfile("debug").build();
        assertThat(r.unwrapErr()).isInstanceOf(AjustesError.UnknownProfile.class);
    }
}
