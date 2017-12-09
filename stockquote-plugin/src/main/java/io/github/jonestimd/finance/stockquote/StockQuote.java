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
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.Icon;

public class StockQuote implements Comparable<StockQuote> {
    public enum QuoteStatus {
        PENDING("Pending..."),
        NOT_AVAILABLE("Not available"),
        SUCCESSFUL("Successful");

        public final String value;

        QuoteStatus(String value) {
            this.value = value;
        }
    }

    private static Comparator<BigDecimal> PRICE_COMPARATOR = Comparator.nullsFirst(BigDecimal::compareTo);

    public static StockQuote pending(String symbol) {
        return new StockQuote(symbol, QuoteStatus.PENDING, null, null, null, null);
    }

    public static StockQuote notAvailable(String symbol) {
        return new StockQuote(symbol, QuoteStatus.NOT_AVAILABLE, null, null, null, null);
    }

    public static Stream<StockQuote> fromMap(Map<String, BigDecimal> prices, String sourceUrl, Icon sourceIcon, String sourceMessage) {
        return prices.entrySet().stream().map(entry -> new StockQuote(entry.getKey(), entry.getValue(), sourceUrl, sourceIcon, sourceMessage));
    }

    private final String symbol;
    private final QuoteStatus status;
    private final BigDecimal price;
    private final String sourceUrl;
    private final Icon sourceIcon;
    private final String sourceMessage;

    public StockQuote(String symbol, BigDecimal price, String sourceUrl, Icon sourceIcon, String sourceMessage) {
        this(symbol, QuoteStatus.SUCCESSFUL, price, sourceUrl, sourceIcon, sourceMessage);
    }

    protected StockQuote(String symbol, QuoteStatus status, BigDecimal price, String sourceUrl, Icon sourceIcon, String sourceMessage) {
        this.symbol = symbol;
        this.price = price;
        this.status = status;
        this.sourceUrl = sourceUrl;
        this.sourceIcon = sourceIcon;
        this.sourceMessage = sourceMessage;
    }

    public String getSymbol() {
        return symbol;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Icon getSourceIcon() {
        return sourceIcon;
    }

    public String getSourceMessage() {
        return sourceMessage;
    }

    @Override
    public int compareTo(StockQuote o) {
        if (o == null) return 1;
        if (status != o.status) return status.compareTo(o.status);
        return PRICE_COMPARATOR.compare(this.getPrice(), o.getPrice());
    }
}
