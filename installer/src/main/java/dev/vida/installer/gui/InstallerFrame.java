/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.gui;

import dev.vida.installer.InstallOptions;
import dev.vida.installer.InstallReport;
import dev.vida.installer.InstallerCore;
import dev.vida.installer.JavaRuntimeCheck;
import dev.vida.installer.McDirDetector;
import dev.vida.installer.launchers.InstallMode;
import dev.vida.installer.launchers.InstanceRef;
import dev.vida.installer.launchers.LauncherHandler;
import dev.vida.installer.launchers.LauncherKind;
import dev.vida.installer.launchers.LauncherRegistry;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

/**
 * Главное окно GUI-инсталлятора.
 *
 * <p>Содержит:
 * <ul>
 *   <li>dropdown выбора лаунчера (Mojang / Prism / MultiMC / ATLauncher);</li>
 *   <li>data-dir (авто-детект + Browse);</li>
 *   <li>для ATLauncher — dropdown выбора instance'а (с кнопкой Refresh);</li>
 *   <li>для Prism/MultiMC — поле instance-name;</li>
 *   <li>checkbox'ы (Mojang-only для launcher-profile / launch-script);</li>
 *   <li>прогресс-бар и лог.</li>
 * </ul>
 */
public final class InstallerFrame extends JFrame {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String loaderVersion;
    private final JComboBox<LauncherKind> launcherBox;
    private final JTextField dirField;
    private final JTextField mcField;
    private final JTextField instanceNameField;
    private final JComboBox<InstanceItem> instanceBox;
    private final JButton refreshInstancesBtn;
    private final JLabel instanceLabel;
    private final JLabel instanceNameLabel;
    private final JCheckBox launcherProfileBox;
    private final JCheckBox launchScriptBox;
    private final JCheckBox overwriteBox;
    private final JCheckBox dryRunBox;
    private final JButton installButton;
    private final JProgressBar progress;
    private final JTextArea log;

    public InstallerFrame(String loaderVersion) {
        super("Vida Installer");
        this.loaderVersion = loaderVersion;

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignore) { /* оставляем metal */ }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(780, 600));
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        // Launcher
        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Launcher:"), c);
        c.gridx = 1; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        launcherBox = new JComboBox<>(implementedLaunchers());
        launcherBox.setRenderer(new LauncherRenderer());
        form.add(launcherBox, c);
        c.gridwidth = 1;

        // Dir
        c.gridx = 0; c.gridy = 1; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        form.add(new JLabel("Data directory:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        dirField = new JTextField(new McDirDetector().defaultDir().toString(), 40);
        form.add(dirField, c);
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> browseForDir());
        form.add(browse, c);

        // Minecraft version
        c.gridx = 0; c.gridy = 2;
        form.add(new JLabel("Minecraft version:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        mcField = new JTextField("1.21.1", 10);
        form.add(mcField, c);

        // Instance name (Prism/MultiMC)
        c.gridx = 0; c.gridy = 3; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        instanceNameLabel = new JLabel("Instance name:");
        form.add(instanceNameLabel, c);
        c.gridx = 1; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        instanceNameField = new JTextField("", 30);
        instanceNameField.setToolTipText("Leave empty for auto-generated name (vida-<mcVer>-<loaderVer>)");
        form.add(instanceNameField, c);
        c.gridwidth = 1;

        // Target instance (ATLauncher)
        c.gridx = 0; c.gridy = 4; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        instanceLabel = new JLabel("Target instance:");
        form.add(instanceLabel, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        instanceBox = new JComboBox<>();
        form.add(instanceBox, c);
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        refreshInstancesBtn = new JButton("Refresh");
        refreshInstancesBtn.addActionListener(e -> refreshInstances());
        form.add(refreshInstancesBtn, c);

        // Checkboxes
        c.gridx = 0; c.gridy = 5; c.gridwidth = 3;
        JPanel checks = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        launcherProfileBox = new JCheckBox("Add Minecraft launcher profile", true);
        launchScriptBox    = new JCheckBox("Create standalone launch script", true);
        overwriteBox       = new JCheckBox("Overwrite existing install", false);
        dryRunBox          = new JCheckBox("Dry run (don't write files)", false);
        checks.add(launcherProfileBox);
        checks.add(launchScriptBox);
        checks.add(overwriteBox);
        checks.add(dryRunBox);
        form.add(checks, c);

        add(form, BorderLayout.NORTH);

        // ---- центр: лог ---------------------------------------------------
        log = new JTextArea();
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(false);
        log.setForeground(Color.DARK_GRAY);
        log.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        add(new JScrollPane(log), BorderLayout.CENTER);

        // ---- низ: прогресс + кнопки --------------------------------------
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));

        progress = new JProgressBar();
        progress.setIndeterminate(false);
        progress.setStringPainted(true);
        progress.setString("ready");
        south.add(progress);
        south.add(Box.createVerticalStrut(6));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancel = new JButton("Close");
        cancel.addActionListener(e -> dispose());
        installButton = new JButton("Install");
        installButton.addActionListener(e -> startInstall());
        buttons.add(cancel);
        buttons.add(installButton);
        south.add(buttons);

        add(south, BorderLayout.SOUTH);

        launcherBox.addActionListener(e -> onLauncherChanged());
        onLauncherChanged();

        JavaRuntimeCheck.Result jre = JavaRuntimeCheck.checkCurrent();
        appendLog("[runtime] " + jre.message());
        if (!jre.ok()) {
            appendLog("[runtime] Vida will NOT start on this JVM, but installation will proceed.");
        }
        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    //  Launcher dropdown plumbing
    // ------------------------------------------------------------------

    private static LauncherKind[] implementedLaunchers() {
        List<LauncherKind> out = new ArrayList<>();
        for (LauncherKind k : LauncherKind.values()) {
            if (k.isImplemented()) out.add(k);
        }
        return out.toArray(new LauncherKind[0]);
    }

    private LauncherKind currentLauncher() {
        return (LauncherKind) launcherBox.getSelectedItem();
    }

    private void onLauncherChanged() {
        LauncherKind k = currentLauncher();
        LauncherHandler h = LauncherRegistry.forKind(k);

        // Авто-детект data-dir.
        List<Path> detected = h.detectDataDirs();
        if (!detected.isEmpty()) {
            dirField.setText(detected.get(0).toString());
        } else if (k == LauncherKind.MOJANG) {
            dirField.setText(new McDirDetector().defaultDir().toString());
        } else {
            dirField.setText("");
        }

        boolean mojang = k == LauncherKind.MOJANG;
        boolean prismLike = k == LauncherKind.PRISM || k == LauncherKind.MULTIMC;
        boolean patches = h.supportedModes().contains(InstallMode.PATCH_EXISTING_INSTANCE)
                && h.defaultMode() == InstallMode.PATCH_EXISTING_INSTANCE;

        launcherProfileBox.setVisible(mojang);
        launchScriptBox.setVisible(mojang);

        instanceNameLabel.setVisible(prismLike);
        instanceNameField.setVisible(prismLike);

        instanceLabel.setVisible(patches);
        instanceBox.setVisible(patches);
        refreshInstancesBtn.setVisible(patches);

        if (patches) refreshInstances();

        revalidate();
        repaint();
    }

    private void refreshInstances() {
        LauncherKind k = currentLauncher();
        LauncherHandler h = LauncherRegistry.forKind(k);
        Path dir = Path.of(dirField.getText().trim());
        DefaultComboBoxModel<InstanceItem> model = new DefaultComboBoxModel<>();
        try {
            List<InstanceRef> refs = h.listInstances(dir);
            if (refs.isEmpty()) {
                model.addElement(new InstanceItem(null, "(no instances found in " + dir + ")"));
            } else {
                Collections.sort(refs,
                        (a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
                for (InstanceRef r : refs) {
                    String label = r.displayName() + "  —  mc=" + safeMc(r.minecraftVersion())
                            + r.loader().map(l -> "  " + l + r.loaderVersion().map(v -> "=" + v).orElse(""))
                                    .orElse("");
                    model.addElement(new InstanceItem(r.instancePath(), label));
                }
            }
        } catch (Exception ex) {
            model.addElement(new InstanceItem(null, "error: " + ex.getMessage()));
        }
        instanceBox.setModel(model);
    }

    private static String safeMc(String v) { return v == null || v.isEmpty() ? "?" : v; }

    private void browseForDir() {
        JFileChooser fc = new JFileChooser(new File(dirField.getText()));
        fc.setDialogTitle("Choose install directory");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            dirField.setText(fc.getSelectedFile().getAbsolutePath());
            onLauncherChanged();
        }
    }

    // ------------------------------------------------------------------
    //  Install
    // ------------------------------------------------------------------

    private void startInstall() {
        InstallOptions opt;
        try {
            LauncherKind k = currentLauncher();
            Path targetInstance = null;
            if (instanceBox.isVisible()) {
                InstanceItem it = (InstanceItem) instanceBox.getSelectedItem();
                if (it == null || it.path() == null) {
                    JOptionPane.showMessageDialog(this,
                            "Please pick an instance (or Refresh).",
                            "Vida Installer", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                targetInstance = it.path();
            }
            String instanceName = instanceNameField.isVisible() && !instanceNameField.getText().isBlank()
                    ? instanceNameField.getText().trim()
                    : null;
            opt = InstallOptions.builder()
                    .launcherKind(k)
                    .installDir(Path.of(dirField.getText().trim()))
                    .targetInstance(targetInstance)
                    .instanceName(instanceName)
                    .minecraftVersion(mcField.getText().trim())
                    .loaderVersion(loaderVersion)
                    .createLauncherProfile(launcherProfileBox.isSelected())
                    .createLaunchScript(launchScriptBox.isSelected())
                    .overwrite(overwriteBox.isSelected())
                    .dryRun(dryRunBox.isSelected())
                    .build();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Invalid options: " + e.getMessage(),
                    "Vida Installer", JOptionPane.ERROR_MESSAGE);
            return;
        }

        installButton.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setString("installing…");
        appendLog("---- Starting install (" + opt.launcherKind().displayName() + ") ----");

        SwingWorker<InstallReport, String> worker = new SwingWorker<>() {
            @Override
            protected InstallReport doInBackground() {
                InstallerCore core = new InstallerCore(this::publish);
                return core.install(opt);
            }

            @Override
            protected void process(List<String> chunks) {
                for (String m : chunks) appendLog(m);
            }

            @Override
            protected void done() {
                progress.setIndeterminate(false);
                installButton.setEnabled(true);
                try {
                    InstallReport r = get();
                    for (String w : r.warnings()) appendLog("[warn] " + w);
                    if (r.isOk()) {
                        progress.setValue(100);
                        progress.setString("done");
                        appendLog("---- OK: " + r.installedFiles().size() + " files written ----");
                        JOptionPane.showMessageDialog(InstallerFrame.this,
                                "Vida installed for " + r.options().launcherKind().displayName()
                                        + ".\nTarget: " + r.options().installDir(),
                                "Vida Installer", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        progress.setValue(0);
                        progress.setString("failed");
                        for (String err : r.errors()) appendLog("ERROR: " + err);
                        JOptionPane.showMessageDialog(InstallerFrame.this,
                                "Install failed — see log for details.",
                                "Vida Installer", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    progress.setString("failed");
                    appendLog("UNEXPECTED: " + ex);
                }
            }
        };
        worker.execute();
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            log.append(line);
            log.append("\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    // ------------------------------------------------------------------
    //  dropdown value types
    // ------------------------------------------------------------------

    private record InstanceItem(Path path, String label) {
        @Override public String toString() { return label; }
    }

    private static final class LauncherRenderer
            extends javax.swing.DefaultListCellRenderer {
        @java.io.Serial
        private static final long serialVersionUID = 1L;
        @Override
        public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                                                               Object value,
                                                               int index,
                                                               boolean isSelected,
                                                               boolean cellHasFocus) {
            java.awt.Component c = super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
            if (value instanceof LauncherKind k) setText(k.displayName());
            return c;
        }
    }
}
