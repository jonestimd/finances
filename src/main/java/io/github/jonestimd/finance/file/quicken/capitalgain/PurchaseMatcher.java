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
package io.github.jonestimd.finance.file.quicken.capitalgain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.util.MessageHelper;

/**
 * Assigns purchases to {@link SecurityLot}s based on records from a Quicken capital gains report.
 * @see CapitalGainImport
 * @see CapitalGainColumn
 * @see SaleMatcher
 */
public class PurchaseMatcher {
    private static final BigDecimal PURCHASE_SHARES_ERROR = new BigDecimal("0.0005");
    private static final BigDecimal PURCHASE_PRICE_ERROR = new BigDecimal("0.5");
    private static Comparator<TransactionDetail> REMAINING_SHARES_DESCENDING = (p1, p2) -> p2.getRemainingShares().compareTo(p1.getRemainingShares());
    private static Ordering<Entry<CapitalGain, ?>> SHARES_DESCENDING = Ordering.from(new Comparator<Entry<CapitalGain, ?>>() {
        public int compare(Entry<CapitalGain, ?> o1, Entry<CapitalGain, ?> o2) {
            try {
                return o2.getKey().getShares().compareTo(o1.getKey().getShares());
            }
            catch (QuickenException e) {
                throw new RuntimeException(e);
            }
        }
    });
    private final MessageHelper messageHelper = new MessageHelper(BundleType.MESSAGES.get(), PurchaseMatcher.class);
    private TransactionService transactionService;
    private Set<SecurityLot> saleLots = new HashSet<>();

    public PurchaseMatcher(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public Set<SecurityLot> assignPurchases(Map<CapitalGain, SecurityLot> recordLotMap) throws QuickenException {
        for (Entry<PurchaseKey, Collection<CapitalGain>> dateEntry : buildPurchaseMap(recordLotMap).entrySet()) {
            List<TransactionDetail> purchases = transactionService.findPurchasesWithRemainingLots(dateEntry.getKey().account, dateEntry.getKey().security, dateEntry.getKey().purchaseDate);
            assignPurchasesToLots(Maps.filterKeys(recordLotMap, Predicates.in(dateEntry.getValue())), purchases);
        }
        return saleLots;
    }

    private Map<PurchaseKey, Collection<CapitalGain>> buildPurchaseMap(Map<CapitalGain, SecurityLot> recordLotMap) throws QuickenException {
        Multimap<PurchaseKey, CapitalGain> purchaseMap = ArrayListMultimap.create();
        for (Entry<CapitalGain, SecurityLot> entry : recordLotMap.entrySet()) {
            purchaseMap.put(new PurchaseKey(entry.getValue().getSale(), entry.getKey().getPurchaseDate()), entry.getKey());
        }
        return purchaseMap.asMap();
    }

    private void assignPurchasesToLots(Map<CapitalGain, SecurityLot> recordMap, List<TransactionDetail> purchases) throws QuickenException {
        for (Entry<CapitalGain, SecurityLot> entry : SHARES_DESCENDING.sortedCopy(recordMap.entrySet())) {
            List<TransactionDetail> samePrice = Lists.newArrayList(Iterables.filter(purchases, new SamePrice(entry.getKey(), entry.getValue().getSecurity())));
            Collections.sort(samePrice, REMAINING_SHARES_DESCENDING);
            BigDecimal adjustedShares = adjustShares(entry.getKey(), entry.getValue().getSecurity());
            if (! findExactMatch(entry.getKey(), adjustedShares, samePrice, entry.getValue()) && ! findRemainingShares(entry.getKey(), adjustedShares, samePrice, entry.getValue())) {
                messageHelper.infoLocalized("missingPurchase", entry.getValue().getSecurity().getName(), entry.getKey().getPurchaseDate());
                saleLots.add(entry.getValue());
            }
            samePrice.forEach(purchase -> saleLots.add(new SecurityLot(purchase, entry.getValue().getSale(), BigDecimal.ZERO)));
        }
    }

    private boolean findExactMatch(CapitalGain txfRecord, BigDecimal adjustedShares, Collection<TransactionDetail> purchases, SecurityLot lot) throws QuickenException {
        for (TransactionDetail purchase : purchases) {
            if (purchase.getRemainingShares().subtract(adjustedShares).abs().compareTo(PURCHASE_SHARES_ERROR) <= 0) {
                setPurchase(lot, txfRecord, purchase);
                purchases.remove(purchase);
                return true;
            }
        }
        return false;
    }

    private boolean findRemainingShares(CapitalGain txfRecord, BigDecimal adjustedShares, Collection<TransactionDetail> purchases, SecurityLot lot) throws QuickenException {
        for (TransactionDetail purchase : purchases) {
            if (purchase.getRemainingShares().add(PURCHASE_SHARES_ERROR).compareTo(adjustedShares) >= 0) {
                setPurchase(lot, txfRecord, purchase);
                purchases.remove(purchase);
                return true;
            }
        }
        return false;
    }

    private static BigDecimal adjustShares(CapitalGain txfRecord, Security security) throws QuickenException {
        return security.revertSplits(txfRecord.getShares(), txfRecord.getPurchaseDate(), txfRecord.getSellDate());
    }

    private void setPurchase(SecurityLot lot, CapitalGain record, TransactionDetail purchase) throws QuickenException {
        saleLots.add(lot);
        lot.setPurchase(purchase);
        if (lot.getSaleShares() == null) {
            BigDecimal purchaseShares = getPurchaseShares(lot, record);
            if (isWithinPurchaseSharesError(purchaseShares, purchase.getRemainingShares())) {
                purchaseShares = purchase.getRemainingShares();
            }
            lot.setSaleShares(lot.getSecurity().applySplits(purchaseShares, record.getPurchaseDate(), record.getSellDate()));
        }
    }

    private static boolean isWithinPurchaseSharesError(BigDecimal shares1, BigDecimal shares2) {
        return shares1.subtract(shares2).abs().compareTo(PURCHASE_SHARES_ERROR) <= 0;
    }

    private static BigDecimal getPurchaseShares(SecurityLot lot, CapitalGain record) throws QuickenException {
        Security security = lot.getSecurity();
        BigDecimal saleShares = lot.getSaleShares() == null ? record.getShares() : lot.getSaleShares();
        return security.revertSplits(saleShares, record.getPurchaseDate(), record.getSellDate());
    }

    private static class PurchaseKey {
        private final Account account;
        private final Security security;
        private final Date purchaseDate;
        private final List<?> keyValues;

        @SuppressWarnings("unchecked")
        public PurchaseKey(TransactionDetail detail, Date purchaseDate) {
            this.account = detail.getTransaction().getAccount();
            this.security = detail.getTransaction().getSecurity();
            this.purchaseDate = purchaseDate;
            keyValues = Arrays.asList(account, security, purchaseDate);
        }

        public boolean equals(Object obj) {
            PurchaseKey that = (PurchaseKey) obj;
            return keyValues.equals(that.keyValues);
        }

        public int hashCode() {
            return keyValues.hashCode();
        }
    }

    private static class SamePrice implements Predicate<TransactionDetail> {
        private final BigDecimal price;

        public SamePrice(CapitalGain record, Security security) throws QuickenException {
            price = record.getCostBasis().divide(adjustShares(record, security), RoundingMode.HALF_EVEN);
        }

        public boolean apply(TransactionDetail input) {
            return input.getAmount().compareTo(BigDecimal.ZERO) == 0 || isSamePrice(input);
        }

        private boolean isSamePrice(TransactionDetail purchase) {
            BigDecimal purchasePrice = purchase.getTransaction().getSecurityPurchasePrice();
            return purchasePrice.subtract(price).abs().compareTo(PURCHASE_PRICE_ERROR) <= 0;
        }
    }
}