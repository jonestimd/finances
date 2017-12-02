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

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

public class StockQuote {
    public enum QuoteStatus {
        PENDING("Pending..."),
        SUCCESSFUL("Successful"),
        NOT_AVAILABLE("Not available");

        public final String value;

        QuoteStatus(String value) {
            this.value = value;
        }
    }

    public static StockQuote pending(String symbol) {
        return new StockQuote(symbol, QuoteStatus.PENDING, null);
    }

    public static StockQuote notAvailable(String symbol) {
        return new StockQuote(symbol, QuoteStatus.NOT_AVAILABLE, null);
    }

    public static Stream<StockQuote> fromMap(Map<String, BigDecimal> prices) {
        return prices.entrySet().stream().map(entry -> new StockQuote(entry.getKey(), entry.getValue()));
    }

    private final String symbol;
    private final QuoteStatus status;
    private final BigDecimal price;

    public StockQuote(String symbol, BigDecimal price) {
        this(symbol, QuoteStatus.SUCCESSFUL, price);
    }

    protected StockQuote(String symbol, QuoteStatus status, BigDecimal price) {
        this.symbol = symbol;
        this.price = price;
        this.status = status;
    }

    public String getSymbol() {
        return symbol;
    }

    public String formatValue() {
        return status == QuoteStatus.SUCCESSFUL ? price.toString() :
                "<html><font color=\"red\">" + status.name().toLowerCase() + "</font></html>";
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
