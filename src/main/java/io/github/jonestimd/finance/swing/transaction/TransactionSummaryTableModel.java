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
package io.github.jonestimd.finance.swing.transaction;

import java.util.List;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;

public class TransactionSummaryTableModel<T extends Comparable<? super T> & UniqueId<Long>, S extends TransactionSummary<T>> extends ValidatedBeanListTableModel<S> {
    private final DomainEventListener<Long, T> domainEventListener = event -> {
        if (event.isDelete()) {
            removeAll(getBeans().stream().filter(event::containsAttribute)::iterator);
        }
        else if (event.isReplace()) {
            long delta = getBeans().stream().filter(event::containsAttribute).mapToLong(this::resetCount).sum();
            updateCount(event.getReplacement(), delta);
        }
    };

    public TransactionSummaryTableModel(List<? extends ColumnAdapter<? super S, ?>> columnAdapters, Class<T> summaryAttributeClass,
                                        DomainEventPublisher domainEventPublisher) {
        super(columnAdapters);
        domainEventPublisher.register(summaryAttributeClass, domainEventListener);
    }

    /**
     * @return summary count for the row.
     */
    private long resetCount(S summary) {
        long count = summary.getTransactionCount();
        summary.setTransactionCount(0);
        fireTableRowUpdated(summary);
        return count;
    }

    private void fireTableRowUpdated(S summary) {
        int row = indexOf(summary);
        fireTableRowsUpdated(row, row);
    }

    private void updateCount(T summaryAttribute, long delta) {
        S summary = getBeans().stream().filter(TransactionSummary.isSummary(summaryAttribute)).findFirst().get();
        summary.setTransactionCount(summary.getTransactionCount() + delta);
        fireTableRowUpdated(summary);
    }
}
