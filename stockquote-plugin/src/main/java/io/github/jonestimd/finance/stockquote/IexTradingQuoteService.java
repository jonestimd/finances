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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.typesafe.config.Config;
import org.apache.log4j.Logger;

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
    private final String urlFormat;
    private final List<String> pricePath;

    public IexTradingQuoteService(Config config) {
        this.urlFormat = config.getString("urlFormat");
        this.pricePath = config.getStringList("pricePath");
    }

    @Override
    public Map<String, BigDecimal> getPrices(Collection<String> symbols) throws IOException {
        String url = urlFormat.replaceAll("\\$\\{symbols}", symbols.stream().map(this::encode).collect(Collectors.joining(",")));
        try (InputStream stream = new URL(url).openStream()) {
            return getPrices(stream);
        } catch (Exception ex) {
            logger.warn("error getting price from " + url + ": " + ex.getMessage());
            return Collections.emptyMap();
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
