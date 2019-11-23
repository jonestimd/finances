// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.beans.ObservableBean;
import io.github.jonestimd.beans.ReadWriteAccessor;
import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Asset;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.table.model.BufferedHeaderDetailTableModel;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.DetailAdapter;
import io.github.jonestimd.swing.table.model.EmptyColumnAdapter;
import io.github.jonestimd.swing.table.model.ReadOnlyColumnAdapter;
import io.github.jonestimd.swing.table.model.SingleTypeDetailAdapter;

// TODO publish DomainEvents (for accounts/payees/securities windows)
public class TransactionTableModel extends BufferedHeaderDetailTableModel<Transaction> implements ObservableBean {
    private static final List<Class> BOLD_COLUMNS = ImmutableList.of(Payee.class, Security.class);
    private static final ReadWriteAccessor<TransactionDetail, Account> ACCOUNT_ADAPTER = new ReadWriteAccessor<TransactionDetail, Account>() {
        public Account getValue(TransactionDetail bean) {
            return bean.isTransfer() ? bean.getRelatedDetail().getTransaction().getAccount() : null;
        }

        public void setValue(TransactionDetail detail, Account account) {
            detail.getRelatedDetail().getTransaction().setAccount(account);
        }
    };
    private static final ReadWriteAccessor<TransactionDetail, TransactionCategory> CATEGORY_ADAPTER = new ReadWriteAccessor<TransactionDetail, TransactionCategory>() {
        public TransactionCategory getValue(TransactionDetail bean) {
            return bean.isTransfer() ? null : bean.getCategory();
        }

        public void setValue(TransactionDetail detail, TransactionCategory category) {
            detail.setCategory(category);
        }
    };
    private static final DetailAdapter<Transaction> ROW_ADAPTER = new SingleTypeDetailAdapter<Transaction>() {
        @Override
        public List<?> getDetails(Transaction bean, int detailTypeIndex) {
            return bean.getDetails();
        }

        @Override
        public int appendDetail(Transaction bean) {
            bean.addDetails(new TransactionDetail());
            return bean.getDetails().size();
        }
    };
    private static final Comparator<Transaction> DATE_SORT = Comparator.comparing(Transaction::getDate).thenComparingLong(Transaction::getId);
    public static final String CLEARED_BALANCE_PROPERTY = "clearedBalance";
    public static final String NEW_TRANSACTION_PAYEE_PROPERTY = "newTransactionPayee";
    private static final String RESOURCE_PREFIX = "table.transaction.";
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    protected final int clearedColumn;
    protected final int amountColumn;
    protected final int balanceColumn;
    private final Account account;
    private final Map<Transaction, BigDecimal> balances = new HashMap<>();
    private BigDecimal clearedBalance = BigDecimal.ZERO;
    // need these local references to avoid garbage collection
    private final DomainEventListener<Long, Transaction> transactionListener = this::onTransactionEvent;
    private final DomainEventListener<Long, Payee> payeeListener = new TransactionDomainEventHandler<>(TransactionColumnAdapter.PAYEE_ADAPTER);
    private final DomainEventListener<Long, Security> securityListener = new TransactionDomainEventHandler<>(TransactionColumnAdapter.SECURITY_ADAPTER);
    private final DomainEventListener<Long, Account> accountListener = new DetailDomainEventHandler<>(ACCOUNT_ADAPTER);
    private final DomainEventListener<Long, TransactionGroup> groupListener = new DetailDomainEventHandler<>(TransactionDetailColumnAdapter.GROUP_ADAPTER);
    private final DomainEventListener<Long, TransactionCategory> categoryListener = new DetailDomainEventHandler<>(CATEGORY_ADAPTER);

    public TransactionTableModel(Account account) {
        super(ROW_ADAPTER, UniqueId::getId);
        this.account = account;
        setColumnAdapters(Arrays.asList(
                TransactionColumnAdapter.DATE_ADAPTER,
                TransactionColumnAdapter.NUMBER_ADAPTER,
                TransactionColumnAdapter.PAYEE_ADAPTER,
                TransactionColumnAdapter.MEMO_ADAPTER,
                TransactionColumnAdapter.CLEARED_ADAPTER,
                TransactionColumnAdapter.AMOUNT_ADAPTER,
                new BalanceColumnAdapter()));
        setDetailColumnAdapters(Collections.singletonList(Arrays.asList(
                new EmptyColumnAdapter<>("dummyColumn0", String.class),
                TransactionDetailColumnAdapter.GROUP_ADAPTER,
                TransactionDetailColumnAdapter.TYPE_ADAPTER,
                TransactionDetailColumnAdapter.MEMO_ADAPTER,
                new EmptyColumnAdapter<>("dummyColumn1", String.class),
                TransactionDetailColumnAdapter.AMOUNT_ADAPTER,
                new EmptyColumnAdapter<>("dummyColumn2", String.class))));
        clearedColumn = getColumnIndex(TransactionColumnAdapter.CLEARED_ADAPTER);
        amountColumn = getColumnIndex(TransactionColumnAdapter.AMOUNT_ADAPTER);
        balanceColumn = getColumnCount() - 1;
    }

    protected TransactionTableModel(Account account,
                                    ColumnAdapter<Transaction, ? extends Asset> assetColumnAdapter,
                                    ColumnAdapter<TransactionDetail, BigDecimal> assetQuantityAdapter) {
        super(ROW_ADAPTER, UniqueId::getId);
        this.account = account;
        setColumnAdapters(Arrays.asList(
                TransactionColumnAdapter.DATE_ADAPTER,
                TransactionColumnAdapter.NUMBER_ADAPTER,
                TransactionColumnAdapter.PAYEE_ADAPTER,
                assetColumnAdapter,
                TransactionColumnAdapter.MEMO_ADAPTER,
                TransactionColumnAdapter.CLEARED_ADAPTER,
                TransactionColumnAdapter.AMOUNT_ADAPTER,
                new BalanceColumnAdapter()));
        setDetailColumnAdapters(Collections.singletonList(Arrays.asList(
                TransactionDetailColumnAdapter.NOTIFICATION_ADAPTER,
                TransactionDetailColumnAdapter.GROUP_ADAPTER,
                TransactionDetailColumnAdapter.TYPE_ADAPTER,
                TransactionDetailColumnAdapter.MEMO_ADAPTER,
                assetQuantityAdapter,
                new EmptyColumnAdapter<>("dummyColumn0", String.class),
                TransactionDetailColumnAdapter.AMOUNT_ADAPTER,
                new EmptyColumnAdapter<>("dummyColumn1", String.class))));
        clearedColumn = getColumnIndex(TransactionColumnAdapter.CLEARED_ADAPTER);
        amountColumn = getColumnIndex(TransactionColumnAdapter.AMOUNT_ADAPTER);
        balanceColumn = getColumnCount() - 1;
    }

    public int getClearedColumn() {
        return clearedColumn;
    }

    public Account getAccount() {
        return account;
    }

    public void setDomainEventPublisher(DomainEventPublisher eventPublisher) {
        eventPublisher.register(Account.class, accountListener);
        eventPublisher.register(Transaction.class, transactionListener);
        eventPublisher.register(Payee.class, payeeListener);
        eventPublisher.register(Security.class, securityListener);
        eventPublisher.register(TransactionGroup.class, groupListener);
        eventPublisher.register(TransactionCategory.class, categoryListener);
    }

    private void onTransactionEvent(DomainEvent<Long, Transaction> event) {
        if (event.getSource() != TransactionTableModel.this && account != null) {
            if (event.isDelete()) {
                event.getDomainObjects().forEach(TransactionTableModel.this::removeBean);
            } else {
                event.getDomainObjects().forEach(TransactionTableModel.this::onTransactionUpdate);
            }
        }
    }

    private void onTransactionUpdate(Transaction transaction) {
        if (!transaction.getAccount().getId().equals(account.getId())) {
            removeBean(transaction);
        } else if (indexOf(transaction) < 0) {
            addBean(getInsertionIndex(transaction), transaction);
        }
    }

    public void setTransactionDate(Date date, int rowIndex) {
        setValueAt(date, rowIndex, getColumnIndex(TransactionColumnAdapter.DATE_ADAPTER));
    }

    public void setTransactionType(TransactionType type, int rowIndex) {
        setValueAt(type, rowIndex, getDetailColumnIndex(0, TransactionDetailColumnAdapter.TYPE_ADAPTER));
    }

    public void setTransactionGroup(TransactionGroup group, int rowIndex) {
        setValueAt(group, rowIndex, getDetailColumnIndex(0, TransactionDetailColumnAdapter.GROUP_ADAPTER));
    }

    public void setDetailMemo(String memo, int rowIndex) {
        setValueAt(memo, rowIndex, getDetailColumnIndex(0, TransactionDetailColumnAdapter.MEMO_ADAPTER));
    }

    public void setAmount(BigDecimal amount, int rowIndex) {
        setValueAt(amount, rowIndex, getDetailColumnIndex(0, TransactionDetailColumnAdapter.AMOUNT_ADAPTER));
    }

    public boolean isPayeeColumn(int columnIndex) {
        return Payee.class.equals(getColumnClass(columnIndex));
    }

    public boolean isBoldColumn(int columnIndex) {
        return columnIndex == amountColumn || BOLD_COLUMNS.contains(getColumnClass(columnIndex));
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    private void calculateClearedBalance() {
        BigDecimal oldClearedBalance = clearedBalance;
        clearedBalance = getBeans().stream().filter(Transaction::isCleared).map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        changeSupport.firePropertyChange(CLEARED_BALANCE_PROPERTY, oldClearedBalance, clearedBalance);
    }

    private void updateClearedBalance(boolean cleared, BigDecimal amount) {
        if (cleared) {
            BigDecimal oldClearedBalance = clearedBalance;
            clearedBalance = clearedBalance.add(amount);
            changeSupport.firePropertyChange(CLEARED_BALANCE_PROPERTY, oldClearedBalance, clearedBalance);
        }
    }

    @Override
    protected int getInsertionIndex(Transaction bean) {
        for (int i = 0; i < getBeanCount(); i++) {
            if (getBean(i).getId() == null || DATE_SORT.compare(getBean(i), bean) >= 0) {
                return i;
            }
        }
        return getBeanCount();
    }

    public void addBean(int row, Transaction bean) {
        super.addBean(row, bean);
        updateBalances(row);
        updateClearedBalance(bean.isCleared(), bean.getAmount());
    }

    public void addEmptyTransaction() {
        queueAdd(new Transaction(account, new Date(), null, false, null, new TransactionDetail()));
    }

    @Override
    public boolean queueDelete(int rowIndex) {
        Transaction bean = getBeanAtRow(rowIndex);
        BigDecimal oldAmount = bean.getAmount();
        boolean subRow = isSubRow(rowIndex);
        if (!super.queueDelete(rowIndex)) {
            if (subRow && bean.isCleared()) {
                updateClearedBalance(true, bean.getAmount().subtract(oldAmount));
            }
            return false;
        }
        return true;
    }

    public void removeBean(Transaction bean) {
        int index = indexOf(bean);
        super.removeBean(bean);
        if (balances.remove(bean) != null) {
            updateBalances(index);
            updateClearedBalance(bean.isCleared(), bean.getAmount().negate());
        }
    }

    public void setBeans(Collection<Transaction> beans) {
        super.setBeans(beans);
        balances.clear();
        updateBalances(0);
        calculateClearedBalance();
    }

    public void setBean(int row, Transaction bean) {
        Transaction oldBean = getBean(row);
        super.setBean(row, bean);
        updateBalances(row);
        if (oldBean.isCleared() || bean.isCleared()) {
            BigDecimal delta = bean.isCleared() ? bean.getAmount() : BigDecimal.ZERO;
            delta = delta.subtract(oldBean.isCleared() ? oldBean.getAmount() : BigDecimal.ZERO);
            updateClearedBalance(true, delta);
        }
    }

    public void removeUnsavedEmptyDetails(Transaction transaction) {
        int row = rowIndexOf(transaction);
        for (Iterator<TransactionDetail> iter = transaction.getDetails().iterator(); iter.hasNext(); ) {
            if (iter.next().isUnsavedAndEmpty()) {
                iter.remove();
                fireTableRowsDeleted(row, row);
            } else {
                row++;
            }
        }
    }

    @Override
    protected void setCellValue(Object value, int rowIndex, int columnIndex) {
        Object oldValue = getValueAt(rowIndex, columnIndex);
        super.setCellValue(value, rowIndex, columnIndex);
        if (columnIndex == clearedColumn) {
            BigDecimal amount = getBeanAtRow(rowIndex).getAmount();
            updateClearedBalance(true, (Boolean) value ? amount : amount.negate());
        } else if (columnIndex == amountColumn) {
            updateBalances(getGroupNumber(rowIndex));
            if (getBeanAtRow(rowIndex).isCleared()) {
                BigDecimal delta = value == null ? BigDecimal.ZERO : ((BigDecimal) value);
                if (oldValue != null) {
                    delta = delta.subtract((BigDecimal) oldValue);
                }
                updateClearedBalance(true, delta);
            }
        } else if (isPayeeColumn(columnIndex) && rowIndex == getLeadRowForGroup(getBeanCount() - 1)) {
            changeSupport.firePropertyChange(NEW_TRANSACTION_PAYEE_PROPERTY, oldValue, value);
        }
    }

    public boolean isUnsavedChanges() {
        return getChangedRows().anyMatch(Transaction::isSavedOrNonempty);
    }

    private void updateBalances(int beanIndex) {
        BigDecimal balance = beanIndex <= 0 ? BigDecimal.ZERO : balances.get(getBean(beanIndex - 1));
        for (int i = beanIndex; i < getBeanCount(); i++) {
            Transaction transaction = getBean(i);
            balance = balance.add(transaction.getAmount());
            balances.put(transaction, balance);
            fireTableCellUpdated(getLeadRowForGroup(i), balanceColumn);
        }
    }

    public BigDecimal getClearedBalance() {
        return clearedBalance;
    }

    public List<TransactionDetail> getDetailDeletes(Transaction transaction) {
        int modelIndex = rowIndexOf(transaction);
        List<TransactionDetail> deletes = new ArrayList<>();
        for (TransactionDetail detail : transaction.getDetails()) {
            if (isPendingDelete(++modelIndex)) {
                deletes.add(detail);
            }
        }
        return deletes;
    }

    private Stream<TransactionDetail> getAllDetails() {
        return getBeans().stream().map(Transaction::getDetails).flatMap(Collection::stream);
    }

    private class BalanceColumnAdapter extends ReadOnlyColumnAdapter<Transaction, BigDecimal> {
        private BalanceColumnAdapter() {
            super(BundleType.LABELS.get(), RESOURCE_PREFIX, "balance", BigDecimal.class);
        }

        public BigDecimal getValue(Transaction row) {
            return balances.get(row);
        }
    }

    private abstract class DomainEventHandler<R, ID, T extends BaseDomain<ID>> implements DomainEventListener<ID, T> {
        private final ReadWriteAccessor<R, T> columnAdapter;
        private final Supplier<Stream<R>> streamSupplier;

        protected DomainEventHandler(ReadWriteAccessor<R, T> columnAdapter, Supplier<Stream<R>> streamSupplier) {
            this.columnAdapter = columnAdapter;
            this.streamSupplier = streamSupplier;
        }

        public void onDomainEvent(DomainEvent<ID, T> event) {
            if (event.isReplace()) {
                streamSupplier.get().forEach(row -> replace(row, event));
                fireTableDataChanged();
            } else if (event.isChange()) {
                streamSupplier.get().forEach(row -> update(row, event));
                fireTableDataChanged();
            }
        }

        private void replace(R row, DomainEvent<ID, T> event) {
            T value = columnAdapter.getValue(row);
            if (value != null && event.contains(value)) {
                columnAdapter.setValue(row, event.getReplacement());
            }
        }

        private void update(R row, DomainEvent<ID, T> event) {
            T value = columnAdapter.getValue(row);
            if (value != null && event.contains(value)) {
                columnAdapter.setValue(row, event.getDomainObject(value.getId()));
            }
        }
    }

    private class TransactionDomainEventHandler<ID, T extends BaseDomain<ID>> extends DomainEventHandler<Transaction, ID, T> {
        public TransactionDomainEventHandler(ReadWriteAccessor<Transaction, T> columnAdapter) {
            super(columnAdapter, getBeans()::stream);
        }
    }

    private class DetailDomainEventHandler<ID, T extends BaseDomain<ID>> extends DomainEventHandler<TransactionDetail, ID, T> {
        public DetailDomainEventHandler(ReadWriteAccessor<TransactionDetail, T> columnAdapter) {
            super(columnAdapter, TransactionTableModel.this::getAllDetails);
        }
    }
}