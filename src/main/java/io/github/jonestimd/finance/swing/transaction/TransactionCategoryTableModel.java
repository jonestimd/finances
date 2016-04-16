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

import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.table.model.ColumnAdapter;

public class TransactionCategoryTableModel extends TransactionSummaryTableModel<TransactionCategory, TransactionCategorySummary> {
    private static final List<ColumnAdapter<? super TransactionCategorySummary, ?>> ADAPTERS = Arrays.<ColumnAdapter<? super TransactionCategorySummary, ?>>asList(
        TransactionCategoryColumnAdapter.INCOME_ADAPTER,
        TransactionCategoryColumnAdapter.SECURITY_ADAPTER,
        TransactionCategoryColumnAdapter.AMOUNT_TYPE_ADAPTER,
        TransactionCategoryColumnAdapter.KEY_ADAPTER,
        TransactionCategoryColumnAdapter.DESCRIPTION_ADAPTER,
        TransactionSummaryColumnAdapter.COUNT_ADAPTER);
    public static final int CODE_INDEX = ADAPTERS.indexOf(TransactionCategoryColumnAdapter.KEY_ADAPTER);

    public TransactionCategoryTableModel(DomainEventPublisher domainEventPublisher) {
        super(ADAPTERS, TransactionCategory.class, domainEventPublisher);
    }

    public void fireTableCellUpdated(int row, int column) {
        super.fireTableCellUpdated(row, column);
        if (column == CODE_INDEX) {
            for (int i = 0; i < getRowCount(); i++) {
                validateCell(i, CODE_INDEX);
                super.fireTableCellUpdated(i, CODE_INDEX);
            }
        }
    }
}