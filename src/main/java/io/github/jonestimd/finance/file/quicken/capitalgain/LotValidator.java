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

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.securitylot.LotAllocationDialog;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class LotValidator {
    private static final String INCOMPLETE_LOTS = "import.capitalgain.incompleteLots";
    private final Logger logger = Logger.getLogger(getClass());
    private final LotAllocationDialog lotAllocationDialog;

    public LotValidator(LotAllocationDialog lotAllocationDialog) {
        this.lotAllocationDialog = lotAllocationDialog;
        logger.setResourceBundle(BundleType.MESSAGES.get());
    }

    public void validateLots(Collection<SecurityLot> saleLots) throws QuickenException {
        Collection<SaleAccumulator> accumulators = createSaleAccumulators(saleLots);
        while (! validateSaleLots(accumulators, saleLots) || ! validatePurchaseLots(saleLots));
    }

    private Collection<SaleAccumulator> createSaleAccumulators(Collection<SecurityLot> lots) {
        Multimap<TransactionDetail, SecurityLot> saleLots = Multimaps.index(lots, SecurityLot::getSale);
        List<SaleAccumulator> saleAccumulators = new ArrayList<>(saleLots.asMap().size());
        for (Entry<TransactionDetail, Collection<SecurityLot>> entry : saleLots.asMap().entrySet()) {
            if (isAllPurchasesAssigned(entry.getValue())) {
                saleAccumulators.add(new SaleAccumulator(entry.getKey(), entry.getValue()));
            }
            else {
                Transaction transaction = entry.getKey().getTransaction();
                logInfo(INCOMPLETE_LOTS, transaction.getSecurity().getName(), transaction.getDate());
                lots.removeAll(entry.getValue());
            }
        }
        return saleAccumulators;
    }

    private void logInfo(String messageKey, Object ... args) {
        logger.l7dlog(Level.INFO, messageKey, args, null);
    }

    private boolean validateSaleLots(Collection<SaleAccumulator> accumulators, Collection<SecurityLot> saleLots) throws QuickenException {
        for (SaleAccumulator accumulator : accumulators) {
            if (accumulator.getUnallocatedShares().compareTo(BigDecimal.ZERO) != 0
                    && ! showLotAllocationDialog(accumulator, Collections2.filter(saleLots, Predicates.in(accumulator.getLots())))) {
                // TODO chicken test?
                saleLots.clear();
                accumulators.clear();
                return false;
            }
        }
        return true;
    }

    private boolean showLotAllocationDialog(final SaleAccumulator accumulator, final Collection<SecurityLot> saleLots) throws QuickenException {
        try {
            SwingUtilities.invokeAndWait(() -> lotAllocationDialog.show(accumulator.getSale(), saleLots));
        }
        catch (InterruptedException ex) {
            throw new QuickenException("unexpectedException", ex);
        }
        catch (InvocationTargetException ex) {
            throw new QuickenException("unexpectedException", ex.getTargetException());
        }
        return ! lotAllocationDialog.isCancelled();
    }

    private boolean isAllPurchasesAssigned(Collection<SecurityLot> lots) {
        for (SecurityLot lot : lots) {
            if (lot.getPurchase() == null) {
                return false;
            }
        }
        return true;
    }

    private boolean validatePurchaseLots(Collection<SecurityLot> lots) throws QuickenException {
        for (Entry<TransactionDetail, Collection<SecurityLot>> entry : Multimaps.index(lots, SecurityLot::getPurchase).asMap().entrySet()) {
            BigDecimal assetQuantity = entry.getKey().getAssetQuantity().abs();
            BigDecimal totalShares = getShares(entry.getValue(), SecurityLot::getPurchaseShares);
            if (assetQuantity.compareTo(totalShares) < 0) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal getShares(Collection<SecurityLot> lots, Function<SecurityLot, BigDecimal> sharesFunction) {
        BigDecimal total = BigDecimal.ZERO;
        for (SecurityLot lot : lots) {
            total = total.add(sharesFunction.apply(lot));
        }
        return total;
    }

    public class SaleAccumulator {
        private final TransactionDetail sale;
        private final List<SecurityLot> lots = new ArrayList<>();
        private BigDecimal unallocatedShares;

        public SaleAccumulator(TransactionDetail sale, Collection<SecurityLot> lots) {
            this.sale = sale;
            unallocatedShares = sale.getAssetQuantity().abs();
            for (SecurityLot lot : lots) {
                addLot(lot);
            }
        }

        public TransactionDetail getSale() {
            return sale;
        }

        public List<SecurityLot> getLots() {
            return Collections.unmodifiableList(lots);
        }

        private BigDecimal getShares(SecurityLot lot) {
            return lot.getSaleShares() == null ? BigDecimal.ZERO : lot.getSaleShares();
        }

        public void addLot(SecurityLot lot) {
            lots.add(lot);
            unallocatedShares = unallocatedShares.subtract(getShares(lot));
        }

        public BigDecimal getUnallocatedShares() {
            return unallocatedShares;
        }
    }
}