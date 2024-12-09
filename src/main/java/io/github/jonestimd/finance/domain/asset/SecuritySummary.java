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
package io.github.jonestimd.finance.domain.asset;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.lang.Comparables;

public class SecuritySummary extends TransactionSummary<Security> {
    private Account account;
    private BigDecimal shares = BigDecimal.ZERO;
    private Date firstAcquired;
    private BigDecimal costBasis = BigDecimal.ZERO;
    private BigDecimal dividends = BigDecimal.ZERO;

    public SecuritySummary() {
        super(new Security(), 0L);
    }

    public SecuritySummary(Security security, long transactionCount, BigDecimal shares, Date firstAcquired, BigDecimal costBasis, BigDecimal dividends) {
        this(null, security, transactionCount, shares, firstAcquired, costBasis);
        this.dividends = dividends;
    }

    public SecuritySummary(Security security, long transactionCount, BigDecimal shares, Account account) {
        this(account, security, transactionCount, shares, null, null);
    }

    public SecuritySummary(Account account, Security security, long transactionCount, BigDecimal shares, Date firstAcquired, BigDecimal costBasis) {
        super(security, transactionCount);
        this.shares = shares;
        this.account = account;
        this.firstAcquired = firstAcquired;
        this.costBasis = costBasis;
        this.dividends = null;
    }

    public SecuritySummary(Security security, long transactionCount, BigDecimal shares) {
        this(security, transactionCount, shares, null);
    }

    public void update(SecuritySummary that) {
        setTransactionCount(getTransactionCount() + that.getTransactionCount());
        this.shares = this.shares.add(that.shares);
        this.firstAcquired = this.firstAcquired == null ? that.firstAcquired : Comparables.min(this.firstAcquired, that.firstAcquired);
        this.costBasis = this.costBasis == null ? that.costBasis : this.costBasis.add(that.costBasis);
        this.dividends = this.dividends == null ? that.dividends : this.dividends.add(that.dividends);
    }

    public String getName() {
        return getSecurity().getName();
    }

    public String getSymbol() {
        return getSecurity().getSymbol();
    }

    public Long getAccountId() {
        return account == null ? null : account.getId();
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Security getSecurity() {
        return getTransactionAttribute();
    }

    public void setSecurity(Security security) {
        super.setTransactionAttribute(security);
    }

    public BigDecimal getShares() {
        return shares;
    }

    public void setShares(BigDecimal shares) {
        this.shares = shares;
    }

    public Date getFirstAcquired() {
        return firstAcquired;
    }

    public void setFirstAcquired(Date firstAcquired) {
        this.firstAcquired = firstAcquired;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(BigDecimal costBasis) {
        this.costBasis = costBasis;
    }

    public BigDecimal getDividends() {
        return dividends;
    }

    public void setDividends(BigDecimal dividends) {
        this.dividends = dividends;
    }

    public static boolean isNotEmpty(SecuritySummary summary) {
        return summary != null && summary.getShares().signum() != 0;
    }

    public boolean isSameIds(SecuritySummary that) {
        return Objects.equals(this.getAccountId(), that.getAccountId())
                && Objects.equals(this.getSecurity().getId(), that.getSecurity().getId());
    }
}