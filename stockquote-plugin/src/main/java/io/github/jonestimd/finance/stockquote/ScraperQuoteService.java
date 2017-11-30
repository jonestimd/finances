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
import java.util.Map;

import com.typesafe.config.Config;
import io.github.jonestimd.util.Streams;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class ScraperQuoteService implements StockQuoteService {
    public static final String SCRAPER_LIST = "scrapers";
    public static final StockQuoteServiceFactory FACTORY = new StockQuoteServiceFactory() {
        @Override
        public boolean isEnabled(Config config) {
            return config.hasPath(SCRAPER_LIST) && ! config.getConfigList("scrapers").isEmpty();
        }

        @Override
        public StockQuoteService create(Config config) {
            return new HierarchicalQuoteService(Streams.map(config.getConfigList(SCRAPER_LIST), ScraperQuoteService::new));
        }
    };

    private final Logger logger = Logger.getLogger(getClass());
    private final String urlFormat;
    private final String symbolSelector;
    private final String symbolAttribute;
    private final String priceSelector;
    private final String priceAttribute;

    private static String getOptionalString(Config config, String path) {
        return config.hasPath(path) ? config.getString(path) : null;
    }

    public ScraperQuoteService(Config config) {
        this.urlFormat = config.getString("urlFormat");
        this.symbolSelector = config.getString("symbolSelector.query");
        this.symbolAttribute = getOptionalString(config, "symbolSelector.attribute");
        this.priceSelector = config.getString("priceSelector.query");
        this.priceAttribute = getOptionalString(config, "priceSelector.attribute");
    }

    @Override
    public Map<String, BigDecimal> getPrices(Collection<String> symbols) throws IOException {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (String symbol : symbols) {
            logger.debug("getting price for " + symbol);
            prices.putAll(getPrice(symbol));
        }
        return prices;
    }

    private Map<String, BigDecimal> getPrice(String symbol) {
        String url = urlFormat.replaceAll("\\$\\{symbol}", symbol);
        try (InputStream stream = new URL(url).openStream()) {
            return getPrice(stream);
        } catch (Exception ex) {
            logger.warn("error getting price from " + url + ": " + ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, BigDecimal> getPrice(InputStream stream) throws IOException {
        Document document = Jsoup.parse(stream, "utf-8", urlFormat);
        String symbol = getString(document, symbolSelector, symbolAttribute);
        BigDecimal price = getNumber(document, priceSelector, priceAttribute);
        return Collections.singletonMap(symbol, price);
    }

    private BigDecimal getNumber(Document document, String selector, String attribute) {
        String string = getString(document, selector, attribute);
        return string == null ? null : new BigDecimal(string);
    }

    private String getString(Document document, String selector, String attribute) {
        Elements elements = document.select(selector);
        if (elements.isEmpty()) return null;
        return attribute == null ? elements.get(0).text() : elements.get(0).attr(attribute);
    }
}
