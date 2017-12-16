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

import javax.json.JsonObject;
import javax.swing.Icon;

import com.typesafe.config.Config;
import org.apache.log4j.Logger;

/**
 * Get security prices from Alpha Vantage.  Enabled by adding a valid API key in the application configuration.
 * <pre>
 * finances {
 *   stockquote {
 *     alphavantage {
 *       apiKey = "your API key"
 *     }
 *   }
 * }
 * </pre>
 */
public class AlphaVantageQuoteService implements StockQuoteService {
    public static final StockQuoteServiceFactory FACTORY = new StockQuoteServiceFactory() {
        @Override
        public Optional<StockQuoteService> create(Config config) {
            try {
                if (config.hasPath("alphavantage.apiKey")) {
                    return Optional.of(new AlphaVantageQuoteService(config.getConfig("alphavantage")));
                }
            } catch (Exception ex) {
                logger.warn("Failed to initialize the Alpha Vantage service client", ex);
            }
            return Optional.empty();
        }
    };

    private static final Logger logger = Logger.getLogger(AlphaVantageQuoteService.class);
    private final Icon icon;
    private final String urlFormat;
    private final long minRequestPeriod;
    private final List<String> seriesPath;
    private final String priceKey;
    private final String errorKey;
    private final List<String> symbolPath;
    private volatile long nextRequestMs = System.currentTimeMillis();

    public AlphaVantageQuoteService(Config config) {
        this.icon = new IconLoader(config).getIcon();
        this.urlFormat = config.getString("urlFormat").replaceAll("\\$\\{apiKey}", config.getString("apiKey"));
        this.minRequestPeriod = config.getLong("minRequestPeriodMs");
        this.symbolPath = config.getStringList("symbolPath");
        this.seriesPath = config.getStringList("seriesPath");
        this.priceKey = config.getString("priceKey");
        this.errorKey = config.getString("errorKey");
    }

    private void waitForNextRequest() {
        while (nextRequestMs > System.currentTimeMillis()) {
            try {
                Thread.sleep(nextRequestMs - System.currentTimeMillis());
            } catch (InterruptedException e) {
                // ignore
            }
        }
        nextRequestMs = System.currentTimeMillis() + minRequestPeriod;
    }

    @Override
    public void getPrices(Collection<String> symbols, Callback callback) throws IOException {
        for (String symbol : symbols) {
            String url = urlFormat.replaceAll("\\$\\{symbol}", symbol);
            waitForNextRequest();
            try (InputStream stream = new URL(url).openStream()) {
                logger.debug("getting price for " + symbol);
                callback.accept(getPrice(stream), url, icon, url);
            } catch (Exception ex) {
                logger.warn("error getting price from " + url + ": " + ex.getMessage());
            }
        }
    }

    private Map<String, BigDecimal> getPrice(InputStream stream) {
        JsonHelper helper = new JsonHelper(stream);
        helper.optionalString(errorKey).ifPresent(this::throwIllegalState);
        String symbol = helper.findString(symbolPath);
        JsonObject series = helper.findObject(seriesPath);
        return series.keySet().stream().max(String::compareTo)
                .map(date -> JsonHelper.findString(priceKey, series.getJsonObject(date)))
                .map(price -> Collections.singletonMap(symbol, new BigDecimal(price)))
                .orElse(Collections.emptyMap());
    }

    private void throwIllegalState(String error) {
        throw new IllegalStateException(error);
    }
}
