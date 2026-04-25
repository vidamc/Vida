/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class McArtifactsDownloadHttpTest {

    private static final byte[] BODY = "Hello, resumable download.".getBytes(UTF_8);

    @Test
    void download_full_without_partial(@TempDir Path tmp) throws Exception {
        HttpServer server = startServer();
        try {
            Path out = tmp.resolve("out.bin");
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/blob");
            long n = McArtifacts.downloadHttpResumable(uri, out);
            assertThat(n).isEqualTo(BODY.length);
            assertThat(Files.readAllBytes(out)).isEqualTo(BODY);
            assertThat(tmp.resolve("out.bin.part")).doesNotExist();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resume_appends_after_partial(@TempDir Path tmp) throws Exception {
        HttpServer server = startServer();
        try {
            Path out = tmp.resolve("out.bin");
            Path part = tmp.resolve("out.bin.part");
            Files.write(part, Arrays.copyOfRange(BODY, 0, Math.min(5, BODY.length)));
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/blob");
            long n = McArtifacts.downloadHttpResumable(uri, out);
            assertThat(n).isEqualTo(BODY.length);
            assertThat(Files.readAllBytes(out)).isEqualTo(BODY);
            assertThat(part).doesNotExist();
        } finally {
            server.stop(0);
        }
    }

    /**
     * When {@code ignoreRangeOnSecondCall}, first GET uses Range and gets HTTP 200 full body — client
     * deletes partial and retries without Range (integration with resume logic).
     */
    @Test
    void http_ok_with_existing_partial_retries_full(@TempDir Path tmp) throws Exception {
        HttpServer server = startServerIgnoringRangeOnce();
        try {
            Path out = tmp.resolve("out.bin");
            Path part = tmp.resolve("out.bin.part");
            Files.write(part, new byte[] {0, 1, 2}); // junk partial
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/blob");
            long n = McArtifacts.downloadHttpResumable(uri, out);
            assertThat(n).isEqualTo(BODY.length);
            assertThat(Files.readAllBytes(out)).isEqualTo(BODY);
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/blob", exchange -> {
            try {
                String range = exchange.getRequestHeaders().getFirst("Range");
                exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                if (range != null && range.startsWith("bytes=")) {
                    int eq = range.indexOf('=');
                    int dash = range.indexOf('-', eq + 1);
                    long start = Long.parseLong(range.substring(eq + 1, dash));
                    int from = (int) start;
                    if (from < 0 || from > BODY.length) {
                        exchange.sendResponseHeaders(416, -1);
                        return;
                    }
                    int len = BODY.length - from;
                    exchange.sendResponseHeaders(206, len);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(BODY, from, len);
                    }
                } else {
                    exchange.sendResponseHeaders(200, BODY.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(BODY);
                    }
                }
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    /** First ranged request → HTTP 200 full (ignore Range); second → normal. */
    private static HttpServer startServerIgnoringRangeOnce() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        boolean[] firstRange = {true};
        server.createContext("/blob", exchange -> {
            try {
                String range = exchange.getRequestHeaders().getFirst("Range");
                exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                if (range != null && range.startsWith("bytes=") && firstRange[0]) {
                    firstRange[0] = false;
                    exchange.sendResponseHeaders(200, BODY.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(BODY);
                    }
                } else if (range != null && range.startsWith("bytes=")) {
                    int eq = range.indexOf('=');
                    int dash = range.indexOf('-', eq + 1);
                    long start = Long.parseLong(range.substring(eq + 1, dash));
                    int from = (int) start;
                    int len = BODY.length - from;
                    exchange.sendResponseHeaders(206, len);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(BODY, from, len);
                    }
                } else {
                    exchange.sendResponseHeaders(200, BODY.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(BODY);
                    }
                }
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }
}
