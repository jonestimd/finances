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

import java.util.ArrayList;
import java.util.List;

import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.TransactionGroupEvent;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionGroupSummary;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.event.ReloadEventHandler;
import io.github.jonestimd.swing.window.FrameManager;

import static io.github.jonestimd.finance.swing.transaction.TransactionGroupTableModel.*;
import static org.apache.commons.lang.StringUtils.*;

// TODO merge action
public class TransactionGroupsPanel extends AccountAccessPanel<TransactionGroup, TransactionGroupSummary> {
    private final TransactionGroupOperations transactionGroupOperations;
    @SuppressWarnings("FieldCanBeLocal")
    private final ReloadEventHandler<Long, TransactionGroupSummary> reloadHandler =
            new ReloadEventHandler<>(this, "transactionGroup.action.reload.status.initialize", this::getTableData, this::getTableModel);

    public TransactionGroupsPanel(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher, FinanceTableFactory tableFactory,
            FrameManager<WindowType> frameManager) {
        super(domainEventPublisher, tableFactory.createValidatedTable(new TransactionGroupTableModel(domainEventPublisher), NAME_INDEX), "transactionGroup", frameManager);
        this.transactionGroupOperations = serviceLocator.getTransactionGroupOperations();
        domainEventPublisher.register(TransactionGroupSummary.class, reloadHandler);
    }

    @Override
    protected boolean isMatch(TransactionGroupSummary tableRow, String criteria) {
        TransactionGroup group = tableRow.getGroup();
        return criteria.isEmpty() ||  containsIgnoreCase(group.getName(), criteria) || containsIgnoreCase(group.getDescription(), criteria);
    }

    @Override
    protected List<TransactionGroupSummary> getTableData() {
        return transactionGroupOperations.getTransactionGroupSummaries();
    }

    @Override
    protected TransactionGroupSummary newBean() {
        return new TransactionGroupSummary();
    }

    @Override
    protected List<? extends DomainEvent<?, ?>> saveChanges(List<TransactionGroup> changedGroups, List<TransactionGroup> deletedGroups) {
        List<TransactionGroupEvent> events = new ArrayList<>();
        if (!changedGroups.isEmpty()) {
            transactionGroupOperations.saveAll(changedGroups);
            events.add(new TransactionGroupEvent(this, EventType.CHANGED, changedGroups));
        }
        if (!deletedGroups.isEmpty()) {
            transactionGroupOperations.deleteAll(deletedGroups);
            events.add(new TransactionGroupEvent(this, EventType.DELETED, deletedGroups));
        }
        return events;
    }
}