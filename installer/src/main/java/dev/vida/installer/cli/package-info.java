/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Headless-режим инсталлятора — используется в CI и на серверах.
 *
 * <p>Точка входа — {@link dev.vida.installer.cli.CliInstaller}, вызывается
 * из {@link dev.vida.installer.InstallerMain} при флаге {@code --headless}
 * либо автоматически, если нет GUI-окружения.
 */
package dev.vida.installer.cli;
