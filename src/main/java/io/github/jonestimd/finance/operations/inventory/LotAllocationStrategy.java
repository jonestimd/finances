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
package io.github.jonestimd.finance.operations.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Ordering;
import io.github.jonestimd.collection.BigDecimals;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;

public class LotAllocationStrategy {
    private static final Ordering<SecurityLot> PURCHASE_PRICE_ORDERING = Ordering.natural().onResultOf(SecurityLot::getPurchasePrice);
    private static final Ordering<SecurityLot> PURCHASE_DATE_ORDERING = Ordering.natural().onResultOf(SecurityLot::getPurchaseDate);

    public static final LotAllocationStrategy LOWEST_PRICE = new LotAllocationStrategy(PURCHASE_PRICE_ORDERING);
    public static final LotAllocationStrategy HIGHEST_PRICE = new LotAllocationStrategy(PURCHASE_PRICE_ORDERING.reverse());
    public static final LotAllocationStrategy FIRST_IN = new LotAllocationStrategy(PURCHASE_DATE_ORDERING);
    public static final LotAllocationStrategy LAST_IN = new LotAllocationStrategy(PURCHASE_DATE_ORDERING.reverse());

    private final Comparator<SecurityLot> comparator;

    protected LotAllocationStrategy(Comparator<SecurityLot> comparator) {
        this.comparator = comparator;
    }

    public void allocateLots(Collection<SecurityLot> availableLots, BigDecimal shares) {
        BigDecimal remaining = shares.subtract(getAllocatedShares(availableLots));
        Iterator<SecurityLot> iterator = lotIterator(availableLots);
        while (iterator.hasNext() && remaining.signum() > 0) {
            SecurityLot lot = iterator.next();
            if (lot.getRemainingPurchaseShares().signum() > 0) remaining = lot.allocateShares(remaining);
        }
    }

    private Iterator<SecurityLot> lotIterator(Collection<SecurityLot> availableLots) {
        List<SecurityLot> sortedLots = new ArrayList<>(availableLots);
        sortedLots.sort(comparator);
        return sortedLots.iterator();
    }

    private BigDecimal getAllocatedShares(Collection<SecurityLot> availableLots) {
        return BigDecimals.sum(availableLots, SecurityLot::getAdjustedShares);
    }
}