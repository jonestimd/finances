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
import java.util.Map;

import javax.swing.Icon;

import com.typesafe.config.Config;
import io.github.jonestimd.util.Streams;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Extract a security quote from a web page using CSS selectors.  Multiple sources can be included in the
 * application configuration.  A separate instance of this class is used for each configured source.
 * <h3>Configuration format:</h3>
 * <pre>
 * finances {
 *   stockquote {
 *     scrapers = [{
 *       urlFormat = "https://host/page?symbol=${symbol}"
 *       iconUrl = "https://host/favicon.ico"
 *       price {
 *         format = "#,##0.0#" // optional format for parsing the value
 *         selector {
 *           query = "#quote-data > meta[itemprop=price]"
 *           attribute = "content" // optional: name of attribute containing the price, otherwise uses tag content
 *         }
 *       }
 *     }, ...]
 *   }
 * }
 * </pre>
 */
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
    private final Icon sourceIcon;
    private final PriceExtractor priceExtractor;

    public ScraperQuoteService(Config config) {
        this.urlFormat = config.getString("urlFormat");
        this.sourceIcon = new IconLoader(config).getIcon();
        this.priceExtractor = new PriceExtractor(config.getConfig("price"));
    }

    @Override
    public void getPrices(Collection<String> symbols, Callback callback) throws IOException {
        for (String symbol : symbols) {
            logger.debug("getting price for " + symbol);
            String url = urlFormat.replaceAll("\\$\\{symbol}", symbol);
            try (InputStream stream = new URL(url).openStream()) {
                callback.accept(getPrice(symbol, stream), url, sourceIcon, url);
            } catch (Exception ex) {
                logger.warn("error getting price from " + url + ": " + ex);
            }
        }
    }

    private Map<String, BigDecimal> getPrice(String symbol, InputStream stream) throws IOException {
        Document document = Jsoup.parse(stream, "utf-8", urlFormat);
        BigDecimal price = priceExtractor.getValue(document);
        return Collections.singletonMap(symbol, price);
    }
}
