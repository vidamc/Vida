/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Кросс-платформенный инсталлятор Vida.
 *
 * <p>Артефакт — single-jar, запускаемый как {@code java -jar vida-installer-X.Y.Z.jar}.
 * По умолчанию открывается Swing-GUI; с флагом {@code --headless} работает
 * как CLI (используется в CI, скриптах и на серверах).
 *
 * <p><b>Важно:</b> fat-jar именуется без {@code -all}-суффикса начиная с 0.3.0 —
 * теперь это единственный «клик-руннабельный» артефакт модуля. Тонкий jar
 * (Gradle-дефолт) получает classifier {@code -thin} и не имеет
 * {@code Main-Class}; он оставлен только для нужд распространения через
 * Maven/Ivy координаты и не предназначен для двойного клика.
 *
 * <h2>Что делает инсталлятор</h2>
 *
 * <ol>
 *   <li>Проверяет, что текущая JVM ≥ 21;</li>
 *   <li>определяет каталог установки (по умолчанию — каталог Minecraft
 *       для текущей ОС, с возможностью переопределить);</li>
 *   <li>распаковывает из собственных ресурсов {@code loader.jar} в
 *       {@code <install-dir>/vida/loader/vida-loader-<ver>.jar};</li>
 *   <li>создаёт подкаталоги {@code mods/}, {@code config/}, {@code logs/};</li>
 *   <li>пишет launch-script ({@code vida.bat} на Windows, {@code vida.sh}
 *       на *nix) со всеми нужными флагами {@code -javaagent};</li>
 *   <li>сохраняет отчёт об установке в {@code install.json}, чтобы можно
 *       было откатить установку.</li>
 * </ol>
 *
 * <h2>Структура пакета</h2>
 *
 * <ul>
 *   <li>{@link dev.vida.installer.InstallerMain} — точка входа, выбор GUI/CLI;</li>
 *   <li>{@link dev.vida.installer.InstallerCore} — чистая логика установки,
 *       не зависящая от UI;</li>
 *   <li>{@link dev.vida.installer.cli} — CLI-режим;</li>
 *   <li>{@link dev.vida.installer.gui} — Swing-GUI.</li>
 * </ul>
 */
package dev.vida.installer;
