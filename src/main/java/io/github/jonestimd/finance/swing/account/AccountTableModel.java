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
package io.github.jonestimd.finance.swing.account;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.event.TableModelEvent;

import com.google.common.base.MoreObjects;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.TransactionSummaryColumnAdapter;
import io.github.jonestimd.swing.table.PropertyAdapter;
import io.github.jonestimd.swing.table.TableSummary;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionPropertyAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.util.Streams;

public class AccountTableModel extends ValidatedBeanListTableModel<AccountSummary> implements TableSummary {
    private static final List<ColumnAdapter<? super AccountSummary, ?>> ADAPTERS = Arrays.<ColumnAdapter<? super AccountSummary, ?>>asList(
        AccountColumnAdapter.CLOSED_ADAPTER,
        AccountColumnAdapter.COMPANY_ADAPTER,
        AccountColumnAdapter.NAME_ADAPTER,
        AccountColumnAdapter.TYPE_ADAPTER,
        AccountColumnAdapter.NUMBER_ADAPTER,
        AccountColumnAdapter.DESCRIPTION_ADAPTER,
        TransactionSummaryColumnAdapter.COUNT_ADAPTER,
        AccountColumnAdapter.BALANCE_ADAPTER);
    public static final int COMPANY_INDEX = ADAPTERS.indexOf(AccountColumnAdapter.COMPANY_ADAPTER);
    public static final int NAME_INDEX = ADAPTERS.indexOf(AccountColumnAdapter.NAME_ADAPTER);
    public static final String TOTAL_PROPERTY = "total";

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private final PropertyAdapter<BigDecimal> totalPropertyAdapter = new FunctionPropertyAdapter<>(
            TOTAL_PROPERTY, BundleType.LABELS.getString("panel.accounts.cashBalance"), this::getTotal, FormatFactory::currencyFormat);
    private final DomainEventListener<Long, AccountSummary> domainEventListener = new DomainEventListener<Long, AccountSummary>() {
        @Override
        public void onDomainEvent(DomainEvent<Long, AccountSummary> event) {
            if (! getBeans().isEmpty()) {
                List<Long> accountIds = Streams.map(getBeans(), UniqueId::getId);
                for (AccountSummary summary : event.getDomainObjects()) {
                    int index = accountIds.indexOf(summary.getId());
                    updateAccount(getBeans().get(index), summary.getBalance(), summary.getTransactionCount());
                    fireTableRowsUpdated(index, index);
                }
            }
            changeSupport.firePropertyChange(TOTAL_PROPERTY, null, total);
        }

        private void updateAccount(AccountSummary accountSummary, BigDecimal balance, long transactionCount) {
            total = total.add(balance);
            accountSummary.setBalance(accountSummary.getBalance().add(balance));
            accountSummary.setTransactionCount(accountSummary.getTransactionCount()+transactionCount);
        }
    };
    private BigDecimal total = BigDecimal.ZERO;

    public AccountTableModel(DomainEventPublisher domainEventPublisher) {
        super(ADAPTERS);
        domainEventPublisher.register(AccountSummary.class, domainEventListener);
    }

    @Override
    public List<? extends PropertyAdapter<?>> getSummaryProperties() {
        return Collections.singletonList(totalPropertyAdapter);
    }

    public void fireTableCellUpdated(int rowIndex, int columnIndex) {
        super.fireTableCellUpdated(rowIndex, columnIndex);
        if (columnIndex == COMPANY_INDEX) {
            for (int i = 0; i < getRowCount(); i++) {
                validateCell(i, NAME_INDEX);
            }
            super.fireTableChanged(new TableModelEvent(this, 0, Integer.MAX_VALUE, NAME_INDEX));
        }
    }

    @Override
    public void setBeans(Collection<AccountSummary> beans) {
        super.setBeans(beans);
        calculateTotal();
    }

    public BigDecimal getTotal() {
        return total;
    }

    private void calculateTotal() {
        total = BigDecimal.ZERO;
        for (AccountSummary account : getBeans()) {
            total = total.add(MoreObjects.firstNonNull(account.getBalance(), BigDecimal.ZERO));
        }
        changeSupport.firePropertyChange(TOTAL_PROPERTY, null, total);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }
}