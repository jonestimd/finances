// The MIT License (MIT)
//
// Copyright (c) 2024 Tim Jones
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.TransactionSummaryColumnAdapter;
import io.github.jonestimd.swing.table.model.BeanListMultimapTableModel;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.TableDataProvider;
import io.github.jonestimd.util.Streams;

public class AccountSecurityTableModel extends BeanListMultimapTableModel<Long, SecuritySummary> {
    private static final List<ColumnAdapter<? super SecuritySummary, ?>> ADAPTERS = ImmutableList.<ColumnAdapter<? super SecuritySummary, ?>>of(
            SecurityColumnAdapter.NAME_ADAPTER,
            SecurityColumnAdapter.TYPE_ADAPTER,
            SecurityColumnAdapter.SYMBOL_ADAPTER,
            SecurityColumnAdapter.FIRST_PURCHASE_ADAPTER,
            TransactionSummaryColumnAdapter.COUNT_ADAPTER,
            SecurityColumnAdapter.SHARES_ADAPTER,
            SecurityColumnAdapter.COST_BASIS_ADAPTER);
    public static final int NAME_INDEX = ADAPTERS.indexOf(SecurityColumnAdapter.NAME_ADAPTER);

    // use account ID because the user can change the account key (company and name)
    private static Predicate<SecuritySummary> equivalentTo(SecuritySummary s1) {
        return s2 -> Objects.equal(s1.getAccountId(), s2.getAccountId()) && s1.getTransactionAttribute().getId().equals(s2.getTransactionAttribute().getId());
    }

    private final Map<Long, Account> accountMap;
    // need references to domain event listeners to avoid garbage collection
    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, SecuritySummary> summaryEventListener = this::onSummaryChange;
    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, Security> securityEventListener = this::onSecurityChange;
    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, Account> accountEventListener = this::onAccountChange;
    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, Company> companyEventListener = this::onCompanyChange;

    public AccountSecurityTableModel(DomainEventPublisher domainEventPublisher, Iterable<? extends TableDataProvider<SecuritySummary>> tableDataProviders) {
        this(domainEventPublisher, tableDataProviders, new HashMap<>());
    }

    private AccountSecurityTableModel(DomainEventPublisher domainEventPublisher, Iterable<? extends TableDataProvider<SecuritySummary>> tableDataProviders,
                                      final Map<Long, Account> accountMap) {
        super(ADAPTERS, tableDataProviders, SecuritySummary::getAccountId, accountId -> {
            Account account = accountMap.get(accountId);
            return account != null ? account.qualifiedName(": ") : "";
        });
        this.accountMap = accountMap;
        domainEventPublisher.register(SecuritySummary.class, summaryEventListener);
        domainEventPublisher.register(Security.class, securityEventListener);
        domainEventPublisher.register(Account.class, accountEventListener);
        domainEventPublisher.register(Company.class, companyEventListener);
    }

    private void onSecurityChange(DomainEvent<Long, Security> event) {
        if (event.isChange()) {
            getBeans().stream().filter(event::containsAttribute).forEach(
                    summary -> updateSecurity(summary, event.getDomainObject(summary.getSecurity().getId())));
        }
    }

    private void onAccountChange(DomainEvent<Long, Account> event) {
        if (event.isChange()) {
            event.getDomainObjects().forEach(this::updateAccount);
        }
    }

    private void onCompanyChange(DomainEvent<Long, Company> event) {
        if (event.isChange()) {
            accountMap.values().stream().filter(account -> account.getCompany() != null).forEach(account -> {
                if (event.contains(account.getCompany())) {
                    accountMap.get(account.getId()).setCompany(event.getDomainObject(account.getCompanyId()));
                    putAll(account.getId(), removeAll(account.getId()));
                }
            });
        }
    }

    private void onSummaryChange(DomainEvent<Long, SecuritySummary> event) {
        if (event.isChange()) event.getDomainObjects().forEach(this::addShares);
        else if (event.isReplace()) event.getDomainObjects().forEach(this::setShares);
    }

    private void addShares(SecuritySummary summary) {
        int index = indexOf(equivalentTo(summary));
        if (index >= 0) {
            SecuritySummary bean = getBean(index);
            bean.setTransactionCount(bean.getTransactionCount() + summary.getTransactionCount());
            updateShares(index, summary.getShares()::add);
        }
        else {
            accountMap.put(summary.getAccount().getId(), summary.getAccount());
            put(summary.getAccountId(), summary);
        }
    }

    private void setShares(SecuritySummary summary) {
        int index = indexOf(equivalentTo(summary));
        if (index >= 0) {
            getBean(index).setTransactionCount(summary.getTransactionCount());
            updateShares(index, ignored -> summary.getShares());
        }
    }

    private void updateShares(int index, Function<BigDecimal, BigDecimal> update) {
        SecuritySummary summary = getBean(index);
        BigDecimal oldShares = summary.getShares();
        summary.setShares(update.apply(summary.getShares()));
        if (summary.getShares().signum() == 0) remove(index);
        else {
            notifyDataProviders(summary, SecurityColumnAdapter.SHARES_ADAPTER.getColumnId(), oldShares);
            fireTableRowsUpdated(index, index);
        }
    }

    private void updateSecurity(SecuritySummary summary, Security security) {
        summary.setSecurity(security);
        int index = indexOf(equivalentTo(summary));
        fireTableRowsUpdated(index, index);
    }

    private void updateAccount(Account updatedAccount) {
        if (accountMap.containsKey(updatedAccount.getId())) {
            accountMap.put(updatedAccount.getId(), updatedAccount);
            putAll(updatedAccount.getId(), removeAll(updatedAccount.getId()));
            for (SecuritySummary summary : getBeans(updatedAccount.getId())) {
                summary.setAccount(updatedAccount);
            }
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public void setBeans(Multimap<Long, SecuritySummary> beans) {
        Set<Account> accounts = Streams.unique(beans.values(), SecuritySummary::getAccount);
        accountMap.putAll(Streams.uniqueIndex(accounts, UniqueId::getId));
        super.setBeans(beans);
    }

    @Override
    protected void setBean(int rowIndex, SecuritySummary bean) {
        accountMap.put(bean.getAccountId(), bean.getAccount());
        super.setBean(rowIndex, bean);
    }

    @Override
    public void put(Long group, SecuritySummary bean) {
        accountMap.put(group, bean.getAccount());
        super.put(group, bean);
    }
}
