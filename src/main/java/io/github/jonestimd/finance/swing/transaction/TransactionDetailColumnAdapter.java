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
package io.github.jonestimd.finance.swing.transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class TransactionDetailColumnAdapter<V> extends FunctionColumnAdapter<TransactionDetail, V> {
    public static abstract class ValidatedColumnAdapter<V> extends TransactionDetailColumnAdapter<V> implements BeanPropertyValidator<TransactionDetail, V> {
        private ValidatedColumnAdapter(String resourcePrefix, Class<V> valueType, Function<TransactionDetail, V> getter, BiConsumer<TransactionDetail, V> setter) {
            super(resourcePrefix, valueType, getter, setter);
        }
    }

    private TransactionDetailColumnAdapter(String columnId, Class<V> valueType, Function<TransactionDetail, V> getter, BiConsumer<TransactionDetail, V> setter) {
        super(LABELS.get(), "table.transaction.detail.", columnId, valueType, getter, setter);
    }

    public static final TransactionDetailColumnAdapter<BigDecimal> AMOUNT_ADAPTER =
        new ValidatedColumnAdapter<BigDecimal>(TransactionDetail.AMOUNT, BigDecimal.class, TransactionDetail::getAmount, TransactionDetail::setAmount) {
            private final String requiredMessage = LABELS.getString("validation.transactionDetail.amountRequired");

            public String validate(int selectedIndex, BigDecimal propertyValue, List<? extends TransactionDetail> beans) {
                TransactionDetail detail = beans.get(selectedIndex);
                if (! detail.isEmpty() && detail.getAmount() == null) {
                    return requiredMessage;
                }
                return null;
            }
        };

    public static final TransactionDetailColumnAdapter<String> MEMO_ADAPTER =
        new TransactionDetailColumnAdapter<>(TransactionDetail.MEMO, String.class, TransactionDetail::getMemo, TransactionDetail::setMemo);

    public static final TransactionDetailColumnAdapter<TransactionGroup> GROUP_ADAPTER =
        new TransactionDetailColumnAdapter<>(TransactionDetail.GROUP, TransactionGroup.class, TransactionDetail::getGroup, TransactionDetail::setGroup);

    public static final TransactionDetailColumnAdapter<TransactionType> TYPE_ADAPTER =
        new TransactionDetailColumnAdapter<>("type", TransactionType.class, TransactionDetail::getTransactionType, TransactionDetail::setTransactionType);

    public static final TransactionDetailColumnAdapter<Currency> CURRENCY_ADAPTER =
        new TransactionDetailColumnAdapter<>("currency", Currency.class, row -> (Currency) row.getExchangeAsset(), TransactionDetail::setExchangeAsset);

    public static final TransactionDetailColumnAdapter<NotificationIcon> NOTIFICATION_ADAPTER =
        new TransactionDetailColumnAdapter<>("notification", NotificationIcon.class, detail -> detail.isMissingLots() ? NotificationIcon.MISSING_LOTS : null, null);

    public static final TransactionDetailColumnAdapter<BigDecimal> SHARES_ADAPTER =
        new ValidatedColumnAdapter<BigDecimal>("shares", BigDecimal.class, TransactionDetail::getAssetQuantity, TransactionDetail::setAssetQuantity) {
            private final String requiredMessage = LABELS.getString("validation.transactionDetail.sharesRequired");
            private final String invalidMessage = LABELS.getString("validation.transactionDetail.invalidShares");
            private final String notAllowedMessage = LABELS.getString("validation.transactionDetail.sharesNotAllowed");

            @Override
            public String validate(int selectedIndex, BigDecimal value, List<? extends TransactionDetail> beans) {
                TransactionDetail detail = beans.get(selectedIndex);
                SecurityAction action = SecurityAction.forCategory(detail.getCategory());
                if (action != null && action.isSharesRequired()) {
                    if (value == null || value.signum() == 0) return requiredMessage;
                    return ! action.isValidShares(value) ? invalidMessage : null;
                }
                return value != null && value.signum() != 0 ? notAllowedMessage : null;
            }
        };

    public static final TransactionDetailColumnAdapter<BigDecimal> EXCHANGE_QUANTITY_ADAPTER =
        new TransactionDetailColumnAdapter<>(TransactionDetail.ASSET_QUANTITY, BigDecimal.class, TransactionDetail::getAssetQuantity, TransactionDetail::setAssetQuantity);
}