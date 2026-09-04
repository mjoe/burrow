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

import javax.swing.*;
import java.awt.*;

/**
 * Status bar component for displaying messages.
 */
public final class StatusBar extends JPanel {
    private final JLabel messageLabel;
    private final JLabel statusLabel;
    private Timer clearTimer;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        messageLabel = new JLabel("Ready");
        messageLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        add(messageLabel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.EAST);
    }

    public void showMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setForeground(UIManager.getColor("Label.foreground"));

        // Auto-clear after 5 seconds
        if (clearTimer != null) {
            clearTimer.stop();
        }
        clearTimer = new Timer(5000, e -> messageLabel.setText("Ready"));
        clearTimer.setRepeats(false);
        clearTimer.start();
    }

    public void showWarning(String message) {
        messageLabel.setText(message);
        messageLabel.setForeground(new Color(255, 165, 0)); // Orange
    }

    public void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setForeground(Color.RED);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}
