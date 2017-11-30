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
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;

import com.typesafe.config.Config;
import org.apache.log4j.Logger;

public class QuandlQuoteService implements StockQuoteService {
    public static final StockQuoteServiceFactory FACTORY = new StockQuoteServiceFactory() {
        @Override
        public boolean isEnabled(Config config) {
            return config.hasPath("quandl.apiKey");
        }

        @Override
        public StockQuoteService create(Config config) {
            return new QuandlQuoteService(config.getConfig("quandl"));
        }
    };

    private final Logger logger = Logger.getLogger(getClass());
    private final String urlFormat;
    private final List<String> symbolPath;
    private final List<String> columnsPath;
    private final List<String> tablePath;
    private final String priceColumn;

    public QuandlQuoteService(Config config) {
        this.urlFormat = config.getString("urlFormat").replaceAll("\\$\\{apiKey}", config.getString("apiKey"));
        this.symbolPath = config.getStringList("symbolPath");
        this.columnsPath = config.getStringList("columnsPath");
        this.tablePath = config.getStringList("tablePath");
        this.priceColumn = config.getString("priceColumn");
    }

    @Override
    public Map<String, BigDecimal> getPrices(Collection<String> symbols) throws IOException {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (String symbol : symbols) {
            String url = urlFormat.replaceAll("\\$\\{symbol}", symbol);
            try (InputStream stream = new URL(url).openStream()) {
                prices.putAll(getPrice(stream));
            } catch (Exception ex) {
                logger.warn("error getting price from " + url + ": " + ex.getMessage());
            }
        }
        return prices;
    }

    private Map<String, BigDecimal> getPrice(InputStream stream) {
        JsonHelper helper = new JsonHelper(stream);
        String symbol = helper.getString(symbolPath);
        JsonArray columnNames = helper.getArray(columnsPath);
        JsonArray row = helper.getArray(tablePath).getJsonArray(0);
        for (int i = 0; i < columnNames.size(); i++) {
            if (priceColumn.equals(columnNames.getString(i))) {
                return Collections.singletonMap(symbol, row.getJsonNumber(i).bigDecimalValue());
            }
        }
        throw new IllegalArgumentException("Price not found in " + helper.toString());
    }
}
