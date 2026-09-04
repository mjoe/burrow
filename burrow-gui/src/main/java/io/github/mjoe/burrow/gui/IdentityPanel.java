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
import io.github.mjoe.burrow.core.ssh.TunnelManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for managing SSH identities.
 */
public final class IdentityPanel extends JPanel implements Refreshable {
    private final TunnelManager tunnelManager;
    private final IdentityTableModel tableModel;
    private final JTable table;

    public IdentityPanel(TunnelManager tunnelManager) {
        this.tunnelManager = tunnelManager;
        this.tableModel = new IdentityTableModel();
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
        addButton.addActionListener(e -> addIdentity());
        toolbar.add(addButton);

        var editButton = new JButton("Edit");
        editButton.addActionListener(e -> editIdentity());
        toolbar.add(editButton);

        var deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteIdentity());
        toolbar.add(deleteButton);

        toolbar.addSeparator();

        var refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(refreshButton);

        add(toolbar, BorderLayout.NORTH);

        refresh();
    }

    private void addIdentity() {
        var dialog = new IdentityDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            tunnelManager.addIdentity(dialog.getIdentity());
            refresh();
        }
    }

    private void editIdentity() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an identity to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var identity = tableModel.getIdentity(selectedRow);
        var dialog = new IdentityDialog((Frame) SwingUtilities.getWindowAncestor(this), identity);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            tunnelManager.updateIdentity(dialog.getIdentity());
            refresh();
        }
    }

    private void deleteIdentity() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an identity to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var identity = tableModel.getIdentity(selectedRow);
        var result = JOptionPane.showConfirmDialog(this,
                "Delete identity '" + identity.alias() + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            tunnelManager.removeIdentity(identity.id().value());
            refresh();
        }
    }

    @Override
    public void refresh() {
        var config = tunnelManager.getConfiguration();
        tableModel.setIdentities(config.identities());
    }

    private static final class IdentityTableModel extends AbstractTableModel {
        private List<Identity> identities = new ArrayList<>();
        private final String[] columns = {"Alias", "Username", "Key File", "Has Password"};

        public void setIdentities(List<Identity> identities) {
            this.identities = new ArrayList<>(identities);
            fireTableDataChanged();
        }

        public Identity getIdentity(int row) {
            return identities.get(row);
        }

        @Override
        public int getRowCount() {
            return identities.size();
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
            var identity = identities.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> identity.alias();
                case 1 -> identity.username();
                case 2 -> identity.keyFilePath().orElse("-");
                case 3 -> identity.hasPassword() ? "Yes" : "No";
                default -> null;
            };
        }
    }
}
