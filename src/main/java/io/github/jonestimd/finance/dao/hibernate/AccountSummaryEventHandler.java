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
package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.event.AccountSummaryEvent;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.swing.event.EventType;

public class AccountSummaryEventHandler implements EventHandlerEventHolder {
    private final Object eventSource;
    private final SetMultimap<Long, AccountSummary> transactionChanges = HashMultimap.create();

    public AccountSummaryEventHandler(Object eventSource) {
        this.eventSource = eventSource;
    }

    @Override
    public void added(UniqueId<?> entity) {
        if (entity instanceof TransactionDetail) {
            TransactionDetail detail = (TransactionDetail) entity;
            AccountSummary summary = getSummary(detail.getTransaction(), detail.getTransaction().getAccount());
            summary.setBalance(summary.getBalance().add(detail.getAmount()));
        }
        else if (entity instanceof Transaction) {
            Transaction transaction = (Transaction) entity;
            AccountSummary summary = getSummary(transaction, transaction.getAccount());
            summary.setTransactionCount(1L);
        }
    }

    @Override
    public void deleted(UniqueId<?> entity, String[] propertyNames, Object[] previousState) {
        if (entity instanceof TransactionDetail) {
            TransactionDetail detail = (TransactionDetail) entity;
            updateAccount(getTransaction(propertyNames, previousState), detail.getAmount().negate());
        } else if (entity instanceof Transaction) {
            Transaction transaction = (Transaction) entity;
            AccountSummary summary = getSummary(transaction, transaction.getAccount());
            summary.setTransactionCount(-1L);
        }
    }

    @Override
    public void changed(UniqueId<?> entity, String[] propertyNames, Object[] previousState) {
        if (entity instanceof TransactionDetail) {
            TransactionDetail detail = (TransactionDetail) entity;
            BigDecimal oldAmount = getAmount(propertyNames, previousState);
            if (detail.getAmount().compareTo(oldAmount) != 0) {
                updateAccount(detail.getTransaction(), detail.getAmount().subtract(oldAmount));
            }
        }
        else if (entity instanceof Transaction) {
            Transaction transaction = (Transaction) entity;
            Account oldAccount = getAccount(propertyNames, previousState);
            if (! Objects.equal(transaction.getAccount(), oldAccount)) {
                updateAccounts(transaction, oldAccount);
            }
        }
    }

    private void updateAccount(Transaction transaction, BigDecimal deltaAmount) {
        AccountSummary summary = getSummary(transaction, transaction.getAccount());
        summary.setBalance(summary.getBalance().add(deltaAmount));
    }

    private void updateAccounts(Transaction transaction, Account oldAccount) {
        AccountSummary oldSummary = getSummary(transaction, oldAccount);
        if (oldSummary.getTransactionCount() == 0L) {
            AccountSummary newSummary = getSummary(transaction, transaction.getAccount());
            oldSummary.setBalance(transaction.getAmount().subtract(newSummary.getBalance()).negate());
            oldSummary.setTransactionCount(-1L);
            newSummary.setBalance(transaction.getAmount());
            newSummary.setTransactionCount(1L);
        }
    }

    private AccountSummary getSummary(Transaction transaction, Account account) {
        for (AccountSummary summary : transactionChanges.get(transaction.getId())) {
            if (account.getId().equals(summary.getTransactionAttribute().getId())) {
                return summary;
            }
        }
        AccountSummary summary = new AccountSummary(account, 0L, BigDecimal.ZERO);
        transactionChanges.put(transaction.getId(), summary);
        return summary;
    }

    private Transaction getTransaction(String[] propertyNames, Object[] state) {
        return EntityState.getValue(propertyNames, state, TransactionDetail.TRANSACTION, Transaction.class, null);
    }

    private BigDecimal getAmount(String[] propertyNames, Object[] state) {
        return EntityState.getValue(propertyNames, state, TransactionDetail.AMOUNT, BigDecimal.class, BigDecimal.ZERO);
    }

    private Account getAccount(String[] propertyNames, Object[] state) {
        return EntityState.getValue(propertyNames, state, Transaction.ACCOUNT, Account.class, null);
    }

    @Override
    public List<DomainEvent<?, ?>> getEvents() {
        return transactionChanges.isEmpty() ? EMPTY_LIST : Collections.<DomainEvent<?, ?>>singletonList(getSummaryEvent());
    }

    private AccountSummaryEvent getSummaryEvent() {
        List<AccountSummary> accountTotals = new ArrayList<>();
        for (Entry<Account, Collection<AccountSummary>> entry : updatesByAccount().entrySet()) {
            accountTotals.add(getTotal(entry.getKey(), entry.getValue()));
        }
        return new AccountSummaryEvent(eventSource, EventType.CHANGED, accountTotals);
    }

    private AccountSummary getTotal(Account account, Collection<AccountSummary> summaries) {
        AccountSummary total = new AccountSummary(account, 0L, BigDecimal.ZERO);
        for (AccountSummary summary : summaries) {
            total.setBalance(total.getBalance().add(summary.getBalance()));
            total.setTransactionCount(total.getTransactionCount() + summary.getTransactionCount());
        }
        return total;
    }

    private Map<Account, Collection<AccountSummary>> updatesByAccount() {
        return Multimaps.index(transactionChanges.values(), AccountSummary::getTransactionAttribute).asMap();
    }
}
