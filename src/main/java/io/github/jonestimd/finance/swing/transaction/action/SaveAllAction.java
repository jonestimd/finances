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
import io.github.jonestimd.finance.operations.TransactionBulkUpdate;
import io.github.jonestimd.finance.operations.TransactionUpdate;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import io.github.jonestimd.finance.swing.transaction.TransactionsPanel;
import io.github.jonestimd.swing.action.BackgroundAction;

/**
 * Not thread safe.  Intended for use by a single {@link TransactionsPanel}.
 */
public class SaveAllAction extends BackgroundAction<List<? extends DomainEvent<?, ?>>> {
    private final TransactionTable transactionTable;
    private final TransactionService transactionService;
    private final DomainEventPublisher domainEventPublisher;
    private TransactionBulkUpdate bulkUpdate;

    public SaveAllAction(TransactionTable transactionTable, TransactionService transactionService, DomainEventPublisher domainEventPublisher) {
        super(transactionTable, BundleType.LABELS.get(), "action.saveAll.transaction");
        this.transactionTable = transactionTable;
        this.transactionService = transactionService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    protected boolean confirmAction(ActionEvent event) {
        if (transactionTable.getModel().getChangedRows().anyMatch(Transaction::isInvalid)) {
            return false;
        }
        bulkUpdate = new TransactionBulkUpdate();
        transactionTable.getModel().getChangedRows().forEach(transaction -> {
            if (! transaction.isUnsavedAndEmpty()) {
                transactionTable.getModel().removeUnsavedEmptyDetails(transaction);
                TransactionUpdate update = new TransactionUpdate(transaction, transactionTable.getModel().getDetailDeletes(transaction));
                if (transactionTable.getModel().isPendingDelete(transaction) || update.isDeleteAllDetails()) {
                    bulkUpdate.delete(transaction);
                }
                else {
                    bulkUpdate.update(update);
                }
            }
        });
        return ! bulkUpdate.isEmpty();
    }

    @Override
    public List<? extends DomainEvent<?, ?>> performTask() {
        return transactionService.updateTransactions(bulkUpdate);
    }

    @Override
    public void updateUI(List<? extends DomainEvent<?, ?>> domainEvents) {
        removeStaleEntities();
        domainEvents.forEach(domainEventPublisher::publishEvent);
        final TransactionTableModel tableModel = transactionTable.getModel();
        if (! tableModel.getBean(tableModel.getBeanCount()-1).isNew()) {
            tableModel.addEmptyTransaction();
            transactionTable.selectLastTransaction();
        }
    }

    protected void removeStaleEntities() {
        domainEventPublisher.publishEvent(new TransactionEvent(this, EventType.DELETED, bulkUpdate.getUpdatedTransactions()));
    }
}