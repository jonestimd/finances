// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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

import java.util.Comparator;

import javax.swing.SortOrder;

import com.google.common.collect.Ordering;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.swing.table.sort.HeaderDetailTableRowSorter;

public class TransactionTableRowSorter extends HeaderDetailTableRowSorter<Transaction, TransactionTableModel> {
    public TransactionTableRowSorter(TransactionTable table) {
        super(table, new TransactionTableRowComparator(table));
    }

    protected static class TransactionTableRowComparator extends HeaderDetailTableRowComparator<Transaction> {
        private final Ordering<HeaderDetailViewToModel<Transaction>> beanIndexOrdering
                = Ordering.from(Comparator.comparingInt(HeaderDetailViewToModel::getBeanIndex));
        private final Comparator<HeaderDetailViewToModel<Transaction>> modelIndexComparator
                = Comparator.comparingInt(HeaderDetailViewToModel::getModelIndex);
        private final TransactionTable table;

        public TransactionTableRowComparator(TransactionTable table) {
            super(table);
            this.table = table;
        }

        @Override
        public int compare(HeaderDetailViewToModel<Transaction> row1, HeaderDetailViewToModel<Transaction> row2) {
            TransactionTableModel model = table.getModel();
            if (row1.getBean(model).getId() == null) {
                if (row2.getBean(model).getId() != null) {
                    return 1;
                }
            }
            else if (row2.getBean(model).getId() == null) {
                return -1;
            }
            return super.compare(row1, row2);
        }

        @Override
        protected Comparator<HeaderDetailViewToModel<Transaction>> columnComparator(SortKey sortKey) {
            if (isDateColumn(sortKey)) {
                return sortKey.getSortOrder() == SortOrder.ASCENDING ? beanIndexOrdering.compound(modelIndexComparator)
                        : beanIndexOrdering.reverse().compound(modelIndexComparator);
            }
            return super.columnComparator(sortKey);
        }

        private boolean isDateColumn(SortKey sortKey) {
            return sortKey.getColumn() == 0;
        }
    }
}