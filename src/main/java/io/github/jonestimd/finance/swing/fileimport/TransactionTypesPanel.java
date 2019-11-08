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
package io.github.jonestimd.finance.swing.fileimport;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import io.github.jonestimd.finance.domain.fileimport.ImportTransactionType;
import io.github.jonestimd.finance.swing.BorderFactory;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.TableFactory;

public class TransactionTypesPanel extends JPanel {
    private final DecoratedTable<ImportTransactionType, ImportTransactionTypeTableModel> table;

    public TransactionTypesPanel(FileImportsDialog owner, TableFactory tableFactory) {
        super(new BorderLayout(BorderFactory.GAP, BorderFactory.GAP));
        setBorder(BorderFactory.panelBorder());
        setPreferredSize(new Dimension(550, 100));
        table = tableFactory.validatedTableBuilder(new ImportTransactionTypeTableModel()).get();
        setTableModel(owner.getModel());
        add(new JScrollPane(table), BorderLayout.CENTER);
        owner.getModel().addSelectionListener((oldFile, newFile) -> setTableModel(owner.getModel()));
    }

    private void setTableModel(FileImportsModel model) {
        table.setModel(model.getCategoryTableModel());
    }
}
