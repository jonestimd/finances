// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.finance.stockquote;

import java.awt.Color;
import java.awt.Component;
import java.math.BigDecimal;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import io.github.jonestimd.finance.stockquote.StockQuote.QuoteStatus;

public class StockQuoteTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof StockQuote) {
            StockQuote quote = (StockQuote) value;
            if (quote.getStatus() == QuoteStatus.SUCCESSFUL) {
                setHorizontalAlignment(RIGHT);
                setHorizontalTextPosition(LEADING);
                String price = "$" + quote.getPrice().setScale(2, BigDecimal.ROUND_HALF_UP).toString();
                if (quote.getSourceUrl() == null) setValue(price);
                else setValue(String.format("<html><a href=\"%s\">%s</a></html>", quote.getSourceUrl(), price));
                setIcon(quote.getSourceIcon());
                setToolTipText(quote.getSourceMessage());
            }
            else {
                setIcon(null);
                setHorizontalAlignment(CENTER);
                setForeground(Color.gray);
                setValue(quote.getStatus().value);
            }
        }
        return this;
    }
}
