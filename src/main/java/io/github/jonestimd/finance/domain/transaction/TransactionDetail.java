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
package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
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
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.google.common.base.MoreObjects;
import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Asset;
import io.github.jonestimd.finance.domain.asset.Security;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Formula;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "transaction_detail", uniqueConstraints = @UniqueConstraint(name = "related_detail_uk", columnNames = "related_detail_id"))
@SequenceGenerator(name = "id_generator", sequenceName = "transaction_detail_id_seq")
@NamedQueries({
    @NamedQuery(name = TransactionDetail.FIND_ORPHAN_TRANSFERS,
        query = "select td from TransactionDetail td join td.relatedDetail rd where rd.relatedDetail.id is null"),
    @NamedQuery(name = TransactionDetail.SECURITY_SALES_WITHOUT_LOTS, query = "select distinct td " +
        "from TransactionDetail td " +
        "join td." + TransactionDetail.TRANSACTION + " t " +
        "join td." + TransactionDetail.CATEGORY + " type " +
        "join t." + Transaction.SECURITY + " s " +
        "where lower(s." + Security.NAME + ") like lower(:securityName)" +
        " and t." + Transaction.DATE + " = :saleDate" +
        " and type." + TransactionCategory.CODE + " in (:actions)" +
        " and not exists (from SecurityLot l where l." + SecurityLot.SALE + ".id = td.id)"),
    @NamedQuery(name = TransactionDetail.UNSOLD_SECURITY_SHARES_BY_DATE, query =
        "select distinct td " +
        "from TransactionDetail td join td." + TransactionDetail.TRANSACTION + " t join td." + TransactionDetail.CATEGORY + " c left join fetch td.saleLots " +
        "where t." + Transaction.DATE + " = :purchaseDate" +
        " and t." + Transaction.ACCOUNT + " = :account" +
        " and t." + Transaction.SECURITY + " = :security" +
        " and c." + TransactionCategory.CODE + " in (:actions) " +
        " and td." + TransactionDetail.ASSET_QUANTITY +
        " > (select coalesce(sum(l." + SecurityLot.PURCHASE_SHARES + "),0) from SecurityLot l where l." + SecurityLot.PURCHASE + ".id = td.id))"),
    @NamedQuery(name = TransactionDetail.UNSOLD_SECURITY_SHARES, query =
        "select distinct td " +
        "from TransactionDetail td join td." + TransactionDetail.TRANSACTION + " t join td." + TransactionDetail.CATEGORY + " c left join fetch td.saleLots " +
        "where t." + Transaction.DATE + " <= :saleDate" +
        " and t." + Transaction.ACCOUNT + " = :account" +
        " and t." + Transaction.SECURITY + " = :security" +
        " and c." + TransactionCategory.CODE + " in (:actions) " +
        " and td." + TransactionDetail.ASSET_QUANTITY +
        " > (select coalesce(sum(l." + SecurityLot.PURCHASE_SHARES + "),0) from SecurityLot l where l." + SecurityLot.PURCHASE + ".id = td.id))"),
    @NamedQuery(name = TransactionDetail.REPLACE_CATEGORY_QUERY, query =
        "update TransactionDetail set category.id = :newCategoryId where category.id in (:oldCategoryIds)")
})
public class TransactionDetail extends BaseDomain<Long> {
    public static final String FIND_ORPHAN_TRANSFERS = "transactionDetail.findOrphanTransfers";
    public static final String SECURITY_SALES_WITHOUT_LOTS = "transaction.securitySalesWithoutLots";
    public static final String UNSOLD_SECURITY_SHARES_BY_DATE = "transaction.unsoldSecuritySharesByDate";
    public static final String UNSOLD_SECURITY_SHARES = "transaction.unsoldSecurityShares";
    public static final String REPLACE_CATEGORY_QUERY = "transaction.replaceCategory";

    public static final String TRANSACTION = "transaction";
    public static final String AMOUNT = "amount";
    public static final String GROUP = "group";
    public static final String MEMO = "memo";
    public static final String CATEGORY = "category";
    public static final String RELATED_DETAIL = "relatedDetail";
    public static final String EXCHANGE_ASSET = "exchangeAsset";
    public static final String ASSET_QUANTITY = "assetQuantity";

    @Id @GeneratedValue(strategy=GenerationType.AUTO, generator="id_generator")
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="transaction_id", nullable=false)
    @ForeignKey(name = "tx_detail_tx_fk") @Cascade({CascadeType.SAVE_UPDATE, CascadeType.MERGE})
    private Transaction transaction;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @ManyToOne @Cascade(CascadeType.SAVE_UPDATE) @ForeignKey(name="tx_detail_group_fk")
    @JoinColumn(name = "transaction_group_id")
    private TransactionGroup group;
    @Column(name = "memo", length = 2000)
    private String memo;
    @ManyToOne @ForeignKey(name="tx_detail_tx_type_fk") @JoinColumn(name = "transaction_category_id")
    protected TransactionCategory category;
    @ManyToOne @ForeignKey(name="tx_detail_transfer_fk")
    @JoinColumn(name = "related_detail_id") @Cascade({CascadeType.SAVE_UPDATE, CascadeType.MERGE, CascadeType.DELETE})
    protected TransactionDetail relatedDetail;
    @ManyToOne @JoinColumn(name="exchange_asset_id") @ForeignKey(name="tx_asset_fk")
    private Asset exchangeAsset;
    @Column(name = "asset_quantity", precision = 19, scale = 6)
    private BigDecimal assetQuantity;
    @OneToMany(mappedBy="purchase")
    // TODO cascade delete
    private List<SecurityLot> saleLots = new ArrayList<>();
    // not adjusted for splits
    @Transient
    private transient BigDecimal remainingShares;
    @Formula("(select sum(security_lot.sale_shares) from security_lot where id = security_lot.sale_tx_detail_id)")
    private BigDecimal saleLotShares;

    public static TransactionDetail newTransfer(Account account, BigDecimal amount) {
        TransactionDetail detail = new TransactionDetail(amount, null, null);
        detail.getRelatedDetail().setTransaction(new Transaction(account, new Date(), false, null));
        return detail;
    }

    public TransactionDetail() {}

    /**
     * Create a transfer detail and it's related detail.
     */
    public TransactionDetail(BigDecimal amount, String memo, TransactionGroup group) {
        this(null, amount, memo, group);
        relatedDetail = new TransactionDetail(this);
    }

    public TransactionDetail(TransactionCategory type, BigDecimal amount, String memo, TransactionGroup group) {
        this.category = type;
        this.amount = amount;
        this.memo = memo;
        this.group = group;
    }

    /**
     * Create a transfer detail and it's transaction from the related detail.
     */
    public TransactionDetail(Account account, TransactionDetail relatedDetail) {
        this(relatedDetail);
        new Transaction(account, relatedDetail.getTransaction(), this);
    }

    /**
     * Create the related transfer detail.
     */
    private TransactionDetail(TransactionDetail relatedDetail) {
        this(null, nullSafeNegate(relatedDetail.amount), relatedDetail.memo, relatedDetail.group);
        this.relatedDetail = relatedDetail;
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        if (this.transaction != null && this.transaction != transaction) {
            this.transaction.getDetails().remove(this);
        }
        this.transaction = transaction;
        if (transaction != null && ! transaction.getDetails().contains(this)) {
            transaction.getDetails().add(this);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        if (relatedDetail != null) {
            relatedDetail.amount = nullSafeNegate(amount);
        }
    }

    public TransactionGroup getGroup() {
        return group;
    }

    public void setGroup(TransactionGroup group) {
        this.group = group;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Transient
    public boolean isTransfer() {
        return relatedDetail != null;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public TransactionType getTransactionType() {
        return isTransfer() ? relatedDetail.getTransaction().getAccount() : category;
    }

    public void setTransactionType(TransactionType transactionType) {
        if (transactionType == null) {
            setCategory(null);
            setRelatedDetail(null);
        }
        else if (transactionType.isTransfer()) {
            setCategory(null);
            Account account = (Account) transactionType;
            if (isTransfer()) {
                getRelatedDetail().getTransaction().setAccount(account);
            }
            else {
                setRelatedDetail(new TransactionDetail(account, this));
            }
        }
        else {
            setCategory((TransactionCategory) transactionType);
            setRelatedDetail(null);
        }
    }

    public TransactionDetail getRelatedDetail() {
        return relatedDetail;
    }

    public void setRelatedDetail(TransactionDetail relatedDetail) {
        this.relatedDetail = relatedDetail;
    }

    public Asset getExchangeAsset() {
        return exchangeAsset;
    }

    public void setExchangeAsset(Asset exchangeAsset) {
        this.exchangeAsset = exchangeAsset;
    }

    public BigDecimal getAssetQuantity() {
        return assetQuantity;
    }

    public void setAssetQuantity(BigDecimal assetQuantity) {
        this.assetQuantity = assetQuantity;
    }

    public List<SecurityLot> getSaleLots() {
        return saleLots;
    }

    public BigDecimal getRemainingShares() {
        if (remainingShares == null) {
            remainingShares = getAssetQuantity();
            for (SecurityLot lot : saleLots) {
                remainingShares = remainingShares.subtract(lot.getPurchaseShares());
            }
        }
        return remainingShares;
    }

    public void resetRemainingShares() {
        remainingShares = null;
    }

    public BigDecimal getSaleLotShares() {
        return MoreObjects.firstNonNull(saleLotShares, BigDecimal.ZERO);
    }

    public void setSaleLotShares(BigDecimal saleLotShares) {
        this.saleLotShares = saleLotShares;
    }

    public boolean isMissingLots() {
        return category != null && category.getParent() == null && SecurityAction.REQUIRES_LOTS.contains(category.getCode())
                && getAssetQuantity().add(getSaleLotShares()).compareTo(BigDecimal.ZERO) != 0;
    }

    public void addLot(SecurityLot lot) {
        if (saleLots.add(lot) && remainingShares != null) {
            remainingShares = remainingShares.subtract(lot.getPurchaseShares());
        }
    }

    public void removeLot(SecurityLot lot) {
        if (saleLots.remove(lot) && remainingShares != null) {
            remainingShares = remainingShares.add(lot.getPurchaseShares());
        }
    }

    public boolean isEmpty() {
        return category == null && group == null && relatedDetail == null && memo == null && amount == null;
    }

    public boolean isZeroAmount() {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isUnsavedAndEmpty() {
        return id == null && isEmpty();
    }

    public boolean isInvalid() {
        return !isEmpty() && amount == null;
    }

    private static BigDecimal nullSafeNegate(BigDecimal amount) {
        return amount == null ? null : amount.negate();
    }

    public String toString() {
        return String.format("TransactionDetail[id=%d]", id);
    }
}