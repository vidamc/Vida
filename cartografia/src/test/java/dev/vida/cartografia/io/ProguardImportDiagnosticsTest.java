/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import dev.vida.core.Result;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class ProguardImportDiagnosticsTest {

    @Test
    void reportsExternalTypeInDescriptorNotMappedInPass1() {
        String pg = """
                com.app.A -> a:
                    com.ext.B field -> x
                """;
        Result<ProguardImportDiagnostics, dev.vida.cartografia.MappingError> r =
                ProguardReader.readWithDiagnostics(
                        "t.txt", new StringReader(pg), Namespace.OBF, Namespace.NAMED);
        assertThat(r.isOk()).isTrue();
        ProguardImportDiagnostics d = r.unwrap();
        MappingTree tree = d.tree();
        assertThat(tree.size()).isEqualTo(1);
        assertThat(d.unresolvedInternalClassRefsInDescriptors()).contains("com/ext/B");
    }
}
