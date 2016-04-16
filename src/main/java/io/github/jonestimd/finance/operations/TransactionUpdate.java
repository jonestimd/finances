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
package io.github.jonestimd.finance.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

public class TransactionUpdate {
    private final Transaction transaction;
    private final List<TransactionDetail> deletes = new ArrayList<>();
    
    public TransactionUpdate(Transaction transaction) {
        this(transaction, Collections.<TransactionDetail>emptyList());
    }

    public TransactionUpdate(Transaction transaction, Collection<TransactionDetail> deletes) {
        this.transaction = transaction;
        this.deletes.addAll(deletes);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public List<Transaction> getTransactions() {
        List<Transaction> transactions = Lists.newArrayList(transaction);
        transaction.getDetails().stream().filter(TransactionDetail::isTransfer)
                .map(TransactionDetail::getRelatedDetail)
                .map(TransactionDetail::getTransaction)
                .forEach(transactions::add);
        return transactions;

    }

    public List<TransactionDetail> getDeletes() {
        return deletes;
    }

    public boolean isDeleteAllDetails() {
        return transaction.getDetails().size() == deletes.size();
    }
}
