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
package io.github.jonestimd.finance.stockquote.quandl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.typesafe.config.Config;
import io.github.jonestimd.finance.stockquote.StockQuoteService;
import org.apache.log4j.Logger;

public class QuandlQuoteService implements StockQuoteService {
    private final Logger logger = Logger.getLogger(getClass());
    private final String urlFormat;
    private final JsonMapper jsonMapper;

    public QuandlQuoteService(Config config) {
        String urlFormat = config.getString("urlFormat");
        String apiKey = config.getString("apiKey");
        this.urlFormat = urlFormat.replaceAll("\\$\\{apiKey}", apiKey);
        this.jsonMapper = new JsonMapper(config);
    }

    @Override
    public Map<String, BigDecimal> getPrices(Collection<String> symbols) throws IOException {
        Map<String, BigDecimal> prices = new HashMap<>();
        for (String symbol : symbols) {
            String url = urlFormat.replaceAll("\\$\\{symbol}", symbol);
            try (InputStream stream = new URL(url).openStream()) {
                prices.putAll(jsonMapper.getPrice(stream));
            } catch (Exception ex) {
                logger.warn("error getting price from " + url + ": " + ex.getMessage());
            }
        }
        return prices;
    }
}
