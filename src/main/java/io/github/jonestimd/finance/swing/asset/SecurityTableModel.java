// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.List;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.TransactionSummaryColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.util.Streams;

public class SecurityTableModel extends ValidatedBeanListTableModel<SecuritySummary> {
    public static final int NAME_INDEX = SecurityColumnAdapter.ADAPTERS.indexOf(SecurityColumnAdapter.NAME_ADAPTER);

    // need references to domain event listeners to avoid garbage collection
    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, SecuritySummary> summaryEventListener = this::addOrUpdateSecurity;

    public SecurityTableModel(DomainEventPublisher domainEventPublisher, Iterable<SecurityTableExtension> tableExtensions) {
        super(SecurityColumnAdapter.ADAPTERS, tableExtensions);
        domainEventPublisher.register(SecuritySummary.class, summaryEventListener);
    }

    private void addOrUpdateSecurity(DomainEvent<Long, SecuritySummary> event) {
        List<Long> securityIds = Streams.map(getBeans(), UniqueId::getId);
        for (SecuritySummary summary : event.getDomainObjects()) {
            int index = securityIds.indexOf(summary.getId());
            if (index < 0) {
                addRow(summary);
            }
            else {
                updateSecurity(getBeans().get(index), summary.getShares(), summary.getTransactionCount());
                fireTableRowsUpdated(index, index);
            }
        }
    }

    private void updateSecurity(SecuritySummary securitySummary, BigDecimal shares, long transactionCount) {
        BigDecimal oldShares = securitySummary.getShares();
        securitySummary.setShares(securitySummary.getShares().add(shares));
        notifyDataProviders(securitySummary, SecurityColumnAdapter.SHARES_ADAPTER.getColumnId(), oldShares);

        long oldCount = securitySummary.getTransactionCount();
        securitySummary.setTransactionCount(oldCount+transactionCount);
        notifyDataProviders(securitySummary, TransactionSummaryColumnAdapter.COUNT_ADAPTER.getColumnId(), oldCount);
    }
}
