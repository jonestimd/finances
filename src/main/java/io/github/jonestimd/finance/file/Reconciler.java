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
package io.github.jonestimd.finance.file;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;

import static io.github.jonestimd.util.JavaPredicates.*;

public class Reconciler {
    private final TransactionTableModel tableModel;
    private final List<Transaction> uncleared;

    public Reconciler(TransactionTableModel tableModel) {
        this.tableModel = tableModel;
        this.uncleared = tableModel.getBeans().stream().filter(not(Transaction::isCleared)).collect(Collectors.toList());
    }

    public void reconcile(Collection<Transaction> transactions) {
        transactions.forEach(this::reconcile);
    }

    private void reconcile(Transaction transaction) {
        Transaction toClear = select(transaction);
        if (toClear.isNew()) {
            toClear.setCleared(true);
            tableModel.queueAdd(tableModel.getBeanCount()-1, toClear);
        }
        else {
            tableModel.setValueAt(true, tableModel.rowIndexOf(toClear), tableModel.getClearedColumn());
        }
    }

    private Transaction select(Transaction transaction) {
        final Stream<Transaction> matches = uncleared.stream().filter(sameAmount(transaction));
        return matches.max(samePayee(transaction)).orElse(transaction);
    }

    private Predicate<Transaction> sameAmount(Transaction transaction) {
        return t2 -> t2.getAmount().compareTo(transaction.getAmount()) == 0;
    }

    private Comparator<Transaction> samePayee(Transaction transaction) {
        return (t1, t2) -> {
            if (Objects.equals(t1.getPayee(), transaction.getPayee())) {
                return Objects.equals(t2.getPayee(), transaction.getPayee()) ? 0 : 1;
            }
            return Objects.equals(t2.getPayee(), transaction.getPayee()) ? -1 : 0;
        };
    }
}
