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

import java.util.List;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.DomainEventTablePanel;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.util.Streams;

public abstract class TransactionSummaryTablePanel<S extends UniqueId<Long> & Comparable<? super S>, T extends TransactionSummary<S>> extends DomainEventTablePanel<T> {
    protected TransactionSummaryTablePanel(DomainEventPublisher domainEventPublisher, DecoratedTable<T, ? extends ValidatedBeanListTableModel<T>> table, String resourceGroup) {
        super(domainEventPublisher, table, resourceGroup);
    }

    @Override
    protected boolean isDeleteEnabled(List<T> selectionMinusPendingDeletes) {
        return super.isDeleteEnabled(selectionMinusPendingDeletes) && ! selectionMinusPendingDeletes.stream().anyMatch(TransactionSummary::isUsed);
    }

    @Override
    protected List<T> confirmDelete(List<T> items) {
        return items;
    }

    @Override
    protected List<? extends DomainEvent<?, ?>> saveChanges(Stream<T> changedRows, List<T> deletedRows) {
        return saveChanges(Streams.map(changedRows, TransactionSummary::getTransactionAttribute), Streams.map(deletedRows, TransactionSummary::getTransactionAttribute));
    }

    protected abstract List<? extends DomainEvent<?, ?>> saveChanges(List<S> changedRows, List<S> deletedRows);
}
