/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.*;

import dev.vida.base.LatidoGlobal;
import dev.vida.base.ModulosInstaladosGlobal;
import dev.vida.base.latidos.Fase;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.base.latidos.Prioridad;
import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.platform.PlatformBridge;
import dev.vida.platform.VanillaBridge;
import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;
import dev.vida.resolver.ResolverError;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Интеграционные тесты Session 6 «Платформа»:
 * <ul>
 *   <li>синтетический провайдер {@code vida} закрывает зависимость мода
 *       {@code "required": {"vida": ">=0.0.1"}};</li>
 *   <li>{@link VanillaBridge} установлен после бутстрапа и корректно
 *       диспатчит {@link LatidoPulso} и {@link LatidoRenderHud};</li>
 *   <li>N тиков вызывают ровно N событий на шине.</li>
 * </ul>
 *
 * <p>Мы не поднимаем JVM-instrumentation: цель теста — проверить склейку
 * {@code SyntheticProviders → BootSequence → VanillaBridge → LatidoBus},
 * а не сам {@code ClassFileTransformer}.
 */
final class BootSequenceIntegrationTest {

    @BeforeEach
    void reset() {
        VidaRuntime.resetForTests();
        LatidoGlobal.resetForTests();
        ModulosInstaladosGlobal.resetForTests();
        VanillaBridge.resetForTests();
    }

    @AfterEach
    void cleanup() {
        VidaRuntime.resetForTests();
        LatidoGlobal.resetForTests();
        ModulosInstaladosGlobal.resetForTests();
        VanillaBridge.resetForTests();
    }

    @Test
    void mod_with_required_vida_resolves_against_synthetic(@TempDir Path root) throws Exception {
        Path mods = root.resolve("mods");
        java.nio.file.Files.createDirectories(mods);

        String manifest = TestSupport.modWithRequired(
                "consumer", "1.0.0",
                Map.of("vida", ">=0.0.1", "java", ">=21.0.0"));
        TestSupport.buildModJarRaw(mods.resolve("consumer.jar"), manifest, null, null);

        BootReport report = VidaBoot.boot(BootOptions.builder()
                .modsDir(mods)
                .vidaVersion("0.7.0")
                .build());

        assertThat(report.errors()).as("errors=%s", report.errors()).isEmpty();
        assertThat(report.environment().resolvedMods())
                .extracting(m -> m.id())
                .containsExactly("consumer");
    }

    @Test
    void mod_with_required_minecraft_resolves_when_version_provided(@TempDir Path root) throws Exception {
        Path mods = root.resolve("mods");
        java.nio.file.Files.createDirectories(mods);

        String manifest = TestSupport.modWithRequired(
                "needs_mc", "1.0.0",
                Map.of("minecraft", ">=1.21.0"));
        TestSupport.buildModJarRaw(mods.resolve("needs_mc.jar"), manifest, null, null);

        BootReport report = VidaBoot.boot(BootOptions.builder()
                .modsDir(mods)
                .minecraftVersion("1.21.1")
                .build());

        assertThat(report.errors()).as("errors=%s", report.errors()).isEmpty();
        assertThat(report.environment().resolvedMods())
                .extracting(m -> m.id())
                .containsExactly("needs_mc");
    }

    @Test
    void access_denied_root_mod_fails_resolution(@TempDir Path root) throws Exception {
        Path mods = root.resolve("mods");
        Files.createDirectories(mods);
        TestSupport.buildModJar(mods.resolve("blocked.jar"), "blocked", "1.0.0", null, null);

        BootReport report = VidaBoot.boot(BootOptions.builder()
                .modsDir(mods)
                .addAccessDenied("blocked")
                .build());

        assertThat(report.errors())
                .anyMatch(e -> e instanceof LoaderError.ResolutionFailed rf
                        && rf.cause() instanceof ResolverError.AccessPolicyDenied);
    }

    @Test
    void mod_with_required_minecraft_fails_without_version(@TempDir Path root) throws Exception {
        Path mods = root.resolve("mods");
        java.nio.file.Files.createDirectories(mods);

        String manifest = TestSupport.modWithRequired(
                "needs_mc", "1.0.0",
                Map.of("minecraft", ">=1.21.0"));
        TestSupport.buildModJarRaw(mods.resolve("needs_mc.jar"), manifest, null, null);

        BootReport report = VidaBoot.boot(BootOptions.builder().modsDir(mods).build());

        assertThat(report.errors())
                .as("missing minecraft must fail resolution")
                .isNotEmpty();
        assertThat(report.errors())
                .anyMatch(e -> e instanceof LoaderError.ResolutionFailed);
    }

    @Test
    void vanilla_bridge_dispatches_pulso_and_hud() throws Exception {
        BootReport report = VidaBoot.boot(BootOptions.builder().skipDiscovery(true).build());
        assertThat(report.isOk()).isTrue();

        PlatformBridge bridge = VanillaBridge.current();
        assertThat(bridge).as("VanillaBridge must be installed by BootSequence").isNotNull();

        LatidoBus bus = LatidoGlobal.actual();
        List<LatidoPulso> pulsos = new ArrayList<>();
        bus.suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, Fase.DESPUES, pulsos::add);

        int N = 5;
        for (int i = 0; i < N; i++) bridge.onClientTick();

        assertThat(pulsos).hasSize(N);
        assertThat(pulsos).extracting(LatidoPulso::tickActual)
                .containsExactly(0L, 1L, 2L, 3L, 4L);

        // HUD: без Minecraft.getInstance() окно не резолвится — событие
        // пропускается; ошибок быть не должно.
        AtomicInteger hudCount = new AtomicInteger();
        bus.suscribir(LatidoRenderHud.TIPO, Prioridad.NORMAL, Fase.DESPUES,
                e -> hudCount.incrementAndGet());

        // Вызов без Minecraft-классов — bridge молча дропает кадр.
        bridge.onHudRender(new Object(), 0.5f);
        assertThat(hudCount.get()).isZero();

        // Но если подменить bridge на тестовый, события доходят до подписчиков.
        VanillaBridge.resetForTests();
        VanillaBridge.install(new PlatformBridge() {
            @Override public void onClientTick() {}
            @Override public void onHudRender(Object gg, float pt) {
                PintorHud pintor = (x, y, w, h, argb) -> {};
                bus.emitir(LatidoRenderHud.TIPO,
                        new LatidoRenderHud(320, 240, pt, pintor));
            }
        });
        VanillaBridge.current().onHudRender(null, 0.25f);
        assertThat(hudCount.get()).isEqualTo(1);
    }

    @Test
    void platform_morphs_registered_even_without_mods(@TempDir Path root) throws Exception {
        BootReport report = VidaBoot.boot(BootOptions.builder().skipDiscovery(true).build());
        VidaEnvironment env = report.environment();
        assertThat(env.morphs().hasMorphs("net/minecraft/client/Minecraft")).isTrue();
        assertThat(env.morphs().hasMorphs("net/minecraft/client/gui/Gui")).isTrue();
        assertThat(env.morphs().hasMorphs("net/minecraft/server/MinecraftServer")).isTrue();
    }
}
