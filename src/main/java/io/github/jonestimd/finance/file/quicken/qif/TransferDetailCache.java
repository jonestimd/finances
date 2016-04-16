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
package io.github.jonestimd.finance.file.quicken.qif;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;

public class TransferDetailCache {
    private Map<TransferDetailKey,List<TransactionDetail>> cache = new HashMap<TransferDetailKey,List<TransactionDetail>>();
    private Map<TransactionDetail, TransferDetailKey> reverseMap = new HashMap<TransactionDetail, TransferDetailKey>();

    public void add(TransactionDetail detail) {
        add(detail, detail.getAmount());
    }

    public void add(TransactionDetail detail, BigDecimal amount) {
        add(new TransferDetailKey(detail, amount), detail);
    }

    private void add(TransferDetailKey key, TransactionDetail detail) {
        List<TransactionDetail> details = cache.get(key);
        if (details == null) {
            details = new ArrayList<TransactionDetail>();
            cache.put(key, details);
        }
        details.add(detail);
        reverseMap.put(detail, key);
    }

    public TransactionDetail remove(Account account1, String account2Name, Date date, BigDecimal amount, TransactionGroup group) {
        return remove(new TransferDetailKey(account1, account2Name, date, amount, group));
    }

    private TransactionDetail remove(TransferDetailKey key) {
        List<TransactionDetail> details = cache.get(key);
        return details == null ? null : removeDetail(key, details.get(0));
    }

    private TransactionDetail removeDetail(TransferDetailKey key, TransactionDetail detail) {
        List<TransactionDetail> details = cache.get(key);
        details.remove(detail);
        if (details.isEmpty()) {
            cache.remove(key);
        }
        reverseMap.remove(detail);
        return detail;
    }

    public List<TransactionDetail> getPendingTransferDetails() {
        List<TransactionDetail> details = new ArrayList<TransactionDetail>();
        for (List<TransactionDetail> pendingDetails : cache.values()) {
            details.addAll(pendingDetails);
        }
        return details;
    }

    private class TransferDetailKey {
        private long account1Id;
        private String account2Name;
        private Date transactionDate;
        private BigDecimal amount;
        private Long groupId;

        public TransferDetailKey(TransactionDetail detail, BigDecimal amount) {
            this.account1Id = detail.getTransaction().getAccount().getId();
            this.account2Name = detail.getRelatedDetail().getTransaction().getAccount().getName();
            this.transactionDate = detail.getTransaction().getDate();
            this.amount = amount;
            this.groupId = detail.getGroup() == null ? null : detail.getGroup().getId();
        }

        public TransferDetailKey(Account account1, String account2Name, Date transactionDate, BigDecimal amount, TransactionGroup group) {
            this.account1Id = account1.getId();
            this.account2Name = account2Name;
            this.transactionDate = transactionDate;
            this.amount = amount;
            this.groupId = group == null ? null : group.getId();
        }

        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (account1Id ^ (account1Id >>> 32));
            result = prime * result + ((account2Name == null) ? 0 : account2Name.hashCode());
            long temp = amount.hashCode();
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
            result = prime * result + ((transactionDate == null) ? 0 : transactionDate.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final TransferDetailKey other = (TransferDetailKey) obj;
            if (account1Id != other.account1Id) return false;
            if (account2Name == null ? other.account2Name != null : !account2Name.equals(other.account2Name)) return false;
            if (! amount.equals(other.amount)) return false;
            if (groupId == null ? other.groupId != null : !groupId.equals(other.groupId)) return false;
            if (transactionDate == null ? other.transactionDate != null : !transactionDate.equals(other.transactionDate)) return false;
            return true;
        }
    }
}