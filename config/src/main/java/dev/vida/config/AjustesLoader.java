/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import dev.vida.config.internal.TomlBridge;
import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Загружает {@link Ajustes} из TOML-источников с поддержкой профилей.
 *
 * <h2>Модель слоёв</h2>
 * <pre>
 *   base ← profile (если активен) ← runtime-overlay (опционально)
 * </pre>
 *
 * <p>Профиль может определяться двумя способами:
 * <ol>
 *   <li><b>Вложенный</b>: в том же файле существует секция
 *       {@code [profile.&lt;name&gt;]}. При активации именно она мержится поверх
 *       корня (при этом сама секция {@code profile} удаляется из результата).</li>
 *   <li><b>Внешний</b>: загружается через {@link Builder#overlay}, что даёт
 *       полный контроль над источниками.</li>
 * </ol>
 *
 * <h2>Пример</h2>
 * <pre>{@code
 * Result<Ajustes, AjustesError> r = AjustesLoader
 *     .fromToml("vida.toml", tomlText)
 *     .withProfile("debug")
 *     .build();
 *
 * if (r.isOk()) {
 *     Ajustes cfg = r.unwrap();
 *     int port = cfg.getInt("server.port", 25565);
 * } else {
 *     log.error(r.unwrapErr().message());
 * }
 * }</pre>
 */
@ApiStatus.Stable
public final class AjustesLoader {

    /** Имя под-таблицы, содержащей именованные профили. */
    public static final String PROFILE_TABLE = "profile";

    private AjustesLoader() {}

    // ============================================================ factories

    public static Builder fromToml(String sourceName, String toml) {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(toml, "toml");
        return new Builder(sourceName, toml);
    }

    public static Builder fromFile(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return new Builder(file.toString(), text);
    }

    /** Без источника: конструирует пустой loader, к которому можно добавить overlay. */
    public static Builder empty() {
        return new Builder("<empty>", "");
    }

    // ============================================================== Builder

    public static final class Builder {

        private final String baseSource;
        private final String baseToml;
        private Optional<String> profileName = Optional.empty();
        private final List<OverlaySource> overlays = new ArrayList<>();
        private Optional<ConfigNode.Table> runtimeOverlay = Optional.empty();

        private Builder(String baseSource, String baseToml) {
            this.baseSource = baseSource;
            this.baseToml = baseToml;
        }

        /** Выбрать активный профиль (встроенный). */
        public Builder withProfile(String name) {
            this.profileName = Optional.ofNullable(name).filter(s -> !s.isBlank());
            return this;
        }

        /** Добавить внешний TOML-overlay поверх базы и встроенного профиля. */
        public Builder overlay(String sourceName, String toml) {
            overlays.add(new OverlaySource(sourceName, toml));
            return this;
        }

        /** Добавить runtime-overlay: готовую таблицу (например, из флагов CLI). */
        public Builder runtime(ConfigNode.Table overlay) {
            this.runtimeOverlay = Optional.ofNullable(overlay);
            return this;
        }

        public Result<Ajustes, AjustesError> build() {
            Result<ConfigNode.Table, AjustesError> baseR = parse(baseSource, baseToml);
            if (baseR.isErr()) return Result.err(baseR.unwrapErr());
            ConfigNode.Table base = baseR.unwrap();

            // 1) Встроенный профиль (если запрошен).
            ConfigNode profilesNode = base.get(PROFILE_TABLE);
            ConfigNode.Table withoutProfiles =
                    profilesNode != null ? removeKey(base, PROFILE_TABLE) : base;

            ConfigNode.Table stack = withoutProfiles;

            if (profileName.isPresent()) {
                String name = profileName.get();
                if (!(profilesNode instanceof ConfigNode.Table profiles)) {
                    return Result.err(new AjustesError.UnknownProfile(name));
                }
                ConfigNode selected = profiles.get(name);
                if (!(selected instanceof ConfigNode.Table overlay)) {
                    return Result.err(new AjustesError.UnknownProfile(name));
                }
                stack = ConfigMerger.merge(stack, overlay);
            }

            // 2) Внешние overlay.
            for (OverlaySource o : overlays) {
                Result<ConfigNode.Table, AjustesError> ovR = parse(o.source(), o.toml());
                if (ovR.isErr()) return Result.err(ovR.unwrapErr());
                stack = ConfigMerger.merge(stack, ovR.unwrap());
            }

            // 3) Runtime overlay.
            if (runtimeOverlay.isPresent()) {
                stack = ConfigMerger.merge(stack, runtimeOverlay.get());
            }

            return Result.ok(new Ajustes(stack));
        }
    }

    // ----------------------------------------------------------- internals

    private static Result<ConfigNode.Table, AjustesError> parse(String source, String toml) {
        if (toml.isBlank()) return Result.ok(ConfigNode.Table.EMPTY);
        Result<ConfigNode.Table, TomlBridge.ParseFailure> r = TomlBridge.parse(source, toml);
        return r.mapErr(f -> new AjustesError.SyntaxError(f.source(), f.line(), f.column(), f.detail()));
    }

    /** Возвращает новую таблицу без указанного ключа. */
    private static ConfigNode.Table removeKey(ConfigNode.Table base, String key) {
        if (!base.has(key)) return base;
        ConfigNode.Table.Builder b = ConfigNode.Table.builder();
        for (Map.Entry<String, ConfigNode> e : base.entries().entrySet()) {
            if (!e.getKey().equals(key)) {
                b.put(e.getKey(), e.getValue());
            }
        }
        return b.build();
    }

    private record OverlaySource(String source, String toml) {}
}
