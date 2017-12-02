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
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.function.Predicate;

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
import io.github.jonestimd.swing.table.model.AsyncTableDataProvider;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionPropertyAdapter;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.asset.SecurityColumnAdapter.*;

public class StockQuoteTableProvider extends AsyncTableDataProvider<SecuritySummary, Collection<String>, Map<String, BigDecimal>>
implements SecurityTableExtension, TableSummary {
    public static final String TOTAL_VALUE = "totalValue";
    private static final Predicate<String> NOT_EMPTY = input -> ! Strings.isNullOrEmpty(input);
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("io.github.jonestimd.finance.stockquote.ComponentLabels");
    private final Map<String, BigDecimal> stockQuotes = new HashMap<>();
    private final ColumnAdapter<SecuritySummary, BigDecimal> priceAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "price", BigDecimal.class, this::getLastPrice, null);
    private final ColumnAdapter<SecuritySummary, BigDecimal> marketValueAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "marketValue", BigDecimal.class, this::getMarketValue, null);
    private final ColumnAdapter<SecuritySummary, BigDecimal> returnOnInvestmentAdapter =
            new FunctionColumnAdapter<>(BUNDLE, RESOURCE_PREFIX, "returnOnInvestment", BigDecimal.class, this::getReturnOnInvestment, null);
    private List<ColumnAdapter<SecuritySummary, ?>> columnAdapters = ImmutableList.of(priceAdapter, marketValueAdapter, returnOnInvestmentAdapter);
    private final PropertyAdapter<BigDecimal> totalValueAdapter = new FunctionPropertyAdapter<>(
            TOTAL_VALUE, BUNDLE.getString("table.security.totalValue"), this::getTotalValue, FormatFactory::currencyFormat);

    private final StockQuoteService quoteService;
    private final boolean bulkQueries;
    private Map<String, BigDecimal> sharesBySymbol = new HashMap<>();
    private BigDecimal totalValue = BigDecimal.ZERO;
    private final DomainEventListener<Long, SecuritySummary> securitySummaryListener = event -> {
        for (SecuritySummary summary : event.getDomainObjects()) {
            addShares(summary.getSecurity().getSymbol(), summary.getShares());
        }
        submitIfNotPending(Streams.map(event.getDomainObjects(), SecuritySummary::getSymbol));
    };

    public StockQuoteTableProvider(StockQuoteService quoteService, boolean bulkQueries, DomainEventPublisher domainEventPublisher) {
        this.quoteService = quoteService;
        this.bulkQueries = bulkQueries;
        domainEventPublisher.register(SecuritySummary.class, securitySummaryListener);
    }

    private BigDecimal getLastPrice(SecuritySummary summary) {
        return stockQuotes.get(summary.getSecurity().getSymbol());
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

    @Override
    public List<? extends PropertyAdapter<?>> getSummaryProperties() {
        return Collections.singletonList(totalValueAdapter);
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
        submitIfNotPending(Streams.map(securitySummaries, SecuritySummary::getSymbol));
    }

    private void addShares(String symbol, BigDecimal shares) {
        if (! Strings.isNullOrEmpty(symbol)) {
            BigDecimal total = MoreObjects.firstNonNull(sharesBySymbol.get(symbol), BigDecimal.ZERO);
            sharesBySymbol.put(symbol, total.add(shares));
        }
    }

    @Override
    public void addBean(SecuritySummary securitySummary) {
        submitIfNotPending(Collections.singleton(securitySummary.getSecurity().getSymbol()));
    }

    @Override
    public boolean updateBean(SecuritySummary securitySummary, String columnId, Object oldValue) {
        return false;
    }

    @Override
    public void removeBean(SecuritySummary securitySummary) {
    }

    @Override
    protected void submitIfNotPending(Collection<String> strings) {
        List<String> symbols = Streams.filter(strings, NOT_EMPTY);
        if (! symbols.isEmpty()) {
            if (bulkQueries) super.submitIfNotPending(symbols);
            else symbols.forEach(symbol -> super.submitIfNotPending(Collections.singleton(symbol)));
        }
    }

    @Override
    protected Map<String, BigDecimal> getData(Collection<String> symbols) throws IOException {
        return null; // quoteService.getPrices(symbols);
    }

    @Override
    protected void setResult(Map<String, BigDecimal> stockQuotes) {
        this.stockQuotes.putAll(stockQuotes);
        updateTotalValue();
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    private void updateTotalValue() {
        totalValue = BigDecimal.ZERO;
        for (Entry<String, BigDecimal> entry : sharesBySymbol.entrySet()) {
            BigDecimal price = this.stockQuotes.get(entry.getKey());
            if (price != null) {
                totalValue = totalValue.add(entry.getValue().multiply(price));
            }
        }
        firePropertyChange(TOTAL_VALUE, null, totalValue);
    }

    @Override
    protected boolean matches(Collection<String> pendingQuery, Collection<String> newQuery) {
        return pendingQuery.containsAll(newQuery);
    }
}
