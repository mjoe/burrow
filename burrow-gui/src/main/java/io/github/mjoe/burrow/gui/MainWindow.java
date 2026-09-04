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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Main application window with tabbed interface and system tray integration.
 */
public final class MainWindow extends JFrame implements TunnelListener {
    private final TunnelManager tunnelManager;
    private final JTabbedPane tabbedPane;
    private final StatusBar statusBar;
    private TrayIcon trayIcon;

    public MainWindow(TunnelManager tunnelManager) {
        super("Burrow - SSH Tunnel Manager");
        this.tunnelManager = tunnelManager;
        this.tunnelManager.addListener(this);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                minimizeToTray();
            }
        });
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

        // System tray
        initSystemTray();
    }

    private void initSystemTray() {
        if (!SystemTray.isSupported()) {
            return;
        }

        var tray = SystemTray.getSystemTray();

        // Tray popup menu
        var popup = new PopupMenu();

        var showItem = new MenuItem("Show Burrow");
        showItem.addActionListener(e -> showFromTray());
        popup.add(showItem);

        popup.addSeparator();

        var connectAllItem = new MenuItem("Connect All");
        connectAllItem.addActionListener(e -> tunnelManager.connectAll());
        popup.add(connectAllItem);

        var disconnectAllItem = new MenuItem("Disconnect All");
        disconnectAllItem.addActionListener(e -> tunnelManager.disconnectAll());
        popup.add(disconnectAllItem);

        popup.addSeparator();

        var exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> exitApplication());
        popup.add(exitItem);

        // Tray icon
        var trayImage = createTrayImage();
        trayIcon = new TrayIcon(trayImage, "Burrow - SSH Tunnel Manager", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Burrow - SSH Tunnel Manager");

        // Double-click to show
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showFromTray();
                }
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            // Tray not available
        }
    }

    private Image createTrayImage() {
        // Create a simple colored icon as tray image
        var size = 16;
        var image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g2d = image.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Green circle
        g2d.setColor(new Color(0x4CAF50));
        g2d.fillOval(1, 1, size - 2, size - 2);

        // Letter "B"
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        var fm = g2d.getFontMetrics();
        var text = "B";
        var x = (size - fm.stringWidth(text)) / 2;
        var y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(text, x, y);
        g2d.dispose();

        return image;
    }

    private void minimizeToTray() {
        if (trayIcon != null) {
            setVisible(false);
            trayIcon.displayMessage("Burrow", "Minimized to system tray", TrayIcon.MessageType.INFO);
        } else {
            exitApplication();
        }
    }

    private void showFromTray() {
        setVisible(true);
        setState(JFrame.NORMAL);
        toFront();
        requestFocus();
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
        exitItem.addActionListener(e -> exitApplication());
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

    private void exitApplication() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        tunnelManager.shutdown();
        dispose();
        System.exit(0);
    }

    public void refreshData() {
        for (var i = 0; i < tabbedPane.getTabCount(); i++) {
            var component = tabbedPane.getComponentAt(i);
            if (component instanceof Refreshable refreshable) {
                refreshable.refresh();
            }
        }
    }

    private void updateTrayTooltip(String text) {
        if (trayIcon != null) {
            trayIcon.setToolTip(text);
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
        SwingUtilities.invokeLater(() -> {
            statusBar.showMessage("Connecting to " + connectionAlias + "...");
            updateTrayTooltip("Connecting to " + connectionAlias + "...");
        });
    }

    @Override
    public void onConnected(String connectionAlias) {
        SwingUtilities.invokeLater(() -> {
            statusBar.showMessage("Connected to " + connectionAlias);
            updateTrayTooltip("Connected to " + connectionAlias);
        });
    }

    @Override
    public void onDisconnected(String connectionAlias, String reason) {
        SwingUtilities.invokeLater(() -> {
            statusBar.showMessage("Disconnected from " + connectionAlias);
            updateTrayTooltip("Disconnected from " + connectionAlias);
        });
    }

    @Override
    public void onFailed(String connectionAlias, String reason) {
        SwingUtilities.invokeLater(() -> {
            statusBar.showMessage("Failed to connect to " + connectionAlias + ": " + reason);
            updateTrayTooltip("Failed: " + connectionAlias);
        });
    }

    @Override
    public void onReconnecting(String connectionAlias, int attempt) {
        SwingUtilities.invokeLater(() -> {
            statusBar.showMessage("Reconnecting to " + connectionAlias + " (attempt " + attempt + ")...");
            updateTrayTooltip("Reconnecting to " + connectionAlias + "...");
        });
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
