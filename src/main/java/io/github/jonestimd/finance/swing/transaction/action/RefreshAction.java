// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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
package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.swing.action.BackgroundAction;
import org.apache.log4j.Logger;

public class RefreshAction extends BackgroundAction<List<Transaction>> {
    public static final int INITIAL_LOAD_ACTION_ID = -1;
    private final Logger logger = Logger.getLogger(RefreshAction.class);
    private final TransactionTable transactionTable;
    private final TransactionService transactionService;
    private boolean initialLoad;
    private int selectedRow;
    private int selectedColumn;

    public RefreshAction(TransactionTable transactionTable, TransactionService transactionService) {
        super(transactionTable, BundleType.LABELS.get(), "action.refreshTransactions");
        this.transactionTable = transactionTable;
        this.transactionService = transactionService;
    }

    protected boolean confirmAction(ActionEvent event) {
        initialLoad = event.getID() == INITIAL_LOAD_ACTION_ID;
        selectedRow = transactionTable.getSelectionModel().getLeadSelectionIndex();
        selectedColumn = transactionTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
        return true;
    }

    public List<Transaction> performTask() {
        logger.debug("refresh account performTask");
        return transactionService.getTransactions(transactionTable.getModel().getAccount().getId());
    }

    public void updateUI(List<Transaction> transactions) {
        logger.debug("refresh account updateUI");
        transactionTable.getModel().setBeans(transactions);
        transactionTable.getModel().queueAdd(new Transaction(transactionTable.getModel().getAccount(), new Date(), null, false, null, new TransactionDetail()));
        if (initialLoad || selectedRow < 0 || selectedRow >= transactionTable.getRowCount()) {
            transactionTable.selectLastTransaction();
        }
        else if (selectedRow >= 0 && selectedRow < transactionTable.getRowCount()) {
            transactionTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
            transactionTable.getColumnModel().getSelectionModel().setSelectionInterval(selectedColumn, selectedColumn);
        }
    }
}

