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
package io.github.jonestimd.finance.swing.transaction;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

public class TransactionColumnAdapter<V> extends FunctionColumnAdapter<Transaction, V> {
    private TransactionColumnAdapter(String columnId, Class<V> valueType, Function<Transaction, V> getter, BiConsumer<Transaction, V> setter) {
        super(BundleType.LABELS.get(), "table.transaction.", columnId, valueType, getter, setter);
    }

    public static final TransactionColumnAdapter<Date> DATE_ADAPTER =
        new TransactionColumnAdapter<>(Transaction.DATE, Date.class, Transaction::getDate, Transaction::setDate);

    public static final TransactionColumnAdapter<String> NUMBER_ADAPTER =
        new TransactionColumnAdapter<>(Transaction.NUMBER, String.class, Transaction::getNumber, Transaction::setNumber);

    public static final TransactionColumnAdapter<String> MEMO_ADAPTER =
        new TransactionColumnAdapter<>(Transaction.MEMO, String.class, Transaction::getMemo, Transaction::setMemo);

    public static final TransactionColumnAdapter<Boolean> CLEARED_ADAPTER =
        new TransactionColumnAdapter<>(Transaction.CLEARED, Boolean.class, Transaction::isCleared, Transaction::setCleared);

    public static final TransactionColumnAdapter<BigDecimal> AMOUNT_ADAPTER =
        new TransactionColumnAdapter<>(Transaction.AMOUNT, BigDecimal.class, Transaction::getAmount, null);

    public static final TransactionColumnAdapter<Payee> PAYEE_ADAPTER =
        new TransactionColumnAdapter<>(Transaction.PAYEE, Payee.class, Transaction::getPayee, Transaction::setPayee);

    public static final ValidatedColumnAdapter<Transaction, Security> SECURITY_ADAPTER =
        new ValidatedTransactionColumnAdapter<Security>(Transaction.SECURITY, Security.class, Transaction::getSecurity, Transaction::setSecurity) {
            private final String requiredMessage = BundleType.LABELS.getString("validation.transaction.securityRequired");

            @Override
            public String validate(int selectedIndex, Security propertyValue, List<? extends Transaction> beans) {
                Transaction transaction = beans.get(selectedIndex);
                return transaction.isSecurityRequired() && propertyValue == null ? requiredMessage : null;
            }
        };

    private static abstract class ValidatedTransactionColumnAdapter<V> extends TransactionColumnAdapter<V> implements ValidatedColumnAdapter<Transaction, V> {
        public ValidatedTransactionColumnAdapter(String columnId, Class<V> valueType, Function<Transaction, V> getter, BiConsumer<Transaction, V> setter) {
            super(columnId, valueType, getter, setter);
        }
    }
}