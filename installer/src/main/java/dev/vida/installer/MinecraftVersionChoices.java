/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import java.util.ArrayList;
import java.util.List;

/**
 * Предустановленный список версий игры для GUI (можно также ввести свою при
 * {@link javax.swing.JComboBox#setEditable(boolean)}).
 */
public final class MinecraftVersionChoices {

    private MinecraftVersionChoices() {}

    /** 1.21.1–1.21.24, календарная линия 26.1.x и превью. */
    public static String[] defaultComboItems() {
        List<String> list = new ArrayList<>();
        for (int p = 1; p <= 24; p++) {
            list.add("1.21." + p);
        }
        list.add("26.1.0");
        list.add("26.1.1");
        list.add("26.1.2");
        list.add("26.1.preview");
        return list.toArray(String[]::new);
    }
}
