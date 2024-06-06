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
package io.github.jonestimd.finance.domain.account;

import java.math.BigDecimal;

import com.google.common.base.MoreObjects;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;

public class AccountSummary extends TransactionSummary<Account> {
    private BigDecimal balance;

    public AccountSummary(Currency currency) {
        super(new Account(currency), 0L);
    }

    public AccountSummary(Account account, Long transactionCount, BigDecimal balance) {
        super(account, MoreObjects.firstNonNull(transactionCount, 0L));
        this.balance = balance;
    }

    public String getName() {
        return getTransactionAttribute().getName();
    }

    public BigDecimal getBalance() {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void addToBalance(BigDecimal value) {
        if (value != null) balance = balance.add(value);
    }
}