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

import io.github.mjoe.burrow.core.model.EntityId;
import io.github.mjoe.burrow.core.model.Forward;
import io.github.mjoe.burrow.core.ssh.TunnelManager;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating/editing port forwards.
 */
public final class ForwardDialog extends JDialog {
    private final JComboBox<String> typeComboBox;
    private final JTextField aliasField;
    private final JComboBox<String> connectionComboBox;
    private final JTextField localHostField;
    private final JSpinner localPortSpinner;
    private final JTextField remoteHostField;
    private final JSpinner remotePortSpinner;

    private boolean accepted = false;
    private final Forward originalForward;
    private final TunnelManager tunnelManager;

    public ForwardDialog(Frame owner, Forward forward, TunnelManager tunnelManager) {
        super(owner, forward == null ? "Add Forward" : "Edit Forward", true);
        this.originalForward = forward;
        this.tunnelManager = tunnelManager;

        setMinimumSize(new Dimension(500, 400));
        setLayout(new BorderLayout());

        // Form panel
        var formPanel = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Type:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        typeComboBox = new JComboBox<>(new String[]{"Local", "Remote", "Dynamic"});
        typeComboBox.addActionListener(e -> updateFields());
        formPanel.add(typeComboBox, gbc);

        // Alias
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Alias:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        aliasField = new JTextField(20);
        formPanel.add(aliasField, gbc);

        // Connection
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Connection:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        connectionComboBox = new JComboBox<>();
        populateConnections();
        formPanel.add(connectionComboBox, gbc);

        // Local Host
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Local Host:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        localHostField = new JTextField("127.0.0.1", 20);
        formPanel.add(localHostField, gbc);

        // Local Port
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Local Port:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        localPortSpinner = new JSpinner(new SpinnerNumberModel(8080, 1, 65535, 1));
        formPanel.add(localPortSpinner, gbc);

        // Remote Host
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Remote Host:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        remoteHostField = new JTextField("127.0.0.1", 20);
        formPanel.add(remoteHostField, gbc);

        // Remote Port
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Remote Port:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        remotePortSpinner = new JSpinner(new SpinnerNumberModel(80, 1, 65535, 1));
        formPanel.add(remotePortSpinner, gbc);

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
        if (forward != null) {
            typeComboBox.setSelectedItem(switch (forward.type()) {
                case LOCAL -> "Local";
                case REMOTE -> "Remote";
                case DYNAMIC -> "Dynamic";
            });
            aliasField.setText(forward.alias());

            // Select connection in combo box
            for (int i = 0; i < connectionComboBox.getItemCount(); i++) {
                var item = connectionComboBox.getItemAt(i);
                if (item.contains(forward.connectionId().value())) {
                    connectionComboBox.setSelectedIndex(i);
                    break;
                }
            }

            switch (forward) {
                case Forward.Local local -> {
                    localHostField.setText(local.localAddress().getHostString());
                    localPortSpinner.setValue(local.localAddress().getPort());
                    remoteHostField.setText(local.remoteAddress().getHostString());
                    remotePortSpinner.setValue(local.remoteAddress().getPort());
                }
                case Forward.Remote remote -> {
                    remoteHostField.setText(remote.remoteAddress().getHostString());
                    remotePortSpinner.setValue(remote.remoteAddress().getPort());
                    localHostField.setText(remote.localAddress().getHostString());
                    localPortSpinner.setValue(remote.localAddress().getPort());
                }
                case Forward.Dynamic dynamic -> {
                    localHostField.setText(dynamic.localAddress().getHostString());
                    localPortSpinner.setValue(dynamic.localAddress().getPort());
                }
            }
        }

        updateFields();
        pack();
        setLocationRelativeTo(owner);
    }

    private void populateConnections() {
        var config = tunnelManager.getConfiguration();
        for (var connection : config.connections()) {
            connectionComboBox.addItem(connection.alias() + " (" + connection.id().value() + ")");
        }
    }

    private void updateFields() {
        var type = (String) typeComboBox.getSelectedItem();
        var isDynamic = "Dynamic".equals(type);

        remoteHostField.setEnabled(!isDynamic);
        remotePortSpinner.setEnabled(!isDynamic);
    }

    private void onOk() {
        var alias = aliasField.getText().trim();

        if (alias.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Alias is required", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (connectionComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select a connection", "Validation Error", JOptionPane.WARNING_MESSAGE);
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

    public Forward getForward() {
        var type = (String) typeComboBox.getSelectedItem();
        var selectedItem = (String) connectionComboBox.getSelectedItem();
        var connectionIdStr = selectedItem.substring(selectedItem.indexOf("(") + 1, selectedItem.indexOf(")"));

        return switch (type) {
            case "Remote" -> {
                var builder = Forward.Remote.builder()
                        .alias(aliasField.getText().trim())
                        .connectionId(new EntityId(connectionIdStr))
                        .remoteAddress(remoteHostField.getText().trim(), (Integer) remotePortSpinner.getValue())
                        .localAddress(localHostField.getText().trim(), (Integer) localPortSpinner.getValue());
                if (originalForward != null) {
                    builder.id(originalForward.id());
                }
                yield builder.build();
            }
            case "Dynamic" -> {
                var builder = Forward.Dynamic.builder()
                        .alias(aliasField.getText().trim())
                        .connectionId(new EntityId(connectionIdStr))
                        .localAddress(localHostField.getText().trim(), (Integer) localPortSpinner.getValue());
                if (originalForward != null) {
                    builder.id(originalForward.id());
                }
                yield builder.build();
            }
            default -> {
                var builder = Forward.Local.builder()
                        .alias(aliasField.getText().trim())
                        .connectionId(new EntityId(connectionIdStr))
                        .localAddress(localHostField.getText().trim(), (Integer) localPortSpinner.getValue())
                        .remoteAddress(remoteHostField.getText().trim(), (Integer) remotePortSpinner.getValue());
                if (originalForward != null) {
                    builder.id(originalForward.id());
                }
                yield builder.build();
            }
        };
    }
}
