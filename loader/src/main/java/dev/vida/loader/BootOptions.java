/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.manifest.ModManifest;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Параметры запуска Vida.
 *
 * <p>Иммутабельный объект, собирается через {@link Builder}. Поля:
 * <ul>
 *   <li>{@link #modsDir} — корень для сканирования модов;</li>
 *   <li>{@link #extraSources} — отдельные JAR-файлы/директории-моды;</li>
 *   <li>{@link #gameJars} — jar'ы игры и её runtime-зависимости;</li>
 *   <li>{@link #cacheDir} — куда класть {@code mods.idx} и кеши;</li>
 *   <li>{@link #strict} — бросать ли на первой ошибке;</li>
 *   <li>{@link #skipDiscovery} — не сканировать {@code modsDir} (режим
 *       «только extraSources», полезно для тестов);</li>
 *   <li>{@link #vidaVersion} — явная версия платформы для синтетического
 *       провайдера {@code vida} (если не задана — берётся из встроенного
 *       ресурса загрузчика);</li>
 *   <li>{@link #minecraftVersion} — явная версия игры для синтетического
 *       провайдера {@code minecraft} (если не задана — резолвер синтетику
 *       не предоставит, и моды с {@code required.minecraft} получат
 *       {@code ResolverError.Missing});</li>
 *   <li>{@link #platformProfileId} — идентификатор профиля платформы Vida
 *       (например {@code legacy-121/1.21.1}); загрузчик читает
 *       {@code META-INF/vida/platform-profiles/&lt;id&gt;/profile.json};</li>
 *   <li>{@link #accessDeniedIds} — id модов, запрещённых политикой доступа на этапе
 *       {@code Resolver.resolve} (см. {@code ResolverOptions#accessDeniedIds}).</li>
 * </ul>
 */
@ApiStatus.Stable
public final class BootOptions {

    private final Path modsDir;
    private final List<Path> extraSources;
    private final List<Path> gameJars;
    private final Path cacheDir;
    private final boolean strict;
    private final boolean skipDiscovery;
    private final String vidaVersion;
    private final String minecraftVersion;
    private final String platformProfileId;
    private final Set<String> accessDeniedIds;

    private BootOptions(Builder b) {
        this.modsDir          = b.modsDir;
        this.extraSources     = List.copyOf(b.extraSources);
        this.gameJars         = List.copyOf(b.gameJars);
        this.cacheDir         = b.cacheDir;
        this.strict           = b.strict;
        this.skipDiscovery    = b.skipDiscovery;
        this.vidaVersion          = b.vidaVersion;
        this.minecraftVersion     = b.minecraftVersion;
        this.platformProfileId    = b.platformProfileId;
        this.accessDeniedIds      = Set.copyOf(b.accessDeniedIds);
    }

    public Path modsDir()               { return modsDir; }
    public List<Path> extraSources()    { return extraSources; }
    public List<Path> gameJars()        { return gameJars; }
    public Path cacheDir()              { return cacheDir; }
    public boolean strict()             { return strict; }
    public boolean skipDiscovery()      { return skipDiscovery; }

    /**
     * Id модов, которые резолвер не имеет права выбирать (политика лаунчера / JVM).
     * Пустое множество — поведение по умолчанию.
     */
    public Set<String> accessDeniedIds() { return accessDeniedIds; }

    /**
     * Явно заданная версия платформы Vida для синтетического провайдера
     * зависимостей. Если пусто, загрузчик автоматически подставляет
     * версию из встроенного ресурса {@code META-INF/vida/loader-version.properties}.
     */
    public Optional<String> vidaVersion()      { return Optional.ofNullable(vidaVersion); }

    /**
     * Явно заданная версия Minecraft для синтетического провайдера
     * зависимостей. Если пусто, синтетика для {@code minecraft} не публикуется.
     * Запускалки (Mojang Launcher, Prism, MultiMC) всегда знают точную
     * версию профиля и передают её сюда.
     */
    public Optional<String> minecraftVersion() { return Optional.ofNullable(minecraftVersion); }

    /**
     * Явный идентификатор профиля платформы (каталог под
     * {@code META-INF/vida/platform-profiles/}). Если пусто — рантайм может
     * подставить значение из {@code -Dvida.platformProfile}.
     */
    public Optional<String> platformProfileId() {
        return Optional.ofNullable(platformProfileId);
    }

    /**
     * Возвращает копию этих параметров с заменённым {@code modsDir}.
     * Используется для авто-определения директории модов в агенте.
     */
    public BootOptions withModsDir(Path dir) {
        Builder b = new Builder();
        b.modsDir(dir);
        b.cacheDir(this.cacheDir);
        b.strict(this.strict);
        b.skipDiscovery(this.skipDiscovery);
        b.vidaVersion(this.vidaVersion);
        b.minecraftVersion(this.minecraftVersion);
        b.platformProfile(this.platformProfileId);
        for (Path p : this.extraSources) b.addExtraSource(p);
        for (Path p : this.gameJars)     b.addGameJar(p);
        for (String id : this.accessDeniedIds) {
            b.addAccessDenied(id);
        }
        return b.build();
    }

    public static Builder builder() { return new Builder(); }

    /**
     * Разбивает строку со списком id модов (запятые, без пробелов в самом id).
     * Пустые сегменты отбрасываются. Не проверяет {@link ModManifest#ID_PATTERN} —
     * валидация в {@link Builder#addAccessDenied(String)}.
     */
    public static LinkedHashSet<String> parseCommaSeparatedIds(String raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String s = part.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    public static final class Builder {
        private Path modsDir;
        private final List<Path> extraSources = new ArrayList<>();
        private final List<Path> gameJars = new ArrayList<>();
        private Path cacheDir;
        private boolean strict = false;
        private boolean skipDiscovery = false;
        private String vidaVersion;
        private String minecraftVersion;
        private String platformProfileId;
        private final LinkedHashSet<String> accessDeniedIds = new LinkedHashSet<>();

        public Builder modsDir(Path p)        { this.modsDir = p; return this; }
        public Builder addExtraSource(Path p) {
            this.extraSources.add(Objects.requireNonNull(p, "p"));
            return this;
        }
        public Builder addGameJar(Path p) {
            this.gameJars.add(Objects.requireNonNull(p, "p"));
            return this;
        }
        public Builder cacheDir(Path p)       { this.cacheDir = p; return this; }
        public Builder strict(boolean v)      { this.strict = v; return this; }
        public Builder skipDiscovery(boolean v) { this.skipDiscovery = v; return this; }

        /**
         * Явная версия платформы Vida для синтетического провайдера.
         * Значение {@code null} или пустая строка возвращают поле к
         * авто-детекту из встроенного ресурса.
         */
        public Builder vidaVersion(String v) {
            this.vidaVersion = (v == null || v.isBlank()) ? null : v.trim();
            return this;
        }

        /**
         * Явная версия Minecraft для синтетического провайдера. Значение
         * {@code null} или пустая строка отключают синтетику — моды
         * с {@code required.minecraft} получат {@code ResolverError.Missing}.
         */
        public Builder minecraftVersion(String v) {
            this.minecraftVersion = (v == null || v.isBlank()) ? null : v.trim();
            return this;
        }

        /**
         * Идентификатор профиля платформы ({@code null} или пустая строка —
         * отключено).
         */
        public Builder platformProfile(String id) {
            this.platformProfileId = (id == null || id.isBlank()) ? null : id.trim();
            return this;
        }

        /**
         * Полная замена набора запрещённых id (копируется в билдер).
         */
        public Builder accessDenied(Collection<String> ids) {
            this.accessDeniedIds.clear();
            if (ids != null) {
                for (String id : ids) {
                    addAccessDenied(id);
                }
            }
            return this;
        }

        /**
         * Добавляет один id в политику доступа ({@link ModManifest#ID_PATTERN}).
         */
        public Builder addAccessDenied(String id) {
            if (id == null || id.isBlank()) {
                return this;
            }
            String s = id.trim();
            if (!s.matches(ModManifest.ID_PATTERN)) {
                return this;
            }
            accessDeniedIds.add(s);
            return this;
        }

        public BootOptions build() { return new BootOptions(this); }
    }

    /**
     * Преобразует строку, разделённую {@link java.io.File#pathSeparatorChar},
     * в список путей. Пустые сегменты игнорируются.
     */
    public static List<Path> parsePathList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String[] parts = raw.split(java.util.regex.Pattern.quote(
                String.valueOf(java.io.File.pathSeparatorChar)));
        List<Path> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (!p.isBlank()) out.add(Path.of(p));
        }
        return out;
    }
}
