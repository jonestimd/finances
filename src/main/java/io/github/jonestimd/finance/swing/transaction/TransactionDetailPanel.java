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

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.swing.table.DecoratedTable;

public class TransactionDetailPanel extends JPanel {
    private final DecoratedTable<TransactionDetail, TransactionDetailTableModel> table;
    private final TransactionDetailTableModel model = new TransactionDetailTableModel();

    public TransactionDetailPanel(FinanceTableFactory tableFactory, List<TransactionDetail> transactionDetails) {
        model.setBeans(transactionDetails);
        table = tableFactory.createSortedTable(model);
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
}
