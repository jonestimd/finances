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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockQuoteQuery {
    private final YqlService yqlService;
    private final JsonMapper<StockQuote> jsonMapper;

    public StockQuoteQuery(YqlService yqlService) throws NoSuchMethodException {
        this.yqlService = yqlService;
        this.jsonMapper = new JsonMapper<StockQuote>(StockQuote.class);
    }

    public Map<String, StockQuote> getQuotes(Collection<String> symbols) throws IOException {
        if (!symbols.isEmpty()) {
            String query = new QueryBuilder(jsonMapper).where(StockQuote.SYMBOL).in(symbols).get();
            List< StockQuote > quotes = yqlService.execute(query, jsonMapper);
            return mapBySymbol(quotes);
        }
        return Collections.emptyMap();
    }

    private Map<String, StockQuote> mapBySymbol(List<StockQuote> quotes) {
        Map<String, StockQuote> bySymbol = new HashMap<String, StockQuote>();
        for (StockQuote quote : quotes) {
            bySymbol.put(quote.getSymbol(), quote);
        }
        return bySymbol;
    }
}
