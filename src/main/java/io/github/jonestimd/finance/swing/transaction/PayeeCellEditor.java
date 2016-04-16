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

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Ordering;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.ComboBoxDomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.lang.Comparables;
import io.github.jonestimd.swing.component.EditableComboBoxCellEditor;
import io.github.jonestimd.swing.component.FormatPrefixSelector;
import io.github.jonestimd.swing.validation.Validator;
import io.github.jonestimd.util.Streams;

public class PayeeCellEditor extends EditableComboBoxCellEditor<Payee> {
    private static final String LOADING_MESSAGE_KEY = "table.transaction.payee.initialize";
    private final PayeeOperations payeeOperations;
    // need references to event listeners to avoid garbage collection
    private final ComboBoxDomainEventListener<Long, Payee> payeeEventListener = new ComboBoxDomainEventListener<>(getComboBox());
    private final DomainEventListener<Long, Transaction> transactionEventListener = new DomainEventListener<Long, Transaction>() {
        @Override
        public void onDomainEvent(DomainEvent<Long, Transaction> event) {
            if (! event.isDelete()) {
                event.getDomainObjects().stream().filter(transaction -> transaction.getPayee() != null).forEach(transaction -> {
                    Date lastDate = lastTransactionDates.get(transaction.getPayee().getId());
                    if (lastDate == null || lastDate.before(transaction.getDate())) {
                        lastTransactionDates.put(transaction.getPayee().getId(), transaction.getDate());
                    }
                });
            }
        }
    };
    private Map<Long, Date> lastTransactionDates;

    public PayeeCellEditor(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher) {
        this(serviceLocator, domainEventPublisher, new HashMap<>());
    }

    private PayeeCellEditor(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher, final Map<Long, Date> lastTransactionDates) {
        this(serviceLocator, domainEventPublisher, Ordering.from(byTransactionDate(lastTransactionDates)).reversed());
        this.lastTransactionDates = lastTransactionDates;
    }

    private PayeeCellEditor(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher, Comparator<Payee> byLastTransactionDate) {
        super(new PayeeFormat(), Validator.empty(), new FormatPrefixSelector<>(new PayeeFormat(), byLastTransactionDate), BundleType.LABELS.getString(LOADING_MESSAGE_KEY));
        this.payeeOperations = serviceLocator.getPayeeOperations();
        domainEventPublisher.register(Payee.class, payeeEventListener);
        domainEventPublisher.register(Transaction.class, transactionEventListener);
    }

    protected List<Payee> getComboBoxValues() {
        List<PayeeSummary> payeeSummaries = payeeOperations.getPayeeSummaries();
        payeeSummaries.forEach(summary -> lastTransactionDates.put(summary.getId(), summary.getLastTransactionDate()));
        return Streams.map(payeeSummaries, PayeeSummary::getPayee);
    }

    protected Payee saveItem(Payee item) {
        return item;
    }

    private static Comparator<Payee> byTransactionDate(Map<Long, Date> transactionDates) {
        return (payee1, payee2) -> Comparables.nullFirst(transactionDates.get(payee1.getId()), transactionDates.get(payee2.getId()));
    }
}