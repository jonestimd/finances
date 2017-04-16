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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.table.model.ColumnAdapter;

public class PayeeTableModel extends TransactionSummaryTableModel<Payee, PayeeSummary> {
    private static final List<ColumnAdapter<? super PayeeSummary, ?>> ADAPTERS = Arrays.<ColumnAdapter<? super PayeeSummary, ?>>asList(
            PayeeColumnAdapter.NAME_ADAPTER,
            TransactionSummaryColumnAdapter.COUNT_ADAPTER);
    public static final int NAME_INDEX = 0;

    // need strong reference to avoid garbage collection
    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, Payee> domainEventListener = event -> {
        if (event.isAdd()) {
            for (Payee payee : event.getDomainObjects()) {
                addRow(new PayeeSummary(payee, 1, new Date()));
            }
        }
    };

    public PayeeTableModel(DomainEventPublisher domainEventPublisher) {
        super(ADAPTERS, Payee.class, domainEventPublisher);
        domainEventPublisher.register(Payee.class, domainEventListener);
    }
}