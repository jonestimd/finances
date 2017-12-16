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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.JsonArray;
import javax.swing.Icon;

import com.typesafe.config.Config;
import org.apache.log4j.Logger;

/**
 * Get security prices from Quandl.  Enabled by adding a valid API key in the application configuration.
 * <pre>
 * finances {
 *   stockquote {
 *     quandl {
 *       apiKey = "your API key"
 *     }
 *   }
 * }
 * </pre>
 */
public class QuandlQuoteService implements StockQuoteService {
    public static final StockQuoteServiceFactory FACTORY = new StockQuoteServiceFactory() {
        @Override
        public Optional<StockQuoteService> create(Config config) {
            try {
                return Optional.of(new QuandlQuoteService(config.getConfig("quandl")));
            } catch (Exception ex) {
                logger.warn("Failed to initialize the Quandl service client", ex);
            }
            return Optional.empty();
        }
    };

    private static final Logger logger = Logger.getLogger(QuandlQuoteService.class);
    private final String urlFormat;
    private final Icon icon;
    private final List<String> symbolPath;
    private final List<String> columnsPath;
    private final List<String> tablePath;
    private final String priceColumn;

    public QuandlQuoteService(Config config) {
        this.urlFormat = config.getString("urlFormat").replaceAll("\\$\\{apiKey}", config.getString("apiKey"));
        this.icon = new IconLoader(config).getIcon();
        this.symbolPath = config.getStringList("symbolPath");
        this.columnsPath = config.getStringList("columnsPath");
        this.tablePath = config.getStringList("tablePath");
        this.priceColumn = config.getString("priceColumn");
    }

    @Override
    public void getPrices(Collection<String> symbols, Callback callback) throws IOException {
        for (String symbol : symbols) {
            String url = urlFormat.replaceAll("\\$\\{symbol}", symbol);
            try (InputStream stream = new URL(url).openStream()) {
                callback.accept(getPrice(stream), url, icon, url);
            } catch (Exception ex) {
                logger.warn("error getting price from " + url + ": " + ex.getMessage());
            }
        }
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
