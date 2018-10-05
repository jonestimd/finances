// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.swing.transaction;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.SelectAccountAction;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.action.MnemonicAction;
import io.github.jonestimd.swing.component.MenuActionPanel;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.window.FrameManager;
import io.github.jonestimd.swing.window.StatusFrame;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class TransactionDetailPanel extends MenuActionPanel {
    private final DecoratedTable<TransactionDetail, TransactionDetailTableModel> detailTable;
    private final TransactionDetailTableModel tableModel = new TransactionDetailTableModel();
    private final FrameManager<WindowType> frameManager;

    public TransactionDetailPanel(FinanceTableFactory tableFactory, List<TransactionDetail> transactionDetails,
            FrameManager<WindowType> frameManager) {
        this.frameManager = frameManager;
        tableModel.setBeans(transactionDetails);
        detailTable = tableFactory.createSortedTable(tableModel, SortOrder.DESCENDING, 0);
        detailTable.setRowSelectionInterval(0, 0);
        setLayout(new BorderLayout());
        add(new JScrollPane(detailTable), BorderLayout.CENTER);
    }

    @Override
    protected void initializeMenu(JMenuBar menuBar) {
        JToolBar toolbar = ComponentFactory.newMenuToolBar();
        if (toolbar.getLayout().getClass().getSimpleName().equals("SynthToolBarLayoutManager")) {
            toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.LINE_AXIS));
        }
        menuBar.add(createTransactionsMenu(toolbar), 0);
        menuBar.add(ComponentFactory.newMenuBarSeparator());
        menuBar.add(toolbar);
    }

    private JMenu createTransactionsMenu(JToolBar toolbar) {
        final JMenu menu = ComponentFactory.newMenu(LABELS.get(), "menu.transactions.mnemonicAndName");
        addTransactionAction(menu, toolbar, new GotoAction());
        return menu;
    }

    private void addTransactionAction(JMenu menu, JToolBar toolbar, Action action) {
        menu.add(action);
        toolbar.add(ComponentFactory.newToolbarButton(action));
    }

    private void selectTransaction(TransactionTable table, Transaction transaction) {
        int index = table.getModel().indexOf(transaction);
        int row = table.convertRowIndexToView(table.getModel().getLeadRowForGroup(index));
        table.setRowSelectionInterval(row, row);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    private class GotoAction extends MnemonicAction implements SelectAccountAction {
        public GotoAction() {
            super(LABELS.get(), "action.transaction.goto");
            setEnabled(detailTable.getSelectedRowCount() == 1);
            detailTable.getSelectionModel().addListSelectionListener(event -> {
                setEnabled(detailTable.getSelectedRowCount() == 1);
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            StatusFrame frame = frameManager.showFrame(this);
            TransactionTable table = ComponentTreeUtils.findComponent(frame.getContentPane(), TransactionTable.class);
            TransactionTableModel model = table.getModel();
            final Transaction transaction = detailTable.getSelectedItems().get(0).getTransaction();
            if (model.getBeans().isEmpty()) {
                // wait for transactions to load
                model.addTableModelListener(new TableModelListener() {
                    @Override
                    public void tableChanged(TableModelEvent e) {
                        model.removeTableModelListener(this);
                        SwingUtilities.invokeLater(() -> selectTransaction(table, transaction));
                    }
                });
            }
            else selectTransaction(table, transaction);
        }

        @Override
        public WindowType getWindowInfo() {
            return WindowType.TRANSACTIONS;
        }

        @Override
        public boolean matches(StatusFrame frame) {
            if (frame.getContentPane() instanceof TransactionsPanel) {
                TransactionsPanel panel = (TransactionsPanel) frame.getContentPane();
                return getAccount().getId().equals(panel.getSelectedAccount().getId());
            }
            return false;
        }

        @Override
        public Account getAccount() {
            return detailTable.getSelectedItems().get(0).getTransaction().getAccount();
        }
    }
}
