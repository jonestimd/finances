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
package io.github.jonestimd.finance.swing;

import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import io.github.jonestimd.swing.HtmlHighlighter;
import io.github.jonestimd.swing.table.Highlighter;
import io.github.jonestimd.swing.table.TableFactory;
import io.github.jonestimd.swing.table.TableInitializer;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FinanceTableFactory extends TableFactory {
    public static final Highlighter HIGHLIGHTER = new HtmlHighlighter(LABELS.getString("filter.highlight.startTag"), LABELS.getString("filter.highlight.endTag"));

    public FinanceTableFactory(TableInitializer tableInitializer) {
        super(tableInitializer);
    }

    public TransactionTable newTransactionTable(TransactionTableModel model) {
        return initialize(new TransactionTable(model));
    }
}