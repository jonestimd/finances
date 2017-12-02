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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.table.TableCellRenderer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;

import static io.github.jonestimd.finance.swing.asset.SecurityColumnAdapter.*;

public class PriceTableProvider implements SecurityTableExtension {
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("io.github.jonestimd.finance.stockquote.ComponentLabels");
    private final ColumnAdapter<SecuritySummary, StockQuote> priceAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "price", StockQuote.class, this::getStockQuote, null);
    private final ColumnAdapter<SecuritySummary, BigDecimal> marketValueAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "marketValue", BigDecimal.class, this::getMarketValue, null);
    private final ColumnAdapter<SecuritySummary, BigDecimal> returnOnInvestmentAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "returnOnInvestment", BigDecimal.class, this::getReturnOnInvestment, null);
    private List<ColumnAdapter<SecuritySummary, ?>> columnAdapters = ImmutableList.of(priceAdapter, marketValueAdapter, returnOnInvestmentAdapter);
    protected final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private final Map<String, StockQuote> stockQuotes = new HashMap<>();
    private final BackgroundQuoteService quoteService;

    public PriceTableProvider(BackgroundQuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Override
    public List<? extends ColumnAdapter<SecuritySummary, ?>> getColumnAdapters() {
        return columnAdapters;
    }

    @Override
    public void setBeans(Collection<SecuritySummary> securitySummaries) {
        requestMissingPrices(securitySummaries);
    }

    @Override
    public void addBean(SecuritySummary securitySummary) {
        if (needQuote(securitySummary.getSymbol())) {
            getPrices(Collections.singleton(securitySummary.getSymbol()));
        }
    }

    @Override
    public boolean updateBean(SecuritySummary securitySummary, String columnId, Object oldValue) {
        if (needQuote(securitySummary.getSymbol())) {
            getPrices(Collections.singleton(securitySummary.getSymbol()));
        }
        return false;
    }

    @Override
    public void removeBean(SecuritySummary securitySummary) {
    }

    @Override
    public void addStateChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(STATE_PROPERTY, listener);
    }

    @Override
    public void removeStateChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(STATE_PROPERTY, listener);
    }

    private StockQuote getStockQuote(SecuritySummary summary) {
        return stockQuotes.get(summary.getSecurity().getSymbol());
    }

    private BigDecimal getLastPrice(SecuritySummary summary) {
        StockQuote quote = getStockQuote(summary);
        return quote == null ? null : quote.getPrice();
    }

    private BigDecimal getMarketValue(SecuritySummary summary) {
        BigDecimal lastPrice = getLastPrice(summary);
        return lastPrice == null ? null : lastPrice.multiply(summary.getShares());
    }

    private BigDecimal getReturnOnInvestment(SecuritySummary summary) {
        BigDecimal marketValue = getMarketValue(summary);
        BigDecimal costBasis = summary.getCostBasis();
        return marketValue == null || costBasis == null ? null : marketValue.subtract(costBasis);
    }

    protected void requestMissingPrices(Collection<SecuritySummary> summaries) {
        List<String> symbols = summaries.stream().map(SecuritySummary::getSymbol)
                .filter(this::needQuote).collect(Collectors.toList());
        if (! symbols.isEmpty()) getPrices(symbols);
    }

    private void getPrices(Collection<String> symbols) {
        symbols.forEach(symbol -> stockQuotes.put(symbol, StockQuote.pending(symbol)));
        quoteService.getPrices(symbols, this::addStockQuotes);
    }

    private boolean needQuote(String symbol) {
        return !(Strings.isNullOrEmpty(symbol) || stockQuotes.containsKey(symbol));
    }

    private void addStockQuotes(Stream<StockQuote> stockQuotes) {
        stockQuotes.forEach(stockQuote -> this.stockQuotes.put(stockQuote.getSymbol(), stockQuote));
        changeSupport.firePropertyChange(STATE_PROPERTY, null, null);
    }

    @Override
    public Map<String, TableCellRenderer> getTableCellRenderers() {
        return Collections.singletonMap("stockQuote", new StockQuoteTableCellRenderer());
    }
}