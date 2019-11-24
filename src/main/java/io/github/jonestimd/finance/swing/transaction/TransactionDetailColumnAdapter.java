// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class TransactionDetailColumnAdapter<V> extends FunctionColumnAdapter<TransactionDetail, V> {
    private static abstract class ValidatedDetailColumnAdapter<V> extends TransactionDetailColumnAdapter<V> implements ValidatedColumnAdapter<TransactionDetail, V> {
        private ValidatedDetailColumnAdapter(String resourcePrefix, Class<V> valueType, Function<TransactionDetail, V> getter, BiConsumer<TransactionDetail, V> setter) {
            super(resourcePrefix, valueType, getter, setter);
        }
    }

    private TransactionDetailColumnAdapter(String columnId, Class<V> valueType, Function<TransactionDetail, V> getter, BiConsumer<TransactionDetail, V> setter) {
        super(LABELS.get(), "table.transaction.detail.", columnId, valueType, getter, setter);
    }

    public static final ValidatedColumnAdapter<TransactionDetail, BigDecimal> AMOUNT_ADAPTER =
        new ValidatedDetailColumnAdapter<BigDecimal>(TransactionDetail.AMOUNT, BigDecimal.class, TransactionDetail::getAmount, TransactionDetail::setAmount) {
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

    public static final ValidatedColumnAdapter<TransactionDetail, BigDecimal> SHARES_ADAPTER =
        new ValidatedDetailColumnAdapter<BigDecimal>("shares", BigDecimal.class, TransactionDetail::getAssetQuantity, TransactionDetail::setAssetQuantity) {
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

    public static final TransactionDetailColumnAdapter<Date> TRANSACTION_DATE_ADAPTER =
        new TransactionDetailColumnAdapter<>("transaction.date", Date.class, td -> td.getTransaction().getDate(), null);

    public static final TransactionDetailColumnAdapter<Payee> TRANSACTION_PAYEE_ADAPTER =
        new TransactionDetailColumnAdapter<>("transaction.payee", Payee.class, td -> td.getTransaction().getPayee(), null);

    public static final TransactionDetailColumnAdapter<Account> TRANSACTION_ACCOUNT_ADAPTER =
        new TransactionDetailColumnAdapter<>("transaction.account", Account.class, td -> td.getTransaction().getAccount(), null);

    public static final TransactionDetailColumnAdapter<Security> TRANSACTION_SECURITY_ADAPTER =
        new TransactionDetailColumnAdapter<>("transaction.security", Security.class, td -> td.getTransaction().getSecurity(), null);

    public static final TransactionDetailColumnAdapter<String> TRANSACTION_MEMO_ADAPTER =
        new TransactionDetailColumnAdapter<>("transaction.memo", String.class, td -> td.getTransaction().getMemo(), null);
}