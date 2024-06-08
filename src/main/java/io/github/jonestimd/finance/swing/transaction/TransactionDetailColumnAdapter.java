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

import java.math.BigDecimal;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import static io.github.jonestimd.finance.swing.BundleType.LABELS;

public class TransactionDetailColumnAdapter<V> extends FunctionColumnAdapter<TransactionDetail, V> {

    protected TransactionDetailColumnAdapter(String columnId, Class<V> valueType, Function<TransactionDetail, V> getter, BiConsumer<TransactionDetail, V> setter) {
        super(LABELS.get(), "table.transaction.detail.", columnId, valueType, getter, setter);
    }

    public static final TransactionDetailColumnAdapter<String> MEMO_ADAPTER =
        new TransactionDetailColumnAdapter<>(TransactionDetail.MEMO, String.class, TransactionDetail::getMemo, TransactionDetail::setMemo);

    public static final TransactionDetailColumnAdapter<TransactionGroup> GROUP_ADAPTER =
        new TransactionDetailColumnAdapter<>(TransactionDetail.GROUP, TransactionGroup.class, TransactionDetail::getGroup, TransactionDetail::setGroup);

    public static final TransactionDetailColumnAdapter<Currency> CURRENCY_ADAPTER =
        new TransactionDetailColumnAdapter<>("currency", Currency.class, row -> (Currency) row.getExchangeAsset(), TransactionDetail::setExchangeAsset);

    public static final TransactionDetailColumnAdapter<NotificationIcon> NOTIFICATION_ADAPTER =
        new TransactionDetailColumnAdapter<>("notification", NotificationIcon.class, detail -> detail.isMissingLots() ? NotificationIcon.MISSING_LOTS : null, null);

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