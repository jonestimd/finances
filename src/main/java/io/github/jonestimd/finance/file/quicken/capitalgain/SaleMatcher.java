// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.jonestimd.collection.BigDecimals;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.subset.SubsetSum;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Creates {@link SecurityLot}s based on records from a Quicken capital gains report.
 * @see CapitalGainImport
 * @see CapitalGainColumn
 * @see PurchaseMatcher
 */
public class SaleMatcher {
    private static final BigDecimal IMPORT_SHARES_ERROR = new BigDecimal("0.0005");
    private static final BigDecimal IMPORT_AMOUNT_ERROR = new BigDecimal("0.005");
    private static final String MESSAGE_PREFIX = "import.capitalGains";
    private static final String MISSING_SALE = MESSAGE_PREFIX + ".missingSale";
    private static final String NO_LOTS_FOR_SALE = MESSAGE_PREFIX + ".noLotsForSale";
    private final Logger logger = Logger.getLogger(getClass());
    private static final Comparator<Collection<?>> SIZE_ASCENDING = Comparator.comparingInt(Collection::size);

    private Map<CapitalGain, SecurityLot> recordLotMap = new HashMap<>();
    private TransactionService transactionService;

    public SaleMatcher(TransactionService transactionService) {
        this.transactionService = transactionService;
        logger.setResourceBundle(BundleType.MESSAGES.get());
    }

    public Map<CapitalGain, SecurityLot> assignSales(Map<String, Map<Date, List<CapitalGain>>> saleMap) throws QuickenException {
        for (Entry<String, Map<Date, List<CapitalGain>>> securityEntry : saleMap.entrySet()) {
            logDebug("sales for {0}: {1}", securityEntry.getKey(), securityEntry.getValue().size());
            for (Entry<Date, List<CapitalGain>> dateEntry : securityEntry.getValue().entrySet()) {
                logDebug("  {0,date,MM/dd/yyyy}: {1} lots", dateEntry.getKey(), dateEntry.getValue().size());
                List<TransactionDetail> salesWithoutLots = transactionService.findSecuritySalesWithoutLots(securityEntry.getKey(), dateEntry.getKey());
                if (salesWithoutLots.isEmpty()) {
                    logInfo(MISSING_SALE, securityEntry.getKey(), dateEntry.getKey());
                }
                else {
                    moveZeroAmountsToEnd(salesWithoutLots);
                    for (TransactionDetail sale : salesWithoutLots) {
                        createSecurityLots(dateEntry.getValue(), sale);
                    }
                }
            }
        }
        return recordLotMap;
    }

    private void logInfo(String messageKey, Object ... args) {
        logger.l7dlog(Level.INFO, messageKey, args, null);
    }

    private void logDebug(String messageFormat, Object ...args) {
        if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format(messageFormat, args));
        }
    }

    private void moveZeroAmountsToEnd(List<TransactionDetail> sales) {
        for (int i = 0,end = sales.size() - 1; i < end;) {
            if (BigDecimal.ZERO.compareTo(sales.get(i).getAmount()) == 0) {
                sales.add(sales.remove(i));
                end--;
            }
            else {
                i++;
            }
        }
    }

    private void createSecurityLots(List<CapitalGain> txfRecords, TransactionDetail sale) throws QuickenException {
        List<CapitalGain> saleLots = getLots(txfRecords, sale);
        if (saleLots.isEmpty()) { // TODO display dialog?
            Transaction transaction = sale.getTransaction();
            logInfo(NO_LOTS_FOR_SALE, transaction.getSecurity().getName(), transaction.getDate());
        }
        for (CapitalGain record : saleLots) {
            SecurityLot lot = new SecurityLot();
            lot.setSale(sale);
            if (saleLots.size() == 1) {
                lot.setSaleShares(sale.getAssetQuantity().negate());
            }
            recordLotMap.put(record, lot);
        }
        txfRecords.removeAll(saleLots);
    }

    private List<CapitalGain> getLots(Collection<CapitalGain> dateLots, TransactionDetail sale) throws QuickenException {
        BigDecimal assetQuantity = sale.getAssetQuantity().negate();
        boolean zeroAmount = BigDecimal.ZERO.compareTo(sale.getAmount()) == 0;
        List<List<CapitalGain>> subsets = SubsetSum.subsets(assetQuantity, IMPORT_SHARES_ERROR, dateLots, CapitalGain.SHARES_ADAPTER, -1);
        subsets.sort(SIZE_ASCENDING);
        for (List<CapitalGain> subset : subsets) {
            if (zeroAmount || isSaleAmountEqual(subset, sale.getTransaction().getAmount())) {
                return subset;
            }
        }
        return Collections.emptyList();
    }

    private boolean isSaleAmountEqual(Collection<CapitalGain> records, BigDecimal target) {
        return isMatch(BigDecimals.sum(records, CapitalGain.SALE_AMOUNT_ADAPTER), target, IMPORT_AMOUNT_ERROR.multiply(BigDecimal.valueOf(records.size())));
    }

    private boolean isMatch(BigDecimal value, BigDecimal target, BigDecimal tolerance) {
        return value.subtract(target).abs().compareTo(tolerance) <= 0;
    }
}