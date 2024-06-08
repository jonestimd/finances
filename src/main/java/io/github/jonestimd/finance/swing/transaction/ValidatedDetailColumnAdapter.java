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
package io.github.jonestimd.finance.swing.transaction;

import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.github.jonestimd.finance.swing.BundleType.LABELS;

public abstract class ValidatedDetailColumnAdapter<V> extends TransactionDetailColumnAdapter<V> implements ValidatedColumnAdapter<TransactionDetail, V> {
    public static final ValidatedDetailColumnAdapter<TransactionType> TYPE_ADAPTER =
            new ValidatedDetailColumnAdapter<TransactionType>("type", TransactionType.class, TransactionDetail::getTransactionType, TransactionDetail::setTransactionType) {
                private final String invalidAccount = LABELS.getString("validation.transactionDetail.invalidSecurityAccount");

                @Override
                public String validate(int selectedIndex, TransactionType propertyValue, List<? extends TransactionDetail> beans) {
                    TransactionDetail detail = beans.get(selectedIndex);
                    if (detail.isTransfer() && detail.getAssetQuantity() != null) {
                        if (!detail.getTransferAccount().getType().isSecurity()) return invalidAccount;
                    }
                    return null;
                }
            };
    public static final ValidatedDetailColumnAdapter<BigDecimal> AMOUNT_ADAPTER =
        new ValidatedDetailColumnAdapter<BigDecimal>(TransactionDetail.AMOUNT, BigDecimal.class, TransactionDetail::getAmount, TransactionDetail::setAmount) {
            private final String requiredMessage = LABELS.getString("validation.transactionDetail.amountRequired");

            public String validate(int selectedIndex, BigDecimal propertyValue, List<? extends TransactionDetail> beans) {
                TransactionDetail detail = beans.get(selectedIndex);
                return !detail.isEmpty() && detail.getAmount() == null ? requiredMessage : null;
            }
        };
    public static final ValidatedDetailColumnAdapter<BigDecimal> SHARES_ADAPTER =
        new ValidatedDetailColumnAdapter<BigDecimal>("shares", BigDecimal.class, TransactionDetail::getAssetQuantity, TransactionDetail::setAssetQuantity) {
            private final String requiredMessage = LABELS.getString("validation.transactionDetail.sharesRequired");
            private final String invalidMessage = LABELS.getString("validation.transactionDetail.invalidShares");
            private final String securityRequiredMessage = LABELS.getString("validation.transactionDetail.securityRequired");
            private final String notAllowedMessage = LABELS.getString("validation.transactionDetail.sharesNotAllowed");

            @Override
            public String validate(int selectedIndex, BigDecimal value, List<? extends TransactionDetail> beans) {
                TransactionDetail detail = beans.get(selectedIndex);
                if (value != null && value.signum() != 0 && !detail.getTransaction().isSecurity()) return securityRequiredMessage;
                SecurityAction action = SecurityAction.forCategory(detail.getCategory());
                if (action != null && action.isSharesRequired()) {
                    if (value == null || value.signum() == 0) return requiredMessage;
                    return !action.isValidShares(value) ? invalidMessage : null;
                }
                return value != null && value.signum() != 0 && !detail.isTransfer() ? notAllowedMessage : null;
            }
        };

    public static final ValidatedDetailColumnAdapter<Date> DATE_ACQUIRED_ADAPTER =
        new ValidatedDetailColumnAdapter<Date>(TransactionDetail.DATE_ACQUIRED, Date.class, TransactionDetail::getDateAcquired, TransactionDetail::setDateAcquired) {
            private final String notAllowedMessage = LABELS.getString("validation.transactionDetail.dateAcquiredNotAllowed");

            @Override
            public String validate(int selectedIndex, Date propertyValue, List<? extends TransactionDetail> beans) {
                if (propertyValue != null) {
                    TransactionDetail detail = beans.get(selectedIndex);
                    if (!SecurityAction.isDateAcquiredAllowed(detail)) return this.notAllowedMessage;
                }
                return null;
            }

            @Override
            public boolean isEditable(TransactionDetail detail) {
                return super.isEditable(detail) && SecurityAction.isDateAcquiredAllowed(detail) || detail.getDateAcquired() != null;
            }
        };

    protected ValidatedDetailColumnAdapter(String resourcePrefix, Class<V> valueType, Function<TransactionDetail, V> getter, BiConsumer<TransactionDetail, V> setter) {
        super(resourcePrefix, valueType, getter, setter);
    }
}
