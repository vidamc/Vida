/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.modrinth;

import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.mc.JsonTree;
import dev.vida.installer.mc.VidaInstallerJvm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Чтение Modrinth App {@code app.db} (SQLite).
 *
 * <p>Актуальная схема Theseus ({@code modrinth/code}): таблица {@code profiles}
 * с {@code mod_loader}, {@code mod_loader_version},
 * {@code override_extra_launch_args} (JSON-массив строк для JVM).
 *
 * <p>Поддерживается устаревший тестовый набор колонок:
 * {@code loader}, {@code loader_version}, {@code java_args}.
 */
final class ModrinthDbReader {

    private ModrinthDbReader() {}

    static List<InstanceRef> listProfiles(Path dataDir) throws IOException {
        Path db = dataDir.resolve("app.db");
        if (!Files.isRegularFile(db)) {
            return List.of();
        }
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            Schema s = Schema.detect(conn);
            return readProfiles(conn, dataDir, s);
        } catch (SQLException e) {
            throw new IOException("Failed to read Modrinth app.db: " + e.getMessage(), e);
        }
    }

    static boolean patchJavaArgs(Path dataDir, String profilePath, String agentArg,
                                 String minecraftVersion, String loaderVersion)
            throws IOException {
        Path db = dataDir.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            Schema s = Schema.detect(conn);
            Optional<String> resolved = resolveProfilePath(conn, profilePath);
            if (resolved.isEmpty()) {
                return false;
            }
            String pathKey = resolved.get();
            if (s.overrideExtraLaunchArgs) {
                return patchOverrideExtraLaunchArgs(conn, pathKey, agentArg,
                        minecraftVersion, loaderVersion);
            }
            if (s.javaArgs) {
                return patchLegacyJavaArgs(conn, pathKey, agentArg,
                        minecraftVersion, loaderVersion);
            }
            return false;
        } catch (SQLException e) {
            throw new IOException("Failed to patch Modrinth app.db: " + e.getMessage(), e);
        }
    }

    private static boolean patchOverrideExtraLaunchArgs(Connection conn, String pathKey,
                                                        String agentArg,
                                                        String minecraftVersion,
                                                        String loaderVersion) throws SQLException {
        String raw = readSingleString(conn,
                "SELECT override_extra_launch_args FROM profiles WHERE path = ?", pathKey);
        List<Object> list = parseStringArray(raw);
        list = stripVidaInstallerPropsFromJsonArray(list);
        list = mergeAgentIntoJsonArray(list, agentArg);
        for (String t : VidaInstallerJvm.installerJvmPropTokens(minecraftVersion, loaderVersion)) {
            list.add(t);
        }
        String json = JsonTree.write(list);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE profiles SET override_extra_launch_args = ? WHERE path = ?")) {
            ps.setString(1, json);
            ps.setString(2, pathKey);
            return ps.executeUpdate() > 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> parseStringArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        Object parsed = JsonTree.parse(raw);
        if (parsed instanceof List<?> l) {
            return new ArrayList<>((List<Object>) l);
        }
        return new ArrayList<>();
    }

    private static List<Object> stripVidaInstallerPropsFromJsonArray(List<Object> existing) {
        List<Object> out = new ArrayList<>();
        for (Object o : existing) {
            if (o instanceof String s && VidaInstallerJvm.isManagedVidaInstallerJvmToken(s)) {
                continue;
            }
            out.add(o);
        }
        return out;
    }

    private static List<Object> mergeAgentIntoJsonArray(List<Object> existing, String agentArg) {
        List<Object> tail = new ArrayList<>();
        for (Object o : existing) {
            if (!(o instanceof String s)) {
                tail.add(o);
                continue;
            }
            if (isOurVidaAgentArg(s)) {
                continue;
            }
            tail.add(s);
        }
        List<Object> out = new ArrayList<>();
        out.add(agentArg);
        out.addAll(tail);
        return out;
    }

    private static boolean isOurVidaAgentArg(String s) {
        if (!s.startsWith("-javaagent:")) {
            return false;
        }
        String p = s.substring("-javaagent:".length()).toLowerCase(Locale.ROOT);
        return p.contains("vida-loader") || p.contains("/vida/") || p.contains("\\vida\\");
    }

    private static boolean patchLegacyJavaArgs(Connection conn, String pathKey, String agentArg,
                                               String minecraftVersion, String loaderVersion)
            throws SQLException {
        String existing = readSingleString(conn, "SELECT java_args FROM profiles WHERE path = ?",
                pathKey);
        String stripped = VidaInstallerJvm.stripManagedInstallerJvmTokens(
                existing != null ? existing : "");
        String newArgs;
        if (!stripped.isBlank()) {
            if (stripped.contains("-javaagent:")) {
                newArgs = stripped.replaceAll("-javaagent:\\S+",
                        Matcher.quoteReplacement(agentArg));
            } else {
                newArgs = stripped.trim() + " " + agentArg;
            }
        } else {
            newArgs = agentArg;
        }
        newArgs = newArgs.trim() + " "
                + VidaInstallerJvm.spaceSeparatedInstallerJvmProps(minecraftVersion, loaderVersion);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE profiles SET java_args = ? WHERE path = ?")) {
            ps.setString(1, newArgs);
            ps.setString(2, pathKey);
            return ps.executeUpdate() > 0;
        }
    }

    private static String readSingleString(Connection conn, String sql, String pathKey)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pathKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static Optional<String> resolveProfilePath(Connection conn, String profilePath)
            throws SQLException {
        if (profilePath == null || profilePath.isBlank()) {
            return Optional.empty();
        }
        if (rowExists(conn, profilePath)) {
            return Optional.of(profilePath);
        }
        String normalized = profilePath.replace('\\', '/');
        try (PreparedStatement ps = conn.prepareStatement("SELECT path FROM profiles");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String p = rs.getString(1);
                if (p == null) continue;
                String pn = p.replace('\\', '/');
                if (normalized.equals(pn)
                        || normalized.endsWith("/" + pn)
                        || pn.endsWith("/" + normalized)) {
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean rowExists(Connection conn, String path) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM profiles WHERE path = ?")) {
            ps.setString(1, path);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static List<InstanceRef> readProfiles(Connection conn, Path dataDir, Schema s)
            throws SQLException {
        String sql = String.format(Locale.ROOT, """
                SELECT path, name, game_version, %s, %s
                FROM profiles
                ORDER BY name
                """, s.loaderColumn, s.loaderVersionColumn);

        List<InstanceRef> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String relPath = rs.getString("path");
                String name = rs.getString("name");
                String mcVer = rs.getString("game_version");
                String loader = rs.getString(s.loaderColumn);
                String loaderVer = rs.getString(s.loaderVersionColumn);

                Path instancePath = dataDir.resolve("profiles").resolve(relPath);

                result.add(new InstanceRef(
                        relPath != null ? relPath : name,
                        name != null ? name : relPath,
                        instancePath,
                        mcVer != null ? mcVer : "",
                        Optional.ofNullable(loader).filter(x -> !x.isBlank()),
                        Optional.ofNullable(loaderVer).filter(x -> !x.isBlank())));
            }
        }
        return result;
    }

    private static final class Schema {
        final String loaderColumn;
        final String loaderVersionColumn;
        final boolean overrideExtraLaunchArgs;
        final boolean javaArgs;

        Schema(String loaderColumn, String loaderVersionColumn,
               boolean overrideExtraLaunchArgs, boolean javaArgs) {
            this.loaderColumn = loaderColumn;
            this.loaderVersionColumn = loaderVersionColumn;
            this.overrideExtraLaunchArgs = overrideExtraLaunchArgs;
            this.javaArgs = javaArgs;
        }

        static Schema detect(Connection conn) throws SQLException {
            Set<String> cols = columnNames(conn, "profiles");
            String lc = cols.contains("mod_loader") ? "mod_loader"
                    : cols.contains("loader") ? "loader"
                    : null;
            String lvc = cols.contains("mod_loader_version") ? "mod_loader_version"
                    : cols.contains("loader_version") ? "loader_version"
                    : null;
            if (lc == null || lvc == null) {
                throw new SQLException("profiles table missing loader columns");
            }
            boolean overrideArgs = cols.contains("override_extra_launch_args");
            boolean javaArgsCol = cols.contains("java_args");
            return new Schema(lc, lvc, overrideArgs, javaArgsCol);
        }

        private static Set<String> columnNames(Connection conn, String table) throws SQLException {
            Set<String> out = new HashSet<>();
            try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("name").toLowerCase(Locale.ROOT));
                }
            }
            return out;
        }
    }
}
