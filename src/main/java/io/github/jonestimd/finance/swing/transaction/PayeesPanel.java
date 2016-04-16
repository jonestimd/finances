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

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JToolBar;

import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.PayeeEvent;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.MergeAction;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.swing.ComponentFactory;

import static org.apache.commons.lang.StringUtils.*;

// TODO listen for new payees
public class PayeesPanel extends TransactionSummaryTablePanel<Payee, PayeeSummary> {
    private final PayeeOperations payeeOperations;
    private final Action mergeAction;

    public PayeesPanel(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher, FinanceTableFactory tableFactory) {
        super(domainEventPublisher, tableFactory.createValidatedTable(new PayeeTableModel(domainEventPublisher), PayeeTableModel.NAME_INDEX), "payee");
        this.payeeOperations = serviceLocator.getPayeeOperations();
        mergeAction = new MergeAction<>(Payee.class, "mergePayees", getTable(), new PayeeFormat(),
                PayeeSummary::getPayee, payeeOperations, domainEventPublisher);
    }

    @Override
    protected void addActions(JToolBar toolbar) {
        super.addActions(toolbar);
        toolbar.add(ComponentFactory.newToolbarButton(mergeAction));
    }

    @Override
    protected void addActions(JMenu menu) {
        super.addActions(menu);
        menu.add(mergeAction);
    }

    @Override
    protected boolean isMatch(PayeeSummary tableRow, String criteria) {
        return criteria.isEmpty() || containsIgnoreCase(tableRow.getPayee().getName(), criteria);
    }

    @Override
    protected List<PayeeSummary> getTableData() {
        return payeeOperations.getPayeeSummaries();
    }

    @Override
    protected PayeeSummary newBean() {
        return new PayeeSummary();
    }

    @Override
    protected List<? extends DomainEvent<?, ?>> saveChanges(List<Payee> changedPayees, List<Payee> deletedPayees) {
        List<PayeeEvent> events = new ArrayList<>();
        if (! changedPayees.isEmpty()) {
            payeeOperations.saveAll(changedPayees);
            events.add(new PayeeEvent(this, EventType.CHANGED, changedPayees));
        }
        if (! deletedPayees.isEmpty()) {
            payeeOperations.deleteAll(deletedPayees);
            events.add(new PayeeEvent(this, EventType.DELETED, deletedPayees));
        }
        return events;
    }
}