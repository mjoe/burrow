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

import io.github.mjoe.burrow.core.model.Connection;
import io.github.mjoe.burrow.core.model.EntityId;
import io.github.mjoe.burrow.core.ssh.TunnelManager;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating/editing SSH connections.
 */
public final class ConnectionDialog extends JDialog {
    private final JTextField aliasField;
    private final JTextField hostField;
    private final JSpinner portSpinner;
    private final JComboBox<String> identityComboBox;
    private final JCheckBox autoStartCheckBox;
    private final JCheckBox autoReconnectCheckBox;

    private boolean accepted = false;
    private final Connection originalConnection;
    private final TunnelManager tunnelManager;

    public ConnectionDialog(Frame owner, Connection connection, TunnelManager tunnelManager) {
        super(owner, connection == null ? "Add Connection" : "Edit Connection", true);
        this.originalConnection = connection;
        this.tunnelManager = tunnelManager;

        setMinimumSize(new Dimension(450, 350));
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

        // Host
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Host:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        hostField = new JTextField(20);
        formPanel.add(hostField, gbc);

        // Port
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        portSpinner = new JSpinner(new SpinnerNumberModel(22, 1, 65535, 1));
        formPanel.add(portSpinner, gbc);

        // Identity
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Identity:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        identityComboBox = new JComboBox<>();
        populateIdentities();
        formPanel.add(identityComboBox, gbc);

        // Auto Start
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Auto Start:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        autoStartCheckBox = new JCheckBox();
        formPanel.add(autoStartCheckBox, gbc);

        // Auto Reconnect
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Auto Reconnect:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        autoReconnectCheckBox = new JCheckBox();
        autoReconnectCheckBox.setSelected(true);
        formPanel.add(autoReconnectCheckBox, gbc);

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
        if (connection != null) {
            aliasField.setText(connection.alias());
            hostField.setText(connection.host());
            portSpinner.setValue(connection.port());
            autoStartCheckBox.setSelected(connection.autoStart());
            autoReconnectCheckBox.setSelected(connection.autoReconnect());

            // Select identity in combo box
            for (int i = 0; i < identityComboBox.getItemCount(); i++) {
                var item = identityComboBox.getItemAt(i);
                if (item.contains(connection.identityId().value())) {
                    identityComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private void populateIdentities() {
        var config = tunnelManager.getConfiguration();
        for (var identity : config.identities()) {
            identityComboBox.addItem(identity.alias() + " (" + identity.id().value() + ")");
        }
    }

    private void onOk() {
        var alias = aliasField.getText().trim();
        var host = hostField.getText().trim();

        if (alias.isEmpty() || host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Alias and Host are required", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (identityComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select an identity", "Validation Error", JOptionPane.WARNING_MESSAGE);
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

    public Connection getConnection() {
        var selectedItem = (String) identityComboBox.getSelectedItem();
        var identityIdStr = selectedItem.substring(selectedItem.indexOf("(") + 1, selectedItem.indexOf(")"));

        var builder = Connection.builder()
                .alias(aliasField.getText().trim())
                .host(hostField.getText().trim())
                .port((Integer) portSpinner.getValue())
                .identityId(new EntityId(identityIdStr))
                .autoStart(autoStartCheckBox.isSelected())
                .autoReconnect(autoReconnectCheckBox.isSelected());

        if (originalConnection != null) {
            builder.id(originalConnection.id());
        }

        return builder.build();
    }
}
