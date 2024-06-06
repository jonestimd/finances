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

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;

public class SaleLotsTableModel extends ValidatedBeanListTableModel<SecurityLot> {
    public static final int PURCHASE_DATE = 0;
    public static final int AVAILABLE_SHARES = 3;
    public static final int ALLOCATED_SHARES = 4;

    public SaleLotsTableModel() {
        super(ImmutableList.of(
            SecurityLotColumnAdapter.PURCHASE_DATE_ADAPTER,
            SecurityLotColumnAdapter.PURCHASE_SHARES_ADAPTER,
            SecurityLotColumnAdapter.PURCHASE_PRICE_ADAPTER,
            SecurityLotColumnAdapter.AVAILABLE_SHARES_ADAPTER,
            SecurityLotColumnAdapter.ALLOCATED_SHARES_ADAPTER));
    }

    @Override
    public void fireTableCellUpdated(int row, int column) {
        super.fireTableCellUpdated(row, column);
        if (column == ALLOCATED_SHARES) {
            super.fireTableCellUpdated(row, AVAILABLE_SHARES);
        }
    }
}