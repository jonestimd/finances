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
package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.SecuritySummaryEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.swing.event.EventType;

public class SecuritySummaryEventHandler implements EventHandlerEventHolder {
    private final Object eventSource;
    private final Map<SummaryKey, SecuritySummary> securityChanges = new HashMap<>();

    public SecuritySummaryEventHandler(Object eventSource) {
        this.eventSource = eventSource;
    }

    @Override
    public void added(UniqueId<?> entity) {
        if (entity instanceof Transaction) {
            Transaction transaction = (Transaction) entity;
            updateSummary(transaction.getAccount(), transaction.getSecurity(), BigDecimal.ZERO, 1L);
        }
        else if (entity instanceof TransactionDetail) {
            TransactionDetail detail = (TransactionDetail) entity;
            updateSummary(detail.getTransaction().getAccount(), detail.getTransaction().getSecurity(), getAssetQuantity(detail), 0L);
        }
    }

    @Override
    public void deleted(UniqueId <?> entity, String[] propertyNames, Object[] previousState) {
        if (entity instanceof Transaction) {
            Transaction transaction = (Transaction) entity;
            updateSummary(transaction.getAccount(), transaction.getSecurity(), BigDecimal.ZERO, -1L);
        }
        else if (entity instanceof TransactionDetail) {
            TransactionDetail detail = (TransactionDetail) entity;
            Transaction transaction = getTransaction(propertyNames, previousState);
            updateSummary(transaction.getAccount(), transaction.getSecurity(), getAssetQuantity(detail).negate(), 0L);
        }
    }

    @Override
    public void changed(UniqueId<?> entity, String[] propertyNames, Object[] previousState) {
        if (entity instanceof Transaction) {
            Transaction transaction = (Transaction) entity;
            Account oldAccount = getAccount(propertyNames, previousState);
            Security oldSecurity = getSecurity(propertyNames, previousState);
            if (!Objects.equal(transaction.getAccount(), oldAccount) || ! Objects.equal(transaction.getSecurity(), oldSecurity)) {
                updateSummary(oldAccount, oldSecurity, transaction.getAssetQuantity().negate(), -1);
                updateSummary(transaction.getAccount(), transaction.getSecurity(), transaction.getAssetQuantity(), 1);
            }
        }
        else if (entity instanceof TransactionDetail) {
            TransactionDetail detail = (TransactionDetail) entity;
            Transaction transaction = MoreObjects.firstNonNull(detail.getTransaction(), this.getTransaction(propertyNames, previousState));
            BigDecimal deltaShares = getAssetQuantity(detail).subtract(getShares(propertyNames, previousState));
            updateSummary(transaction.getAccount(), transaction.getSecurity(), deltaShares, 0);
        }
    }

    private BigDecimal getAssetQuantity(TransactionDetail detail) {
        return MoreObjects.firstNonNull(detail.getAssetQuantity(), BigDecimal.ZERO);
    }

    private void updateSummary(Account account, Security security, BigDecimal assetQuantity, long transactionCount) {
        if (security != null && (transactionCount != 0L || assetQuantity.signum() != 0)) {
            SecuritySummary summary = getSummary(account, security);
            summary.setShares(summary.getShares().add(assetQuantity));
            summary.setTransactionCount(summary.getTransactionCount() + transactionCount);
        }
    }

    private SecuritySummary getSummary(Account account, Security security) {
        SummaryKey key = new SummaryKey(account, security);
        SecuritySummary securitySummary = securityChanges.get(key);
        if (securitySummary == null) {
            securitySummary = new SecuritySummary(security, 0L, BigDecimal.ZERO, account);
            securityChanges.put(key, securitySummary);
        }
        return securitySummary;
    }

    private Account getAccount(String[] propertyNames, Object[] state) {
        return EntityState.getValue(propertyNames, state, Transaction.ACCOUNT, Account.class, null);
    }

    private Transaction getTransaction(String[] propertyNames, Object[] state) {
        return EntityState.getValue(propertyNames, state, TransactionDetail.TRANSACTION, Transaction.class, null);
    }

    private Security getSecurity(String[] propertyNames, Object[] state) {
        return EntityState.getValue(propertyNames, state, Transaction.SECURITY, Security.class, null);
    }

    private BigDecimal getShares(String[] propertyNames, Object[] state) {
        return EntityState.getValue(propertyNames, state, TransactionDetail.ASSET_QUANTITY, BigDecimal.class, BigDecimal.ZERO);
    }

    @Override
    public List<? extends DomainEvent<?, ?>> getEvents() {
        return securityChanges.isEmpty() ? Collections.<SecuritySummaryEvent>emptyList() :
                Collections.singletonList(new SecuritySummaryEvent(eventSource, EventType.CHANGED, securityChanges.values()));
    }

    private static class SummaryKey {
        private final Long accountId;
        private final Long securityId;

        private SummaryKey(Account account, Security security) {
            this.accountId = account.getId();
            this.securityId = security.getId();
        }

        public boolean equals(Object obj) {
            if (obj instanceof SummaryKey) {
                SummaryKey that = (SummaryKey) obj;
                return accountId.equals(that.accountId) && securityId.equals(that.securityId);
            }
            return false;
        }

        public int hashCode() {
            return accountId.hashCode() * 29 + securityId.hashCode();
        }
    }
}
