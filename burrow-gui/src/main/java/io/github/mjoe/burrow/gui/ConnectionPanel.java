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
import io.github.mjoe.burrow.core.model.ConnectionStatus;
import io.github.mjoe.burrow.core.ssh.TunnelManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for managing SSH connections.
 */
public final class ConnectionPanel extends JPanel implements Refreshable {
    private final TunnelManager tunnelManager;
    private final ConnectionTableModel tableModel;
    private final JTable table;

    public ConnectionPanel(TunnelManager tunnelManager) {
        this.tunnelManager = tunnelManager;
        this.tableModel = new ConnectionTableModel();
        this.table = new JTable(tableModel);

        setLayout(new BorderLayout());

        // Table
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Toolbar
        var toolbar = new JToolBar();
        toolbar.setFloatable(false);

        var addButton = new JButton("Add");
        addButton.addActionListener(e -> addConnection());
        toolbar.add(addButton);

        var editButton = new JButton("Edit");
        editButton.addActionListener(e -> editConnection());
        toolbar.add(editButton);

        var deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteConnection());
        toolbar.add(deleteButton);

        toolbar.addSeparator();

        var connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectSelected());
        toolbar.add(connectButton);

        var disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnectSelected());
        toolbar.add(disconnectButton);

        toolbar.addSeparator();

        var refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(refreshButton);

        add(toolbar, BorderLayout.NORTH);

        refresh();
    }

    private void addConnection() {
        var dialog = new ConnectionDialog((Frame) SwingUtilities.getWindowAncestor(this), null, tunnelManager);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            refresh();
        }
    }

    private void editConnection() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a connection to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var connection = tableModel.getConnection(selectedRow);
        var dialog = new ConnectionDialog((Frame) SwingUtilities.getWindowAncestor(this), connection, tunnelManager);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            refresh();
        }
    }

    private void deleteConnection() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a connection to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var connection = tableModel.getConnection(selectedRow);
        var result = JOptionPane.showConfirmDialog(this,
                "Delete connection '" + connection.alias() + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            refresh();
        }
    }

    private void connectSelected() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a connection to connect", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var connection = tableModel.getConnection(selectedRow);
        tunnelManager.connect(connection.id().value());
    }

    private void disconnectSelected() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a connection to disconnect", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var connection = tableModel.getConnection(selectedRow);
        tunnelManager.disconnect(connection.id().value());
    }

    @Override
    public void refresh() {
        var config = tunnelManager.getConfiguration();
        tableModel.setConnections(config.connections(), tunnelManager.getAllStatuses());
    }

    private static final class ConnectionTableModel extends AbstractTableModel {
        private List<Connection> connections = new ArrayList<>();
        private java.util.Map<String, ConnectionStatus> statuses = java.util.Map.of();
        private final String[] columns = {"Status", "Alias", "Host", "Port", "Identity", "Auto Start", "Auto Reconnect"};

        public void setConnections(List<Connection> connections, java.util.Map<String, ConnectionStatus> statuses) {
            this.connections = new ArrayList<>(connections);
            this.statuses = statuses;
            fireTableDataChanged();
        }

        public Connection getConnection(int row) {
            return connections.get(row);
        }

        @Override
        public int getRowCount() {
            return connections.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            var connection = connections.get(rowIndex);
            var status = statuses.getOrDefault(connection.id().value(), ConnectionStatus.DISCONNECTED);

            return switch (columnIndex) {
                case 0 -> status.displayName();
                case 1 -> connection.alias();
                case 2 -> connection.host();
                case 3 -> connection.port();
                case 4 -> connection.identityId().value();
                case 5 -> connection.autoStart() ? "Yes" : "No";
                case 6 -> connection.autoReconnect() ? "Yes" : "No";
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3) return Integer.class;
            return String.class;
        }
    }
}
