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

import io.github.mjoe.burrow.core.model.Identity;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating/editing SSH identities.
 */
public final class IdentityDialog extends JDialog {
    private final JTextField aliasField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JTextField keyFileField;
    private final JPasswordField keyPassphraseField;
    private final JButton browseButton;

    private boolean accepted = false;
    private final Identity originalIdentity;

    public IdentityDialog(Frame owner, Identity identity) {
        super(owner, identity == null ? "Add Identity" : "Edit Identity", true);
        this.originalIdentity = identity;

        setMinimumSize(new Dimension(400, 300));
        setLayout(new BorderLayout());

        // Form panel
        var formPanel = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Alias
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Alias:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        aliasField = new JTextField(20);
        formPanel.add(aliasField, gbc);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Key File
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Key File:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        var keyFilePanel = new JPanel(new BorderLayout());
        keyFileField = new JTextField(20);
        browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseKeyFile());
        keyFilePanel.add(keyFileField, BorderLayout.CENTER);
        keyFilePanel.add(browseButton, BorderLayout.EAST);
        formPanel.add(keyFilePanel, gbc);

        // Key Passphrase
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Key Passphrase:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        keyPassphraseField = new JPasswordField(20);
        formPanel.add(keyPassphraseField, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        var okButton = new JButton("OK");
        var cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> onCancel());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Populate fields if editing
        if (identity != null) {
            aliasField.setText(identity.alias());
            usernameField.setText(identity.username());
            identity.password().ifPresent(p -> passwordField.setText(p));
            identity.keyFilePath().ifPresent(k -> keyFileField.setText(k));
            identity.keyPassphrase().ifPresent(p -> keyPassphraseField.setText(p));
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private void browseKeyFile() {
        var chooser = new JFileChooser();
        chooser.setDialogTitle("Select Private Key File");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            keyFileField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onOk() {
        var alias = aliasField.getText().trim();
        var username = usernameField.getText().trim();
        var password = new String(passwordField.getPassword()).trim();
        var keyFile = keyFileField.getText().trim();
        var keyPassphrase = new String(keyPassphraseField.getPassword()).trim();

        if (alias.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Alias and Username are required", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        accepted = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Identity getIdentity() {
        var builder = Identity.builder()
                .alias(aliasField.getText().trim())
                .username(usernameField.getText().trim());

        var password = new String(passwordField.getPassword()).trim();
        if (!password.isEmpty()) {
            builder.password(password);
        }

        var keyFile = keyFileField.getText().trim();
        if (!keyFile.isEmpty()) {
            builder.keyFilePath(keyFile);
        }

        var keyPassphrase = new String(keyPassphraseField.getPassword()).trim();
        if (!keyPassphrase.isEmpty()) {
            builder.keyPassphrase(keyPassphrase);
        }

        if (originalIdentity != null) {
            builder.id(originalIdentity.id());
        }

        return builder.build();
    }
}
