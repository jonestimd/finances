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
package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity @Table(name="tx") @Inheritance(strategy=InheritanceType.JOINED)
@SequenceGenerator(name="id_generator", sequenceName="tx_id_seq")
@NamedQueries({
    @NamedQuery(name = Transaction.REPLACE_PAYEE_QUERY, query =
        "update Transaction set payee.id = :newPayeeId where payee.id in (:oldPayeeIds)"),
    @NamedQuery(name = Transaction.LATEST_FOR_PAYEE_QUERY, query =
        "from Transaction where payee.id = :payeeId order by date desc, id desc")
})
public class Transaction extends BaseDomain<Long> {
    public static final String REPLACE_PAYEE_QUERY = "transaction.replacePayee";
    public static final String LATEST_FOR_PAYEE_QUERY = "transaction.latestForPayee";
    public static final String ACCOUNT = "account";
    public static final String DATE = "date";
    public static final String CLEARED = "cleared";
    public static final String MEMO = "memo";
    public static final String DETAILS = "details";
    public static final String NUMBER = "number";
    public static final String PAYEE = "payee";
    public static final String AMOUNT = "amount";
    public static final String SECURITY = "security";

    @Id @GeneratedValue(strategy=GenerationType.AUTO, generator="id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="account_id", foreignKey = @ForeignKey(name="tx_account_fk"))
    private Account account;
    @Column(name="date", nullable=false) @Temporal(value=TemporalType.DATE)
    private Date date;
    @Column(name="reference_number", length=30)
    private String number;
    @ManyToOne @JoinColumn(name="payee_id", foreignKey = @ForeignKey(name="tx_payee_fk"))
    private Payee payee;
    @Column(name="cleared", nullable=false, length=1) @Type(type="yes_no")
    private boolean cleared;
    @Column(name = "memo", length = 2000)
    private String memo;
    @ManyToOne @JoinColumn(name = "security_id", foreignKey = @ForeignKey(name = "tx_security_fk"))
    private Security security;
    @OneToMany(fetch=FetchType.EAGER, mappedBy="transaction")
    @Cascade({CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.DELETE})
    private List<TransactionDetail> details = new ArrayList<>();

    public Transaction() {}

    public Transaction(Long id) {
        this.id = id;
    }

    public Transaction(Account account, Date date, boolean cleared, String memo) {
        this.account = account;
        this.date = date;
        this.cleared = cleared;
        this.memo = memo;
    }

    public Transaction(Account account, Date date, Payee payee, boolean cleared, String memo, TransactionDetail ... details) {
        this(account, date, cleared, memo);
        this.payee = payee;
        addDetails(details);
    }

    public Transaction(Account account, Transaction transaction, TransactionDetail ... details) {
        this(account, transaction.getDate(), transaction.getPayee(), transaction.isCleared(), transaction.getMemo(), details);
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        for (TransactionDetail detail : details) {
            if (detail.isTransfer()) {
                detail.getRelatedDetail().getTransaction().date = date;
            }
        }
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Payee getPayee() {
        return payee;
    }

    public void setPayee(Payee payee) {
        this.payee = payee;
    }

    public boolean isCleared() {
        return cleared;
    }

    public void setCleared(boolean cleared) {
        this.cleared = cleared;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public List<TransactionDetail> getDetails() {
        return details;
    }

    public Optional<TransactionDetail> findFirstDetail(Predicate<TransactionDetail> predicate) {
        return details.stream().filter(predicate).findFirst();
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
        for (TransactionDetail detail : details) {
            if (detail.isTransfer() && detail.getAssetQuantity() != null && detail.relatedDetail.getTransaction().getSecurity() != security) {
                detail.relatedDetail.getTransaction().setSecurity(security);
            }
        }
    }

    public boolean isSecurity() {
        return security != null;
    }

    public boolean isSecurityRequired() {
        return details.stream().anyMatch(d -> SecurityAction.isSecurityRequired(d.getCategory())
                || d.isTransfer() && d.getAssetQuantity() != null);
    }

    public TransactionDetail getDetail(long detailId) {
        for (TransactionDetail detail : details) {
            if (detail.getId() != null && detail.getId() == detailId) {
                return detail;
            }
        }
        return null;
    }

    public void addDetails(TransactionDetail ... newDetails) {
        addDetails(Arrays.asList(newDetails));
    }

    public void addDetails(Collection<TransactionDetail> newDetails) {
        for (TransactionDetail detail : newDetails) {
            detail.setTransaction(this);
        }
    }

    public BigDecimal getAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        for (TransactionDetail detail : details) {
            if (detail.getAmount() != null && (detail.getCategory() == null || detail.getCategory().isAffectsBalance())) {
                amount = amount.add(detail.getAmount());
            }
        }
        return amount;
    }

    public BigDecimal getExpenseAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        for (TransactionDetail detail : details) {
            if (detail.getAmount() != null && detail.getAmount().signum() < 0) {
                amount = amount.add(detail.getAmount());
            }
        }
        return amount;
    }

    public BigDecimal getAssetQuantity() {
        BigDecimal quantity = BigDecimal.ZERO;
        for (TransactionDetail detail : details) {
            if (detail.getAssetQuantity() != null) {
                quantity = quantity.add(detail.getAssetQuantity());
            }
        }
        return quantity;
    }

    public BigDecimal getSecurityPurchasePrice() {
        return getExpenseAmount().divide(getAssetQuantity(), account.getCurrency().getScale(), RoundingMode.HALF_EVEN).abs();
    }

    public BigDecimal getSecuritySalePrice() {
        return getAmount().divide(getAssetQuantity(), account.getCurrency().getScale(), RoundingMode.HALF_EVEN).abs();
    }

    public Transaction getTransfer(Account transferAccount, TransactionGroup group) {
        for (TransactionDetail detail : details) {
            TransactionDetail relatedDetail = detail.getRelatedDetail();
            if (relatedDetail != null && relatedDetail.getTransaction().getAccount().getId().equals(transferAccount.getId())
                    && Objects.equals(detail.getGroup(), group)) {
                return relatedDetail.getTransaction();
            }
        }
        return null;
    }

    public boolean isInvalid() {
        return details.stream().anyMatch(TransactionDetail::isInvalid);
    }

    public boolean isUnsavedAndEmpty() {
        return id == null && details.stream().allMatch(TransactionDetail::isEmpty);
    }

    public boolean isSavedOrNonempty() {
        return ! isUnsavedAndEmpty();
    }

    public String toString() {
        return String.format("Transaction[id=%d]", id);
    }
}