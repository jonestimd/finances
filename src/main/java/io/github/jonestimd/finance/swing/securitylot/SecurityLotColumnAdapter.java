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
package io.github.jonestimd.finance.swing.securitylot;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class SecurityLotColumnAdapter<V> extends FunctionColumnAdapter<SecurityLot, V> {
    private SecurityLotColumnAdapter(String columnId, Class<V> valueType, Function<SecurityLot, V> getter, BiConsumer<SecurityLot, V> setter) {
        super(LABELS.get(), "table.securityLots.", columnId, valueType, getter, setter);
    }

    public static final SecurityLotColumnAdapter<Date> PURCHASE_DATE_ADAPTER =
            new SecurityLotColumnAdapter<>("purchaseDate", Date.class, SecurityLot::getPurchaseDate, null);


    public static final SecurityLotColumnAdapter<BigDecimal> PURCHASE_SHARES_ADAPTER =
            new SecurityLotColumnAdapter<>("purchaseShares", BigDecimal.class, SecurityLot::getTotalPurchaseShares, null);

    public static final SecurityLotColumnAdapter<BigDecimal> PURCHASE_PRICE_ADAPTER =
            new SecurityLotColumnAdapter<>("purchasePrice", BigDecimal.class, SecurityLot::getPurchasePrice, null);

    public static final SecurityLotColumnAdapter<BigDecimal> AVAILABLE_SHARES_ADAPTER =
            new SecurityLotColumnAdapter<>("availableShares", BigDecimal.class, SecurityLot::getRemainingPurchaseShares, null);

    public static final ValidatedColumnAdapter<SecurityLot, BigDecimal> ALLOCATED_SHARES_ADAPTER =
            new ValidatedSecurityLotColumnAdapter<BigDecimal>("allocatedShares", BigDecimal.class, SecurityLot::getAdjustedShares, SecurityLot::setAdjustedShares) {
                @Override
                public String validate(int selectedIndex, BigDecimal value, List<? extends SecurityLot> beans) {
                    BigDecimal availableShares = beans.get(selectedIndex).getRemainingPurchaseShares();
                    return availableShares.signum() < 0 ? LABELS.getString("table.securityLots.allocatedShares.invalid") : null;
                }
            };

    private static abstract class ValidatedSecurityLotColumnAdapter<V> extends SecurityLotColumnAdapter<V> implements ValidatedColumnAdapter<SecurityLot, V> {
        public ValidatedSecurityLotColumnAdapter(String columnId, Class<V> valueType, Function<SecurityLot, V> getter, BiConsumer<SecurityLot, V> setter) {
            super(columnId, valueType, getter, setter);
        }
    }
}