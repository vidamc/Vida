/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.modrinth;

import dev.vida.installer.launchers.InstanceRef;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Чтение Modrinth App {@code app.db} (SQLite).
 *
 * <p>Modrinth App хранит профили в таблице {@code profiles} со столбцами:
 * <ul>
 *   <li>{@code path} — относительный путь к инстансу от data-dir;</li>
 *   <li>{@code name} — display-name;</li>
 *   <li>{@code game_version} — версия Minecraft;</li>
 *   <li>{@code loader} — {@code "fabric"}/{@code "forge"}/{@code "vanilla"};</li>
 *   <li>{@code loader_version} — версия модлоадера;</li>
 *   <li>{@code java_args} — дополнительные аргументы JVM.</li>
 * </ul>
 */
final class ModrinthDbReader {

    private ModrinthDbReader() {}

    /**
     * Перечисляет профили из {@code app.db}.
     *
     * @param dataDir корневой каталог Modrinth App (содержит {@code app.db}
     *                и {@code profiles/})
     */
    static List<InstanceRef> listProfiles(Path dataDir) throws IOException {
        Path db = dataDir.resolve("app.db");
        if (!Files.isRegularFile(db)) {
            return List.of();
        }
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            return readProfiles(conn, dataDir);
        } catch (SQLException e) {
            throw new IOException("Failed to read Modrinth app.db: " + e.getMessage(), e);
        }
    }

    private static List<InstanceRef> readProfiles(Connection conn, Path dataDir)
            throws SQLException {
        List<InstanceRef> result = new ArrayList<>();
        String sql = """
                SELECT path, name, game_version, loader, loader_version
                FROM profiles
                ORDER BY name
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String relPath = rs.getString("path");
                String name = rs.getString("name");
                String mcVer = rs.getString("game_version");
                String loader = rs.getString("loader");
                String loaderVer = rs.getString("loader_version");

                Path instancePath = dataDir.resolve("profiles").resolve(relPath);

                result.add(new InstanceRef(
                        relPath != null ? relPath : name,
                        name != null ? name : relPath,
                        instancePath,
                        mcVer != null ? mcVer : "",
                        Optional.ofNullable(loader).filter(s -> !s.isBlank()),
                        Optional.ofNullable(loaderVer).filter(s -> !s.isBlank())));
            }
        }
        return result;
    }

    /**
     * Обновляет {@code java_args} для указанного профиля, добавляя
     * {@code -javaagent} в существующие аргументы.
     *
     * @return {@code true} если профиль найден и обновлён
     */
    static boolean patchJavaArgs(Path dataDir, String profilePath, String agentArg)
            throws IOException {
        Path db = dataDir.resolve("app.db");
        String url = "jdbc:sqlite:" + db.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            String existing = readJavaArgs(conn, profilePath);
            String newArgs;
            if (existing != null && !existing.isBlank()) {
                if (existing.contains("-javaagent:")) {
                    newArgs = existing.replaceAll("-javaagent:\\S+", agentArg);
                } else {
                    newArgs = existing.trim() + " " + agentArg;
                }
            } else {
                newArgs = agentArg;
            }
            return updateJavaArgs(conn, profilePath, newArgs);
        } catch (SQLException e) {
            throw new IOException("Failed to patch Modrinth app.db: " + e.getMessage(), e);
        }
    }

    private static String readJavaArgs(Connection conn, String profilePath) throws SQLException {
        String sql = "SELECT java_args FROM profiles WHERE path = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profilePath);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("java_args") : null;
            }
        }
    }

    private static boolean updateJavaArgs(Connection conn, String profilePath, String args)
            throws SQLException {
        String sql = "UPDATE profiles SET java_args = ? WHERE path = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, args);
            ps.setString(2, profilePath);
            return ps.executeUpdate() > 0;
        }
    }
}
