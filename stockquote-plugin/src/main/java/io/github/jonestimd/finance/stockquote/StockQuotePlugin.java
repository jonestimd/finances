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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.swing.table.TableCellRenderer;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import io.github.jonestimd.finance.plugin.FinancePlugin;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;

public class StockQuotePlugin implements FinancePlugin {
    private static final Map<String, StockQuoteServiceFactory> FACTORY_NAMES = ImmutableMap.of(
        "alphavantage", AlphaVantageQuoteService.FACTORY,
        "iextrading", IexTradingQuoteService.FACTORY,
        "scraper", ScraperQuoteService.FACTORY,
        "quandl", QuandlQuoteService.FACTORY
    );
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("io.github.jonestimd.finance.stockquote.ComponentLabels");
    private final TableCellRenderer stockQuoteRenderer = new StockQuoteTableCellRenderer();

    private List<? extends SecurityTableExtension> securityTableExtensions = Collections.emptyList();

    @Override
    public void initialize(Config appConfig, ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher) {
        Config config = appConfig.getConfig("finances.stockquote");
        List<StockQuoteService> quoteServices = config.getStringList("services").stream()
                .map(FACTORY_NAMES::get)
                .filter(factory -> factory.isEnabled(config))
                .map(factory -> factory.create(config))
                .collect(Collectors.toList());
        if (!quoteServices.isEmpty()) {
            setExtension(new StockQuoteTableProvider(new BackgroundQuoteService(config, quoteServices), domainEventPublisher));
        }
    }

    private void setExtension(SecurityTableExtension extension) {
        securityTableExtensions = Collections.singletonList(extension);
    }

    @Override
    public List<? extends SecurityTableExtension> getSecurityTableExtensions() {
        return securityTableExtensions;
    }

    @Override
    public Map<Class<?>, TableCellRenderer> getTableCellRenderers() {
        return Collections.singletonMap(StockQuote.class, stockQuoteRenderer);
    }
}