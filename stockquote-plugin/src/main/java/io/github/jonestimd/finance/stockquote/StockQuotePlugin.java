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

import java.util.Collections;
import java.util.List;

import com.typesafe.config.Config;
import io.github.jonestimd.finance.plugin.FinancePlugin;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.stockquote.alphavantage.AlphaVantageQuoteService;
import io.github.jonestimd.finance.stockquote.quandl.QuandlQuoteService;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;

import static io.github.jonestimd.finance.config.ApplicationConfig.*;

public class StockQuotePlugin implements FinancePlugin {
    private List<? extends SecurityTableExtension> securityTableExtensions;

    @Override
    public void initialize(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher) {
        boolean bulkQueries = CONFIG.getBoolean("finances.stockquote.bulkQueries");
        Config config = CONFIG.getConfig("finances.stockquote.alphavantage");
        if (config.hasPath("apiKey")) {
            setExtension(new StockQuoteTableProvider(new AlphaVantageQuoteService(config), bulkQueries, domainEventPublisher));
        }
        else {
            config = CONFIG.getConfig("finances.stockquote.quandl");
            if (config.hasPath("apiKey")) {
                setExtension(new StockQuoteTableProvider(new QuandlQuoteService(config), bulkQueries, domainEventPublisher));
            }
            else securityTableExtensions = Collections.emptyList();
        }
    }

    private void setExtension(SecurityTableExtension extension) {
        securityTableExtensions = Collections.singletonList(extension);
    }

    @Override
    public List<? extends SecurityTableExtension> getSecurityTableExtensions() {
        return securityTableExtensions;
    }
}
