/*
 * Copyright 2026 Burrow Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.mjoe.burrow.gui;

import io.github.mjoe.burrow.core.ssh.TunnelListener;
import io.github.mjoe.burrow.core.ssh.TunnelManager;

import javax.swing.*;
import java.awt.*;

/**
 * Main application window with tabbed interface.
 */
public final class MainWindow extends JFrame implements TunnelListener {
    private final TunnelManager tunnelManager;
    private final JTabbedPane tabbedPane;
    private final StatusBar statusBar;

    public MainWindow(TunnelManager tunnelManager) {
        super("Burrow - SSH Tunnel Manager");
        this.tunnelManager = tunnelManager;
        this.tunnelManager.addListener(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 600));
        setPreferredSize(new Dimension(1000, 700));

        // Create components
        tabbedPane = new JTabbedPane();
        statusBar = new StatusBar();

        // Create tabs
        tabbedPane.addTab("Identities", new IdentityPanel(tunnelManager));
        tabbedPane.addTab("Connections", new ConnectionPanel(tunnelManager));
        tabbedPane.addTab("Forwards", new ForwardPanel(tunnelManager));

        // Layout
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // Menu bar
        setJMenuBar(createMenuBar());

        // Toolbar
        add(createToolBar(), BorderLayout.NORTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        var menuBar = new JMenuBar();

        // File menu
        var fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        var loadItem = new JMenuItem("Load Configuration...");
        loadItem.setAccelerator(KeyStroke.getKeyStroke("meta O"));
        loadItem.addActionListener(e -> loadConfiguration());
        fileMenu.add(loadItem);

        var saveItem = new JMenuItem("Save Configuration");
        saveItem.setAccelerator(KeyStroke.getKeyStroke("meta S"));
        saveItem.addActionListener(e -> saveConfiguration());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        var exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke("meta Q"));
        exitItem.addActionListener(e -> {
            tunnelManager.shutdown();
            System.exit(0);
        });
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // Tunnel menu
        var tunnelMenu = new JMenu("Tunnel");
        tunnelMenu.setMnemonic('T');

        var connectAllItem = new JMenuItem("Connect All");
        connectAllItem.addActionListener(e -> tunnelManager.connectAll());
        tunnelMenu.add(connectAllItem);

        var disconnectAllItem = new JMenuItem("Disconnect All");
        disconnectAllItem.addActionListener(e -> tunnelManager.disconnectAll());
        tunnelMenu.add(disconnectAllItem);

        menuBar.add(tunnelMenu);

        // Help menu
        var helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');

        var aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        var toolBar = new JToolBar();
        toolBar.setFloatable(false);

        var connectAllBtn = createToolButton("Connect All", "▶", e -> tunnelManager.connectAll());
        var disconnectAllBtn = createToolButton("Disconnect All", "⏹", e -> tunnelManager.disconnectAll());
        var refreshBtn = createToolButton("Refresh", "↻", e -> refreshData());

        toolBar.add(connectAllBtn);
        toolBar.add(disconnectAllBtn);
        toolBar.addSeparator();
        toolBar.add(refreshBtn);

        return toolBar;
    }

    private JButton createToolButton(String tooltip, String text, java.awt.event.ActionListener action) {
        var button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(action);
        button.setFocusPainted(false);
        return button;
    }

    public void refreshData() {
        // Refresh all panels
        for (var i = 0; i < tabbedPane.getTabCount(); i++) {
            var component = tabbedPane.getComponentAt(i);
            if (component instanceof Refreshable refreshable) {
                refreshable.refresh();
            }
        }
    }

    private void loadConfiguration() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle("Load Configuration");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("YAML files", "yaml", "yml"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                tunnelManager.loadConfiguration(chooser.getSelectedFile().toPath());
                refreshData();
                statusBar.showMessage("Configuration loaded");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Failed to load configuration: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveConfiguration() {
        try {
            tunnelManager.saveConfiguration();
            statusBar.showMessage("Configuration saved");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save configuration: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Burrow - SSH Tunnel Manager\n\n" +
                "Version: " + getClass().getPackage().getImplementationVersion() + "\n\n" +
                "A modern SSH tunnel management tool.",
                "About Burrow",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // TunnelListener implementation

    @Override
    public void onConnecting(String connectionAlias) {
        SwingUtilities.invokeLater(() -> statusBar.showMessage("Connecting to " + connectionAlias + "..."));
    }

    @Override
    public void onConnected(String connectionAlias) {
        SwingUtilities.invokeLater(() -> statusBar.showMessage("Connected to " + connectionAlias));
    }

    @Override
    public void onDisconnected(String connectionAlias, String reason) {
        SwingUtilities.invokeLater(() -> statusBar.showMessage("Disconnected from " + connectionAlias));
    }

    @Override
    public void onFailed(String connectionAlias, String reason) {
        SwingUtilities.invokeLater(() -> statusBar.showMessage("Failed to connect to " + connectionAlias + ": " + reason));
    }

    @Override
    public void onReconnecting(String connectionAlias, int attempt) {
        SwingUtilities.invokeLater(() -> statusBar.showMessage("Reconnecting to " + connectionAlias + " (attempt " + attempt + ")..."));
    }

    @Override
    public void onForwardEstablished(String forwardAlias, String type) {
        SwingUtilities.invokeLater(() -> statusBar.showMessage(type + " forward " + forwardAlias + " established"));
    }

    @Override
    public void onForwardClosed(String forwardAlias, String type) {
        SwingUtilities.invokeLater(() -> statusBar.showMessage(type + " forward " + forwardAlias + " closed"));
    }
}
