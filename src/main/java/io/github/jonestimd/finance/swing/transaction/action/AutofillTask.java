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

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.swing.BackgroundRunner;
import io.github.jonestimd.swing.BackgroundTask;

public class AutofillTask implements BackgroundTask<Transaction> {
    private final TransactionService transactionService;
    private final Transaction transaction;
    private final TransactionTable table;

    public static void execute(TransactionService transactionService, Transaction transaction, TransactionTable table) {
        if (transaction.isUnsavedAndEmpty()) {
            new BackgroundRunner<Transaction>(new AutofillTask(transactionService, transaction, table), table).doTask();
        }
    }

    public AutofillTask(TransactionService transactionService, Transaction transaction, TransactionTable table) {
        this.transactionService = transactionService;
        this.transaction = transaction;
        this.table = table;
    }

    @Override
    public String getStatusMessage() {
        return BundleType.LABELS.getString("action.transaction.autofill.status");
    }

    @Override
    public Transaction performTask() {
        Long payeeId = transaction.getPayee().getId();
        return payeeId == null ? null : transactionService.findLatestForPayee(payeeId);
    }

    @Override
    public void updateUI(Transaction latestForPayee) {
        if (latestForPayee != null) {
            if (latestForPayee.getDetails().size() == 1 && latestForPayee.getDetails().get(0).isTransfer()) {
                Transaction related = latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction();
                if (related.getDetails().size() > 1 || related.getAccount().getId().equals(table.getModel().getAccount().getId())) {
                    latestForPayee = related;
                }
            }
            table.setDetails(latestForPayee, transaction);
            table.selectLastTransaction(1);
            table.selectAmountColumn();
        }
    }
}
