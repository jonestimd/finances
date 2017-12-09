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
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Icon;

import com.typesafe.config.Config;
import org.apache.log4j.Logger;

/**
 * Get security quotes from IEX.  Enabled by adding a flag in the application configuration.
 * <pre>
 * finances {
 *   stockquote {
 *     iextrading {
 *       enabled = true
 *     }
 *   }
 * }
 * </pre>
 */
public class IexTradingQuoteService implements StockQuoteService {
    public static final StockQuoteServiceFactory FACTORY = new StockQuoteServiceFactory() {
        @Override
        public boolean isEnabled(Config config) {
            return config.getBoolean("iextrading.enabled");
        }

        @Override
        public StockQuoteService create(Config config) {
            return new IexTradingQuoteService(config.getConfig("iextrading"));
        }
    };

    private final Logger logger = Logger.getLogger(getClass());
    private final String tooltip = StockQuotePlugin.BUNDLE.getString("quote.service.iex.attribution.tooltip");
    private final String attributionUrl = StockQuotePlugin.BUNDLE.getString("quote.service.iex.attribution.url");
    private final String urlFormat;
    private final List<String> pricePath;
    private final Icon icon;

    public IexTradingQuoteService(Config config) {
        this.urlFormat = config.getString("urlFormat");
        this.pricePath = config.getStringList("pricePath");
        this.icon = new IconLoader(config).getIcon();
    }

    @Override
    public void getPrices(Collection<String> symbols, Callback callback) throws IOException {
        String url = urlFormat.replaceAll("\\$\\{symbols}", symbols.stream().map(this::encode).collect(Collectors.joining(",")));
        try (InputStream stream = new URL(url).openStream()) {
            callback.accept(getPrices(stream), attributionUrl, icon, tooltip);
        } catch (Exception ex) {
            logger.warn("error getting price from " + url + ": " + ex.getMessage());
        }
    }

    private Map<String, BigDecimal> getPrices(InputStream stream) {
        JsonHelper helper = new JsonHelper(stream);
        return helper.mapEntries(pricePath, JsonHelper::asBigDecimal);
    }

    private String encode(String symbol) {
        try {
            return URLEncoder.encode(symbol, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return symbol;
        }
    }
}
