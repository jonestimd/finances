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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SplitRatio;
import io.github.jonestimd.lang.Comparables;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "security_lot")
@SequenceGenerator(name = "id_generator", sequenceName = "security_lot_id_seq")
@NamedQueries({
    @NamedQuery(name = SecurityLot.FIND_BY_SALE_ID,
        query = "select distinct lot from SecurityLot lot join fetch lot.purchase p join fetch p.saleLots where lot.sale.id = :saleId"),
    @NamedQuery(name = SecurityLot.DELETE_BY_SALE_ID, query = "delete from SecurityLot lot where lot.sale.id = :saleId")
})
public class SecurityLot extends BaseDomain<Long> {
    public static final String FIND_BY_SALE_ID = "SecurityLot.findBySaleIdWithPurchaseLots";
    public static final String DELETE_BY_SALE_ID = "SecurityLot.deleteSaleLots";
    public static final String PURCHASE = "purchase";
    public static final String SALE = "sale";
    public static final String PURCHASE_SHARES = "purchaseShares";
    public static final String ADJUSTED_SHARES = "adjustedShares";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    private Long id;
    @ManyToOne
    @JoinColumn(name = "purchase_tx_detail_id", foreignKey = @ForeignKey(name = "security_lot_purchase_tx_fk"))
    private TransactionDetail purchase;
    @ManyToOne
    @JoinColumn(name = "related_tx_detail_id", foreignKey = @ForeignKey(name = "security_lot_sale_tx_fk"))
    private TransactionDetail sale;
    @Column(name = "purchase_shares", precision = 19, scale = 6)
    private BigDecimal purchaseShares;
    @Column(name = "adjusted_shares", precision = 19, scale = 6)
    private BigDecimal adjustedShares; // TODO calculate from purchase shares or vice versa

    public SecurityLot() {
    }

    public SecurityLot(TransactionDetail purchase, TransactionDetail sale, BigDecimal saleShares) {
        this.sale = sale;
        this.adjustedShares = saleShares;
        setPurchase(purchase);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionDetail getPurchase() {
        return purchase;
    }

    public void setPurchase(TransactionDetail purchase) {
        if (this.purchase != null) {
            this.purchase.removeLot(this);
        }
        this.purchase = purchase;
        if (purchase != null) {
            if (adjustedShares != null) {
                setPurchaseShares(revertSplits(adjustedShares));
                purchase.addLot(this);
            }
        }
    }

    public TransactionDetail getSale() {
        return sale;
    }

    public void setSale(TransactionDetail sale) {
        this.sale = sale;
    }

    public  boolean isTransfer(Account account) {
        Account xferAccount = sale.getTransferAccount();
        return xferAccount != null && xferAccount.equals(account);
    }

    /**
     * @return lot shares as of purchase date (unadjusted for splits).
     */
    public BigDecimal getPurchaseShares() {
        return purchaseShares;
    }

    private void setPurchaseShares(BigDecimal shares) {
        this.purchaseShares = shares;
        purchase.resetRemainingShares();
    }

    public Date getPurchaseDate() {
        return purchase.getTransaction().getDate();
    }

    public BigDecimal getTotalPurchaseShares() {
        return applySplits(purchase.getAssetQuantity());
    }

    public BigDecimal getRemainingPurchaseShares() {
        if (sale.isAccount(purchase.getAccount())) return applySplits(purchase.getRemainingShares());
        return applySplits(purchase.getRemainingShares(sale.getAccount()));
    }

    /**
     * @return the purchase price adjusted for splits
     */
    public BigDecimal getPurchasePrice() {
        SplitRatio ratio = getSecurity().getSplitRatio(purchase.getTransaction().getDate(), sale.getTransaction().getDate());
        return purchase.getTransaction().getSecurityPurchasePrice().divide(ratio.getRatio(getSecurity().getScale()), RoundingMode.HALF_EVEN);
    }

    /**
     * @return lot shares as of sale date ({@link #purchaseShares} adjusted by splits).
     */
    public BigDecimal getAdjustedShares() {
        return adjustedShares;
    }

    public void setAdjustedShares(BigDecimal shares) {
        this.adjustedShares = shares == null ? BigDecimal.ZERO : shares;
        if (purchase != null && sale != null) {
            setPurchaseShares(revertSplits(adjustedShares));
        }
    }

    public Security getSecurity() {
        return purchase != null ? purchase.getTransaction().getSecurity() : sale.getTransaction().getSecurity();
    }

    /**
     * @return true if there are no shares
     */
    public boolean isEmpty() {
        return getAdjustedShares() == null || BigDecimal.ZERO.compareTo(getAdjustedShares()) == 0;
    }

    /**
     * Allocate remaining purchase shares to the sale.
     * @param maxShares maximum shares to be allocated (adjusted for splits)
     * @return unallocated amount of {@code maxShares}
     */
    public BigDecimal allocateShares(BigDecimal maxShares) {
        BigDecimal allocatedShares = Comparables.min(maxShares, getRemainingPurchaseShares());
        setAdjustedShares(adjustedShares.add(allocatedShares));
        return maxShares.subtract(allocatedShares);
    }

    private BigDecimal applySplits(BigDecimal shares) {
        return purchase.getTransaction().getSecurity().applySplits(shares, purchase.getTransaction().getDate(), sale.getTransaction().getDate());
    }

    private BigDecimal revertSplits(BigDecimal shares) {
        return purchase.getTransaction().getSecurity().revertSplits(shares, purchase.getTransaction().getDate(), sale.getTransaction().getDate());
    }

    public String toString() {
        return "[purchaseShares=" + purchaseShares + ", saleShares=" + adjustedShares + "]";
    }
}