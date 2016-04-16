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
import java.util.List;

import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.TransactionEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.operations.TransactionUpdate;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import io.github.jonestimd.finance.swing.transaction.TransactionsPanel;
import io.github.jonestimd.swing.action.BackgroundAction;
import org.apache.log4j.Logger;

/**
 * Not thread safe.  Intended for use by a single {@link TransactionsPanel}.
 */
public class CommitAction extends BackgroundAction<List<? extends DomainEvent<?, ?>>> {
    private final Logger logger = Logger.getLogger(CommitAction.class);
    private final TransactionTable transactionTable;
    private final TransactionService transactionService;
    private final DomainEventPublisher domainEventPublisher;
    private TransactionUpdate update;
    private int viewIndex;

    public CommitAction(TransactionTable transactionTable, TransactionService transactionService, DomainEventPublisher domainEventPublisher) {
        super(transactionTable, BundleType.LABELS.get(), "action.commitTransaction");
        this.transactionTable = transactionTable;
        this.transactionService = transactionService;
        this.domainEventPublisher = domainEventPublisher;
    }

    protected boolean confirmAction(ActionEvent event) {
        Transaction transaction = transactionTable.getSelectedTransaction();
        if (transaction.isUnsavedAndEmpty()) {
            logger.debug("unsaved empty transaction");
            return false;
        }
        if (transaction.getDetails().stream().anyMatch(TransactionDetail::isInvalid)) {
            logger.debug("invalid transaction detail(s)");
            return false;
        }
        int modelIndex = transactionTable.getModel().rowIndexOf(transaction);
        viewIndex = transactionTable.convertRowIndexToView(modelIndex);
        transactionTable.getModel().removeUnsavedEmptyDetails(transaction);
        if (! transactionTable.getModel().isChanged(transaction)) {
            logger.debug("transaction not saved");
            transactionTable.nextTransaction(viewIndex);
            return false;
        }
        logger.debug("saving transaction");
        update = new TransactionUpdate(transaction, transactionTable.getModel().getDetailDeletes(transaction));
        return true;
    }

    public List<? extends DomainEvent<?, ?>> performTask() {
        if (transactionTable.getModel().isPendingDelete(update.getTransaction()) || update.isDeleteAllDetails()) {
            return transactionService.deleteTransaction(update.getTransaction());
        }
        return transactionService.saveTransaction(update);
    }

    public void updateUI(List<? extends DomainEvent<?, ?>> domainEvents) {
        removeStaleEntities();
        for (DomainEvent<?, ?> domainEvent : domainEvents) {
            domainEventPublisher.publishEvent(domainEvent);
        }
        TransactionTableModel tableModel = transactionTable.getModel();
        if (! tableModel.getBean(tableModel.getBeanCount()-1).isNew()) {
            tableModel.addEmptyTransaction();
            transactionTable.selectLastTransaction();
        }
        else if (transactionTable.getSelectedRowCount() > 0) {
            transactionTable.nextTransaction(viewIndex);
        }
        else {
            transactionTable.selectRowAt(viewIndex);
        }
    }

    private void removeStaleEntities() {
        domainEventPublisher.publishEvent(new TransactionEvent(this, EventType.DELETED, update.getTransactions()));
    }
}