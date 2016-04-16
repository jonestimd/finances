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

import java.util.function.Function;

import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class TransactionSummaryColumnAdapter<V> extends FunctionColumnAdapter<TransactionSummary, V> {
    private TransactionSummaryColumnAdapter(String columnId, Class<? super V> valueType, Function<TransactionSummary, V> getter) {
        super(LABELS.get(), "table.transactionSummary.", columnId, valueType, getter, null);
    }

    public static final ColumnAdapter<TransactionSummary, Long> COUNT_ADAPTER =
            new TransactionSummaryColumnAdapter<>(TransactionCategorySummary.TRANSACTION_COUNT, Long.class,
                    TransactionSummary::getTransactionCount);
}
