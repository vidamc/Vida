/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FingerprintsTest {

    /** SHA-256 от "" — хорошо известный вектор. */
    private static final String EMPTY_HEX =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    /** SHA-256 от "abc". */
    private static final String ABC_HEX =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    void emptyBytesHaveKnownDigest() {
        assertThat(Fingerprints.hex(Fingerprints.sha256(new byte[0]))).isEqualTo(EMPTY_HEX);
    }

    @Test
    void abcBytesHaveKnownDigest() {
        byte[] data = "abc".getBytes(StandardCharsets.US_ASCII);
        assertThat(Fingerprints.hex(Fingerprints.sha256(data))).isEqualTo(ABC_HEX);
    }

    @Test
    void digestLengthIs32(@TempDir Path tmp) throws IOException {
        Path f = Files.writeString(tmp.resolve("a.txt"), "hello");
        byte[] d = Fingerprints.sha256(f);
        assertThat(d).hasSize(Fingerprints.SHA256_LENGTH);
    }

    @Test
    void fileAndByteDigestsMatch(@TempDir Path tmp) throws IOException {
        byte[] data = "vida discovery test".getBytes(StandardCharsets.UTF_8);
        Path f = Files.write(tmp.resolve("a.bin"), data);

        String fileHex = Fingerprints.hex(Fingerprints.sha256(f));
        String byteHex = Fingerprints.hex(Fingerprints.sha256(data));
        assertThat(fileHex).isEqualTo(byteHex);
    }

    @Test
    void hexIsLowercase() {
        byte[] d = Fingerprints.sha256("x".getBytes(StandardCharsets.UTF_8));
        assertThat(Fingerprints.hex(d)).matches("[0-9a-f]{64}");
    }
}
