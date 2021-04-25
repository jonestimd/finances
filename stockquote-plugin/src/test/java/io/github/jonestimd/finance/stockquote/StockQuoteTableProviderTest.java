package io.github.jonestimd.finance.stockquote;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.JTable;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.event.SecurityEvent;
import io.github.jonestimd.finance.domain.event.SecuritySummaryEvent;
import io.github.jonestimd.finance.swing.asset.SecurityColumnAdapter;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.swing.table.PropertyAdapter;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StockQuoteTableProviderTest {
    @Mock
    private BackgroundQuoteService quoteService;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private MouseEvent event;
    @Mock
    private JTable table;
    @Mock
    private Desktop desktop;
    @Captor
    private ArgumentCaptor<DomainEventListener<Long, SecuritySummary>> summaryListenerCaptor;
    @Captor
    private ArgumentCaptor<DomainEventListener<Long, Security>> securityListenerCaptor;

    @Test
    public void setBeansRequestsPrices() throws Exception {
        StockQuoteTableProvider provider = new StockQuoteTableProvider(quoteService, eventPublisher);

        provider.setBeans(Arrays.asList(newRow("Security 1", "S1", 1L, null), newRow("Security 2", "S2", 1L, null)));

        verify(quoteService).getPrices(eq(Arrays.asList("S1", "S2")), notNull());
    }

    @Test
    public void addBeansRequestsPrices() throws Exception {
        StockQuoteTableProvider provider = new StockQuoteTableProvider(quoteService, eventPublisher);

        provider.addBean(newRow("Security 3", "S3", 1L, null));

        verify(quoteService).getPrices(eq(singleton("S3")), notNull());
    }

    @Test
    public void requestsPricesForSecuritySummaryEvent() throws Exception {
        new StockQuoteTableProvider(quoteService, eventPublisher);
        verify(eventPublisher).register(eq(SecuritySummary.class), summaryListenerCaptor.capture());

        summaryListenerCaptor.getValue().onDomainEvent(new SecuritySummaryEvent("test", EventType.ADDED, newRow("Security 4", "S4", 1L, null)));

        verify(quoteService).getPrices(eq(singletonList("S4")), notNull());
    }

    @Test
    public void updatesTotalValueForSecuritySummaryChangedEvent() throws Exception {
        StockQuoteTableProvider provider = providerWithQuotes(singletonList(newRow("Security 1", "S1", 1L, null)),
                Stream.of(new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        verify(eventPublisher).register(eq(SecuritySummary.class), summaryListenerCaptor.capture());

        summaryListenerCaptor.getValue().onDomainEvent(new SecuritySummaryEvent("test", EventType.CHANGED, newRow("Security 1", "S1", 2L, null)));

        verify(quoteService).getPrices(eq(singletonList("S1")), notNull());
        assertThat(provider.getTotalValue()).isEqualTo(new BigDecimal(30));
    }

    @Test
    public void updatesTotalValueForSecuritySummaryReplacedEvent() throws Exception {
        StockQuoteTableProvider provider = providerWithQuotes(singletonList(newRow("Security 1", "S1", 1L, null)),
                Stream.of(new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        verify(eventPublisher).register(eq(SecuritySummary.class), summaryListenerCaptor.capture());

        summaryListenerCaptor.getValue().onDomainEvent(new SecuritySummaryEvent("test", EventType.REPLACED, newRow("Security 1", "S1", 2L, null)));

        verify(quoteService).getPrices(eq(singletonList("S1")), notNull());
        assertThat(provider.getTotalValue()).isEqualTo(new BigDecimal(20));
    }

    @Test
    public void requestsPricesForSecurityEvent() throws Exception {
        new StockQuoteTableProvider(quoteService, eventPublisher);
        verify(eventPublisher).register(eq(Security.class), securityListenerCaptor.capture());

        securityListenerCaptor.getValue().onDomainEvent(new SecurityEvent("test", EventType.ADDED, newSecurity("Security 4", "S4")));

        verify(quoteService).getPrices(eq(singletonList("S4")), notNull());
    }

    @Test
    public void updateBeanReturnsFalseForUnknownColumn() throws Exception {
        StockQuoteTableProvider provider = new StockQuoteTableProvider(quoteService, eventPublisher);

        assertThat(provider.updateBean(null, null, null)).isFalse();
    }

    @Test
    public void updateBeanReturnsTrueForSharesColumn() throws Exception {
        StockQuoteTableProvider provider = new StockQuoteTableProvider(quoteService, eventPublisher);

        assertThat(provider.updateBean(null, SecurityColumnAdapter.SHARES_ADAPTER.getColumnId(), null)).isTrue();
    }

    @Test
    public void removeBeanDoesNothing() throws Exception {
        StockQuoteTableProvider provider = new StockQuoteTableProvider(quoteService, eventPublisher);

        provider.removeBean(null);
    }

    @Test
    public void getSummaryProperties() throws Exception {
        StockQuoteTableProvider provider = new StockQuoteTableProvider(quoteService, eventPublisher);
        provider.setBeans(singletonList(newRow("Security 1", "S1", 1L, null)));

        List<? extends PropertyAdapter<?>> summaryProperties = provider.getSummaryProperties();

        assertThat(summaryProperties).hasSize(1);
        assertThat(summaryProperties.get(0).getName()).isEqualTo("totalValue");
        assertThat(summaryProperties.get(0).getValue()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void priceColumnAdapterDisplaysStockQuote() throws Exception {
        List<SecuritySummary> beans = Arrays.asList(newRow("Security 1", "S1", 1L, null), newRow("Security 2", "S2", 1L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S2", BigDecimal.ONE, null, null, null),
                new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, StockQuote> priceAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(0);

        assertThat(priceAdapter.getValue(beans.get(0)).getSymbol()).isEqualTo("S1");
        assertThat(priceAdapter.getValue(beans.get(0)).getPrice()).isEqualTo(BigDecimal.TEN);
        assertThat(priceAdapter.getValue(beans.get(1)).getSymbol()).isEqualTo("S2");
        assertThat(priceAdapter.getValue(beans.get(1)).getPrice()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void priceColumnAdapterShowsHandCursorForSourceUrl() throws Exception {
        List<SecuritySummary> beans = Arrays.asList(newRow("Security 1", "S1", 1L, null), newRow("Security 2", "S2", 1L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(new StockQuote("S1", BigDecimal.TEN, "url", null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, StockQuote> priceAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(0);

        assertThat(priceAdapter.getCursor(event, table, beans.get(0))).isEqualTo(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        assertThat(priceAdapter.getCursor(event, table, beans.get(1))).isNull();
    }

    @Test
    public void priceColumnAdapterOpensSourceUrlInBrowser() throws Exception {
        when(event.getClickCount()).thenReturn(1);
        List<SecuritySummary> beans = Arrays.asList(newRow("Security 1", "S1", 1L, null), newRow("Security 2", "S2", 1L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(new StockQuote("S1", BigDecimal.TEN, "http://localhost", null, null)));
        ColumnAdapter<SecuritySummary, StockQuote> priceAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(0);

        priceAdapter.handleClick(event, table, beans.get(0));

        verify(desktop).browse(new URL("http://localhost").toURI());
    }

    @Test
    public void priceColumnAdapterIgnoresDoubleClick() throws Exception {
        when(event.getClickCount()).thenReturn(2);
        List<SecuritySummary> beans = Arrays.asList(newRow("Security 1", "S1", 1L, null), newRow("Security 2", "S2", 1L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(new StockQuote("S1", BigDecimal.TEN, "http://localhost", null, null)));
        ColumnAdapter<SecuritySummary, StockQuote> priceAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(0);

        priceAdapter.handleClick(event, table, beans.get(0));

        verify(desktop, never()).browse(any());
    }

    @Test
    public void priceColumnAdapterIgnoresClickForNoUrl() throws Exception {
        when(event.getClickCount()).thenReturn(1);
        List<SecuritySummary> beans = Arrays.asList(newRow("Security 1", "S1", 1L, null), newRow("Security 2", "S2", 1L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        ColumnAdapter<SecuritySummary, StockQuote> priceAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(0);

        priceAdapter.handleClick(event, table, beans.get(0));

        verify(desktop, never()).browse(any());
    }

    @Test
    public void marketValueReturnsNullForZeroShares() throws Exception {
        List<SecuritySummary> beans = singletonList(newRow("Security 1", "S1", 0L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, StockQuote> marketValueAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(1);

        assertThat(marketValueAdapter.getValue(beans.get(0))).isNull();
    }

    @Test
    public void marketValueReturnsNullForNoQuote() throws Exception {
        List<SecuritySummary> beans = singletonList(newRow("Security 1", "S1", 10L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S2", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, StockQuote> marketValueAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(1);

        assertThat(marketValueAdapter.getValue(beans.get(0))).isNull();
    }

    @Test
    public void marketValueReturnsValueForPositiveShares() throws Exception {
        List<SecuritySummary> beans = singletonList(newRow("Security 1", "S1", 10L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, BigDecimal> marketValueAdapter = (ColumnAdapter<SecuritySummary, BigDecimal>) provider.getColumnAdapters().get(1);

        assertThat(marketValueAdapter.getValue(beans.get(0))).isEqualTo(new BigDecimal("100"));
    }

    @Test
    public void roiReturnsNullForZeroShares() throws Exception {
        List<SecuritySummary> beans = singletonList(newRow("Security 1", "S1", 0L, 1L));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, StockQuote> roiAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(2);

        assertThat(roiAdapter.getValue(beans.get(0))).isNull();
    }

    @Test
    public void roiReturnsNullForNoQuote() throws Exception {
        List<SecuritySummary> beans = singletonList(newRow("Security 1", "S1", 10L, 1L));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S2", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, StockQuote> roiAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(2);

        assertThat(roiAdapter.getValue(beans.get(0))).isNull();
    }

    @Test
    public void roiReturnsNullForNullCostBasis() throws Exception {
        List<SecuritySummary> beans = singletonList(newRow("Security 1", "S1", 10L, null));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, StockQuote> roiAdapter = (ColumnAdapter<SecuritySummary, StockQuote>) provider.getColumnAdapters().get(2);

        assertThat(roiAdapter.getValue(beans.get(0))).isNull();
    }

    @Test
    public void roiReturnsValueForPositiveShares() throws Exception {
        List<SecuritySummary> beans = singletonList(newRow("Security 1", "S1", 10L, 1L));
        StockQuoteTableProvider provider = providerWithQuotes(beans, Stream.of(
                new StockQuote("S1", BigDecimal.TEN, null, null, null)));
        provider.setBeans(beans);

        ColumnAdapter<SecuritySummary, BigDecimal> roiAdapter = (ColumnAdapter<SecuritySummary, BigDecimal>) provider.getColumnAdapters().get(2);

        assertThat(roiAdapter.getValue(beans.get(0))).isEqualTo(new BigDecimal(99));
    }

    private StockQuoteTableProvider providerWithQuotes(List<SecuritySummary> beans, Stream<StockQuote> quotes) {
        doAnswer(invocation -> {
            Consumer<Stream<StockQuote>> callback = (Consumer<Stream<StockQuote>>) invocation.getArguments()[1];
            callback.accept(quotes);
            return null;
        }).when(quoteService).getPrices(anyCollection(), any());
        StockQuoteTableProvider provider = new StockQuoteTableProvider(quoteService, eventPublisher, desktop);
        provider.setBeans(beans);
        return provider;
    }

    private SecuritySummary newRow(String name, String s1, long shares, Long costBasis) {
        return new SecuritySummary(newSecurity(name, s1), 0, new BigDecimal(shares), null,
                costBasis == null ? null : new BigDecimal(costBasis), null);
    }

    private Security newSecurity(String name, String symbol) {
        Security security = new Security(name, SecurityType.STOCK);
        security.setSymbol(symbol);
        return security;
    }
}