// The MIT License (MIT)
//
// Copyright (c) 2024 Tim Jones
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.action.LocalizedAction;
import io.github.jonestimd.swing.table.MixedRowTable;

public class TransactionTable extends MixedRowTable<Transaction, TransactionTableModel> {
    private final Action deleteAction =
        new LocalizedAction(BundleType.LABELS.get(), "action.deleteTransactionDetail") {
            public void actionPerformed(ActionEvent e) {
                int index = getSelectionModel().getLeadSelectionIndex();
                if (getModel().queueDelete(convertRowIndexToModel(index))) {
                    setEnabled(false);
                    insertDetailAction.setEnabled(getModel().isSubRow(index));
                }
                else {
                    index = Math.max(0, index - 1);
                    getSelectionModel().setSelectionInterval(index, index);
                }
            }
        };

    private final Action insertDetailAction =
        new LocalizedAction(BundleType.LABELS.get(), "action.insertTransactionDetail") {
            public void actionPerformed(ActionEvent e) {
                insertDetail();
            }
        };

    public TransactionTable(TransactionTableModel model) {
        super(model);
        installActions();
        getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateActions();
            }
        });
        setRowSorter(new TransactionTableRowSorter(this));
    }

    private void installActions() {
        installAction(TransactionTableAction.NEXT_CELL, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                int selectedColumn = nextEditableCell();
                if (selectedColumn == getColumnCount()) {
                    int selectedRow = getSelectionModel().getLeadSelectionIndex() + 1;
                    if (selectedRow == getRowCount()) {
                        insertDetail();
                    }
                    else {
                        getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
                    }
                    selectedColumn = nextEditableCell(0);
                }
                getColumnModel().getSelectionModel().setSelectionInterval(selectedColumn, selectedColumn);
            }
        });
        installAction(TransactionTableAction.INSERT_DETAIL, insertDetailAction);
        installAction(TransactionTableAction.DELETE_DETAIL, deleteAction);
        getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    }

    private void installAction(Object key, Action action) {
        installAction(key, (KeyStroke) action.getValue(Action.ACCELERATOR_KEY), action);
    }

    private void installAction(Object key, KeyStroke keyStroke, Action action) {
        getActionMap().put(key, action);
        getInputMap().put(keyStroke, key);
    }

    @Override
    public TransactionTableRowSorter getRowSorter() {
        return (TransactionTableRowSorter) super.getRowSorter();
    }

    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        if (! (sorter instanceof TransactionTableRowSorter)) {
            sorter = new TransactionTableRowSorter(this);
        }
        sorter.setSortKeys(Collections.singletonList(defaultSortKey()));
        super.setRowSorter(sorter);
    }

    private SortKey defaultSortKey() {
        return new SortKey(getColumn(TransactionColumnAdapter.DATE_ADAPTER).getModelIndex(), SortOrder.ASCENDING);
    }

    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        if (! isPaintingForPrint()) {
            int modelColumn = convertColumnIndexToModel(column);
            if (getModel().isSubRow(convertRowIndexToModel(row))) {
                component.setForeground(transparent(component.getForeground())); // TODO resource bundle or preferences
            }
            else if (getModel().isBoldColumn(modelColumn)) {
                component.setFont(component.getFont().deriveFont(Font.BOLD));
            }
        }
        return component;
    }

    private Color transparent(Color color) {
        return new Color(color.getRed(), color.getBlue(), color.getGreen(), 128);
    }

    public void selectLastTransaction() {
        selectLastTransaction(0);
    }

    public void selectLastTransaction(int offset) {
        selectRowAt(convertRowIndexToView(getModel().getLeadRowForGroup(getModel().getBeanCount()-1)+offset));
    }

    public void selectAmountColumn() {
        int column = convertColumnIndexToModel(getModel().getDetailColumnIndex(0, ValidatedDetailColumnAdapter.AMOUNT_ADAPTER));
        setColumnSelectionInterval(column, column);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        int selectedRow = getSelectionModel().getLeadSelectionIndex();
        super.tableChanged(e);
        if (e.getFirstRow() >= 0 && selectedRow != -1 && selectedRow < getRowCount()) {
            if (getSelectionModel().getLeadSelectionIndex() < 0) {
                getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
            }
            scrollToSelectedGroup();
        }
        updateActions();
    }

    private void updateActions() {
        int rowIndex = getLeadSelectionModelIndex();
        if (rowIndex >= 0 && rowIndex < getRowCount()) {
            Transaction transaction = getModel().getBeanAtRow(rowIndex);
            boolean newTransactionRow = getModel().getGroupNumber(rowIndex) == getModel().getBeanCount()-1;
            deleteAction.setEnabled(newTransactionRow ? getModel().isSubRow(rowIndex) && transaction.getDetails().size() > 1
                    : ! getModel().isPendingDelete(rowIndex));
            insertDetailAction.setEnabled(! getModel().isPendingDelete(transaction));
        }
    }

    private int nextEditableCell() {
        return nextEditableCell(getColumnModel().getSelectionModel().getLeadSelectionIndex() + 1);
    }

    private int nextEditableCell(int selectedColumn) {
        int selectedRow = getSelectionModel().getLeadSelectionIndex();
        while (selectedColumn < getColumnCount() && ! isCellEditable(selectedRow, selectedColumn)) {
            selectedColumn++;
        }
        return selectedColumn;
    }

    public Transaction getSelectedTransaction() {
        return getModel().getBeanAtRow(getLeadSelectionModelIndex());
    }

    public void nextTransaction(int viewIndex) {
        selectRowAt(getRowSorter().nextViewGroup(viewIndex));
    }

    private void insertDetail() {
        int newRow = convertRowIndexToView(getModel().queueAppendSubRow(getLeadSelectionModelIndex()));
        scrollRectToVisible(getCellRect(newRow, 0, true));
        getSelectionModel().setSelectionInterval(newRow, newRow);
        int selectedColumn = nextEditableCell(0);
        getColumnModel().getSelectionModel().setSelectionInterval(selectedColumn, selectedColumn);
    }

    public void setDetails(Transaction source, Transaction dest) {
        int row = getModel().rowIndexOf(dest) + 1;
        for (TransactionDetail sourceDetail : source.getDetails()) {
            if (row >= getRowCount()) {
                insertDetail();
            }
            TransactionType transactionType = ValidatedDetailColumnAdapter.TYPE_ADAPTER.getValue(sourceDetail);
            getModel().setTransactionType(transactionType, row);
            getModel().setTransactionGroup(sourceDetail.getGroup(), row);
            getModel().setDetailMemo(sourceDetail.getMemo(), row);
            getModel().setAmount(sourceDetail.getAmount(), row);
            row++;
        }
    }
}