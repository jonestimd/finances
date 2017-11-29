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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.util.Collections.*;

public class CachingStockQuoteService implements StockQuoteService {
    private final StockQuoteService delegate;
    private final Map<String, CacheEntry> pricesCache = new HashMap<>();

    public CachingStockQuoteService(StockQuoteService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<String, BigDecimal> getPrices(Collection<String> symbols) throws IOException {
        Map<Boolean, List<String>> cached = symbols.stream().distinct().collect(Collectors.groupingBy(pricesCache::containsKey));
        Map<String, BigDecimal> prices = getCachedPrices(cached.getOrDefault(true, emptyList()));
        if (cached.get(false) != null) prices.putAll(loadCache(cached.get(false)));
        return prices;
    }

    private Map<String, BigDecimal> getCachedPrices(Collection<String> symbols) {
        return symbols.stream().map(pricesCache::get).filter(CacheEntry::nonEmpty)
                .collect(Collectors.toMap(CacheEntry::getSymbol, CacheEntry::getPrice));
    }

    private Map<String, BigDecimal> loadCache(Collection<String> symbols) throws IOException {
        Map<String, BigDecimal> prices = delegate.getPrices(symbols);
        prices.entrySet().stream().map(CacheEntry::new).forEach(entry -> pricesCache.put(entry.getSymbol(), entry));
        symbols.removeAll(prices.keySet());
        symbols.forEach(symbol -> pricesCache.put(symbol, new CacheEntry(symbol, null)));
        return prices;
    }

    private static class CacheEntry {
        private final String symbol;
        private final BigDecimal price;

        private CacheEntry(Entry<String, BigDecimal> entry) {
            this(entry.getKey(), entry.getValue());
        }

        private CacheEntry(String symbol, BigDecimal price) {
            this.symbol = symbol;
            this.price = price;
        }

        public String getSymbol() {
            return symbol;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public boolean nonEmpty() {
            return price != null;
        }
    }
}
