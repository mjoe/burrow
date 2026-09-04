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

import io.github.mjoe.burrow.core.model.Forward;
import io.github.mjoe.burrow.core.ssh.TunnelManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for managing port forwards.
 */
public final class ForwardPanel extends JPanel implements Refreshable {
    private final TunnelManager tunnelManager;
    private final ForwardTableModel tableModel;
    private final JTable table;

    public ForwardPanel(TunnelManager tunnelManager) {
        this.tunnelManager = tunnelManager;
        this.tableModel = new ForwardTableModel();
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
        addButton.addActionListener(e -> addForward());
        toolbar.add(addButton);

        var editButton = new JButton("Edit");
        editButton.addActionListener(e -> editForward());
        toolbar.add(editButton);

        var deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteForward());
        toolbar.add(deleteButton);

        toolbar.addSeparator();

        var refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        toolbar.add(refreshButton);

        add(toolbar, BorderLayout.NORTH);

        refresh();
    }

    private void addForward() {
        var dialog = new ForwardDialog((Frame) SwingUtilities.getWindowAncestor(this), null, tunnelManager);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            tunnelManager.addForward(dialog.getForward());
            refresh();
        }
    }

    private void editForward() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a forward to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var forward = tableModel.getForward(selectedRow);
        var dialog = new ForwardDialog((Frame) SwingUtilities.getWindowAncestor(this), forward, tunnelManager);
        dialog.setVisible(true);

        if (dialog.isAccepted()) {
            tunnelManager.updateForward(dialog.getForward());
            refresh();
        }
    }

    private void deleteForward() {
        var selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a forward to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var forward = tableModel.getForward(selectedRow);
        var result = JOptionPane.showConfirmDialog(this,
                "Delete forward '" + forward.alias() + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            tunnelManager.removeForward(forward.id().value());
            refresh();
        }
    }

    @Override
    public void refresh() {
        var config = tunnelManager.getConfiguration();
        tableModel.setForwards(config.forwards());
    }

    private static final class ForwardTableModel extends AbstractTableModel {
        private List<Forward> forwards = new ArrayList<>();
        private final String[] columns = {"Type", "Alias", "Connection", "Local Address", "Remote Address"};

        public void setForwards(List<Forward> forwards) {
            this.forwards = new ArrayList<>(forwards);
            fireTableDataChanged();
        }

        public Forward getForward(int row) {
            return forwards.get(row);
        }

        @Override
        public int getRowCount() {
            return forwards.size();
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
            var forward = forwards.get(rowIndex);

            return switch (columnIndex) {
                case 0 -> forward.type().name();
                case 1 -> forward.alias();
                case 2 -> forward.connectionId().value();
                case 3 -> switch (forward) {
                    case Forward.Local local -> local.localAddress().toString();
                    case Forward.Remote remote -> remote.localAddress().toString();
                    case Forward.Dynamic dynamic -> dynamic.localAddress().toString();
                };
                case 4 -> switch (forward) {
                    case Forward.Local local -> local.remoteAddress().toString();
                    case Forward.Remote remote -> remote.remoteAddress().toString();
                    case Forward.Dynamic ignored -> "SOCKS Proxy";
                };
                default -> null;
            };
        }
    }
}
