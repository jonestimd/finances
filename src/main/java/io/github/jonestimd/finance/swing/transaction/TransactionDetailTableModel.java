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

import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.swing.table.model.BeanListTableModel;
import io.github.jonestimd.swing.table.model.ColumnAdapter;

public class TransactionDetailTableModel extends BeanListTableModel<TransactionDetail> {
    private static final List<ColumnAdapter<? super TransactionDetail, ?>> ADAPTERS = Arrays.asList(
            TransactionDetailColumnAdapter.TRANSACTION_DATE_ADAPTER,
            TransactionDetailColumnAdapter.TRANSACTION_ACCOUNT_ADAPTER,
            TransactionDetailColumnAdapter.TRANSACTION_PAYEE_ADAPTER,
            TransactionDetailColumnAdapter.GROUP_ADAPTER,
            TransactionDetailColumnAdapter.TYPE_ADAPTER,
            TransactionDetailColumnAdapter.TRANSACTION_MEMO_ADAPTER,
            TransactionDetailColumnAdapter.MEMO_ADAPTER,
            TransactionDetailColumnAdapter.TRANSACTION_SECURITY_ADAPTER,
            TransactionDetailColumnAdapter.SHARES_ADAPTER,
            TransactionDetailColumnAdapter.AMOUNT_ADAPTER
    );

    public TransactionDetailTableModel() {
        super(ADAPTERS);
    }
}
