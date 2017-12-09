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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Requests security quotes from a series of services.  The services are tried in order and any quotes not
 * returned by a service are requested from the next service in the list.
 */
public class HierarchicalQuoteService implements StockQuoteService {
    private final List<? extends StockQuoteService> quoteServices;

    public HierarchicalQuoteService(List<? extends StockQuoteService> quoteServices) {
        this.quoteServices = quoteServices;
    }

    @Override
    public void getPrices(Collection<String> symbols, Callback callback) throws IOException {
        Set<String> remaining = new HashSet<>(symbols);
        for (StockQuoteService quoteService : quoteServices) {
            quoteService.getPrices(new HashSet<>(remaining), (prices, sourceUrl, sourceIcon, sourceMessage) -> {
                remaining.removeAll(prices.keySet());
                callback.accept(prices, sourceUrl, sourceIcon, sourceMessage);
            });
            if (remaining.isEmpty()) break;
        }
    }
}
