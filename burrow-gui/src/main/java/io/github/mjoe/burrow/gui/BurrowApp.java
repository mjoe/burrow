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

import io.github.mjoe.burrow.core.ssh.TunnelManager;
import io.github.mjoe.burrow.gui.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * Main application entry point for Burrow.
 */
public final class BurrowApp {
    private static final Logger log = LoggerFactory.getLogger(BurrowApp.class);
    private static final String APP_NAME = "Burrow";

    private final TunnelManager tunnelManager;
    private final ThemeManager themeManager;
    private MainWindow mainWindow;

    public BurrowApp() {
        this.tunnelManager = new TunnelManager();
        this.themeManager = new ThemeManager();
    }

    public void start() {
        log.info("Starting {} v{}", APP_NAME, getVersion());

        // Set up theme
        themeManager.applyTheme();

        // Create and show main window
        SwingUtilities.invokeLater(() -> {
            mainWindow = new MainWindow(tunnelManager);
            mainWindow.setVisible(true);

            // Load default config
            loadDefaultConfig();
        });
    }

    private void loadDefaultConfig() {
        var configPath = getConfigPath();
        try {
            tunnelManager.loadConfiguration(configPath);
            mainWindow.refreshData();
            log.info("Default configuration loaded from {}", configPath);
        } catch (Exception e) {
            log.warn("Could not load default config: {}", e.getMessage());
        }
    }

    private Path getConfigPath() {
        var home = Path.of(System.getProperty("user.home"));
        var configDir = home.resolve(".burrow");
        return configDir.resolve("config.yaml");
    }

    private String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    public void shutdown() {
        log.info("Shutting down {}", APP_NAME);
        tunnelManager.shutdown();
        if (mainWindow != null) {
            mainWindow.dispose();
        }
    }

    public static void main(String[] args) {
        // Set up exception handling
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            log.error("Uncaught exception in thread {}", thread.getName(), throwable);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "An unexpected error occurred: " + throwable.getMessage(),
                        APP_NAME + " - Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        });

        var app = new BurrowApp();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown, "shutdown-hook"));

        app.start();
    }
}
