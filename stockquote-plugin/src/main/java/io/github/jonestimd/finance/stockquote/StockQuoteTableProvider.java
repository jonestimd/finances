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

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JTable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.table.PropertyAdapter;
import io.github.jonestimd.swing.table.TableSummary;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.ConstantColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionPropertyAdapter;
import org.apache.log4j.Logger;

import static io.github.jonestimd.finance.stockquote.StockQuotePlugin.*;
import static io.github.jonestimd.finance.swing.asset.SecurityColumnAdapter.*;

public class StockQuoteTableProvider implements SecurityTableExtension, TableSummary {
    public static final String TOTAL_VALUE = "totalValue";
    private final Logger logger = Logger.getLogger(getClass());
    private final ColumnAdapter<SecuritySummary, StockQuote> priceAdapter = new PriceColumnAdapter();
    private final ColumnAdapter<SecuritySummary, BigDecimal> marketValueAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "marketValue", BigDecimal.class, this::getMarketValue, null);
    private final ColumnAdapter<SecuritySummary, BigDecimal> returnOnInvestmentAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "returnOnInvestment", BigDecimal.class, this::getReturnOnInvestment, null);
    private List<ColumnAdapter<SecuritySummary, ?>> columnAdapters = ImmutableList.of(priceAdapter, marketValueAdapter, returnOnInvestmentAdapter);
    private final PropertyAdapter<BigDecimal> totalValueAdapter = new FunctionPropertyAdapter<>(
            TOTAL_VALUE, BUNDLE.getString("table.security.totalValue"), this::getTotalValue, FormatFactory::currencyFormat);

    protected final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private final Map<String, StockQuote> stockQuotes = new HashMap<>();
    private final BackgroundQuoteService quoteService;
    private Map<String, BigDecimal> sharesBySymbol = new HashMap<>();
    private BigDecimal totalValue = BigDecimal.ZERO;
    private final DomainEventListener<Long, SecuritySummary> securitySummaryListener = event -> {
        for (SecuritySummary summary : event.getDomainObjects()) {
            addShares(summary.getSecurity().getSymbol(), summary.getShares());
        }
        requestMissingPrices(event.getDomainObjects());
    };
    private final Desktop desktop;

    public StockQuoteTableProvider(BackgroundQuoteService quoteService, DomainEventPublisher domainEventPublisher) {
        this(quoteService, domainEventPublisher, Desktop.getDesktop());
    }

    protected StockQuoteTableProvider(BackgroundQuoteService quoteService, DomainEventPublisher domainEventPublisher, Desktop desktop) {
        this.quoteService = quoteService;
        this.desktop = desktop;
        domainEventPublisher.register(SecuritySummary.class, securitySummaryListener);
    }

    private BigDecimal getLastPrice(String symbol) {
        StockQuote quote = stockQuotes.get(symbol);
        return quote == null ? null : quote.getPrice();
    }

    private BigDecimal getMarketValue(SecuritySummary summary) {
        BigDecimal lastPrice = getLastPrice(summary.getSymbol());
        return lastPrice == null ? null : lastPrice.multiply(summary.getShares());
    }

    private BigDecimal getReturnOnInvestment(SecuritySummary summary) {
        BigDecimal marketValue = getMarketValue(summary);
        BigDecimal costBasis = summary.getCostBasis();
        return marketValue == null || costBasis == null ? null : marketValue.subtract(costBasis);
    }

    @Override
    public List<? extends PropertyAdapter<?>> getSummaryProperties() {
        return Collections.singletonList(totalValueAdapter);
    }

    @Override
    public void addStateChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(STATE_PROPERTY, listener);
    }

    @Override
    public void removeStateChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(STATE_PROPERTY, listener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends ColumnAdapter<SecuritySummary, ?>> getColumnAdapters() {
        return columnAdapters;
    }

    @Override
    public void setBeans(final Collection<SecuritySummary> securitySummaries) {
        sharesBySymbol.clear();
        totalValue = BigDecimal.ZERO;
        for (SecuritySummary summary : securitySummaries) {
            addShares(summary.getSecurity().getSymbol(), summary.getShares());
        }
        updateTotalValue();
        requestMissingPrices(securitySummaries);
    }

    private void addShares(String symbol, BigDecimal shares) {
        if (! Strings.isNullOrEmpty(symbol)) {
            BigDecimal total = MoreObjects.firstNonNull(sharesBySymbol.get(symbol), BigDecimal.ZERO);
            sharesBySymbol.put(symbol, total.add(shares));
        }
    }

    @Override
    public void addBean(SecuritySummary securitySummary) {
        if (needQuote(securitySummary.getSymbol())) {
            getPrices(Collections.singleton(securitySummary.getSymbol()));
        }
    }

    @Override
    public boolean updateBean(SecuritySummary securitySummary, String columnId, Object oldValue) {
        return false;
    }

    @Override
    public void removeBean(SecuritySummary securitySummary) {
    }

    protected void requestMissingPrices(Collection<SecuritySummary> summaries) {
        List<String> symbols = summaries.stream().map(SecuritySummary::getSymbol)
                .filter(this::needQuote).collect(Collectors.toList());
        if (! symbols.isEmpty()) getPrices(symbols);
    }

    private boolean needQuote(String symbol) {
        return !(Strings.isNullOrEmpty(symbol) || stockQuotes.containsKey(symbol));
    }

    private void getPrices(Collection<String> symbols) {
        symbols.forEach(symbol -> stockQuotes.put(symbol, StockQuote.pending(symbol)));
        quoteService.getPrices(symbols, this::addStockQuotes);
    }

    private void addStockQuotes(Stream<StockQuote> stockQuotes) {
        stockQuotes.forEach(stockQuote -> this.stockQuotes.put(stockQuote.getSymbol(), stockQuote));
        changeSupport.firePropertyChange(STATE_PROPERTY, null, null);
        updateTotalValue();
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    private void updateTotalValue() {
        totalValue = BigDecimal.ZERO;
        for (Entry<String, BigDecimal> entry : sharesBySymbol.entrySet()) {
            BigDecimal price = getLastPrice(entry.getKey());
            if (price != null) {
                totalValue = totalValue.add(entry.getValue().multiply(price));
            }
        }
        changeSupport.firePropertyChange(TOTAL_VALUE, null, totalValue);
    }

    private class PriceColumnAdapter extends ConstantColumnAdapter<SecuritySummary, StockQuote> {
        public PriceColumnAdapter() {
            super(BUNDLE, RESOURCE_PREFIX, "price", StockQuote.class, false);
        }

        @Override
        public StockQuote getValue(SecuritySummary summary) {
            return stockQuotes.get(summary.getSymbol());
        }

        @Override
        public void setValue(SecuritySummary securitySummary, StockQuote stockQuote) {
        }

        @Override
        public Cursor getCursor(MouseEvent event, JTable table, SecuritySummary row) {
            StockQuote quote = getValue(row);
            return quote != null && quote.getSourceUrl() != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null;
        }

        @Override
        public void handleClick(MouseEvent event, JTable table, SecuritySummary row) {
            StockQuote quote = getValue(row);
            if (event.getClickCount() == 1 && quote != null && quote.getSourceUrl() != null) {
                try {
                    desktop.browse(new URL(quote.getSourceUrl()).toURI());
                } catch (Exception ex) {
                    logger.warn("Failed to open url in browser", ex);
                }
            }
        }
    }
}
