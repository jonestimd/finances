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
package io.github.jonestimd.finance.yahoo;

import java.math.BigDecimal;

import io.github.jonestimd.finance.yahoo.annotation.JsonType;
import io.github.jonestimd.finance.yahoo.annotation.YahooColumn;
import io.github.jonestimd.finance.yahoo.annotation.YahooTable;

@YahooTable(name = "yahoo.finance.quote", result = "quote")
public class StockQuote {
    public static final StockQuote EMPTY = new StockQuote();
    public static final String SYMBOL = "symbol";
    public static final String LAST_PRICE = "LastTradePriceOnly";
    @YahooColumn(name = StockQuote.SYMBOL, jsonType = JsonType.STRING)
    private String symbol;
    @YahooColumn(name = StockQuote.LAST_PRICE, jsonType = JsonType.BIG_DECIMAL)
    private BigDecimal lastPrice;

    private StockQuote() {}

    public StockQuote(String symbol, BigDecimal lastPrice) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }
}
