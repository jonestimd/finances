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
package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
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
            SecurityColumnAdapter.COST_BASIS_ADAPTER,
            SecurityColumnAdapter.DIVIDENDS_ADAPTER);
    public static final int NAME_INDEX = ADAPTERS.indexOf(SecurityColumnAdapter.NAME_ADAPTER);

    // use account ID because the user can change the account key (company and name)
    private static Predicate<SecuritySummary> equivalentTo(SecuritySummary s1) {
        return s2 -> Objects.equal(s1.getAccountId(), s2.getAccountId()) && s1.getTransactionAttribute().getId().equals(s2.getTransactionAttribute().getId());
    }

    private final Map<Long, Account> accountMap;
    // need references to domain event listeners to avoid garbage collection
    private final DomainEventListener<Long, SecuritySummary> summaryEventListener = event -> event.getDomainObjects().forEach(AccountSecurityTableModel.this::updateShares);
    private final DomainEventListener<Long, Security> securityEventListener = event -> {
        if (event.isChange()) {
            getBeans().stream().filter(event::containsAttribute)
                    .forEach(summary -> updateSecurity(summary, event.getDomainObject(summary.getSecurity().getId())));
        }
    };
    private final DomainEventListener<Long, Account> accountEventListener = event -> {
        if (event.isChange()) {
            event.getDomainObjects().forEach(AccountSecurityTableModel.this::updateAccount);
        }
    };
    private final DomainEventListener<Long, Company> companyEventListener = new DomainEventListener<Long, Company>() {
        @Override
        public void onDomainEvent(DomainEvent<Long, Company> event) {
            if (event.isChange()) {
                accountMap.values().stream().filter(account -> account.getCompany() != null).forEach(account -> {
                    if (event.contains(account.getCompany())) {
                        accountMap.get(account.getId()).setCompany(event.getDomainObject(account.getCompanyId()));
                        putAll(account.getId(), removeAll(account.getId()));
                    }
                });
            }
        }
    };

    public AccountSecurityTableModel(DomainEventPublisher domainEventPublisher, Iterable<? extends TableDataProvider<SecuritySummary>> tableDataProviders) {
        this(domainEventPublisher, tableDataProviders, new HashMap<>());
    }

    private AccountSecurityTableModel(DomainEventPublisher domainEventPublisher, Iterable<? extends TableDataProvider<SecuritySummary>> tableDataProviders,
                                      final Map<Long, Account> accountMap) {
        super(ADAPTERS, tableDataProviders, input -> accountMap.get(input).qualifiedName(": "));
        this.accountMap = accountMap;
        domainEventPublisher.register(SecuritySummary.class, summaryEventListener);
        domainEventPublisher.register(Security.class, securityEventListener);
        domainEventPublisher.register(Account.class, accountEventListener);
        domainEventPublisher.register(Company.class, companyEventListener);
    }

    private void updateShares(SecuritySummary summary) {
        int index = indexOf(equivalentTo(summary));
        if (index >= 0) {
            updateShares(index, summary.getShares());
        }
        else {
            accountMap.put(summary.getAccount().getId(), summary.getAccount());
            put(summary.getAccount(), summary);
        }
    }

    private void updateShares(int index, BigDecimal shares) {
        SecuritySummary summary = getBean(index);
        BigDecimal oldShares = summary.getShares();
        summary.setShares(summary.getShares().add(shares));
        if (summary.getShares().compareTo(BigDecimal.ZERO) == 0L) {
            remove(index);
        }
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

    public void setSecuritySummaries(List<SecuritySummary> summaries) {
        Set<Account> accounts = Streams.unique(summaries, SecuritySummary::getAccount);
        accountMap.putAll(Streams.uniqueIndex(accounts, UniqueId::getId));
        super.setBeans(Multimaps.index(summaries, SecuritySummary::getAccountId));
    }

    @Override
    public void setBeans(Multimap<Long, SecuritySummary> beans) {
        throw new UnsupportedOperationException("use setSecuritySummaries()");
    }

    public void put(Account account, SecuritySummary summary) {
        accountMap.put(account.getId(), account);
        super.put(account.getId(), summary);
    }

    @Override
    public void put(Long group, SecuritySummary bean) {
        throw new UnsupportedOperationException("use put with account");
    }
}
