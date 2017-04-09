package io.github.jonestimd.finance.yahoo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.SecuritySummaryEvent;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sun.awt.AppContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StockQuoteTableProviderTest {
    public static final String SYMBOL1 = "symbol1";
    public static final String SYMBOL2 = "symbol2";
    @Mock
    private YqlService yqlService;
    @Mock
    private PropertyChangeListener totalListener;
    @Captor
    private ArgumentCaptor<PropertyChangeEvent> eventCaptor;

    private DomainEventPublisher domainEventPublisher = new DomainEventPublisher();
    private StockQuoteTableProvider tableProvider;
    private Security security1 = new SecurityBuilder().nextId().name("security1").symbol(SYMBOL1).get();
    private Security security2 = new SecurityBuilder().nextId().name("security2").symbol(SYMBOL2).get();
    private Account account1 = new AccountBuilder().nextId().company(new Company("comany")).name("account1").get();

    @Before
    public void createTableProvider() {
        tableProvider = new StockQuoteTableProvider(yqlService, domainEventPublisher);
        tableProvider.addPropertyChangeListener(StockQuoteTableProvider.TOTAL_VALUE, totalListener);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void handlesNullPricesFromYahoo() throws Exception {
        when(yqlService.execute(anyString(), any(JsonMapper.class)))
                .thenReturn(Collections.singletonList(new StockQuote(SYMBOL1, null)));

        SwingUtilities.invokeAndWait(() -> domainEventPublisher.publishEvent(newEvent(new SecuritySummary(security1, 1L, new BigDecimal(2), account1))));
        waitForWorker();

        verify(yqlService).execute(contains("in (\"" + SYMBOL1 + "\")"), any(JsonMapper.class));
        verify(totalListener).propertyChange(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPropertyName()).isEqualTo(StockQuoteTableProvider.TOTAL_VALUE);
        assertThat(eventCaptor.getValue().getNewValue()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void securitySummaryEventUpdatesTotal() throws Exception {
        when(yqlService.execute(anyString(), any(JsonMapper.class)))
                .thenReturn(Collections.singletonList(new StockQuote(SYMBOL1, BigDecimal.TEN)));

        SwingUtilities.invokeAndWait(() -> domainEventPublisher.publishEvent(newEvent(
                new SecuritySummary(security1, 1L, new BigDecimal(2), account1),
                new SecuritySummary(security2, 1L, new BigDecimal(3), account1))));
        waitForWorker();

        verify(yqlService).execute(contains("in (\"" + SYMBOL1 + "\",\"" + SYMBOL2 + "\")"), any(JsonMapper.class));
        verify(totalListener).propertyChange(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPropertyName()).isEqualTo(StockQuoteTableProvider.TOTAL_VALUE);
        assertThat(eventCaptor.getValue().getNewValue()).isEqualTo(new BigDecimal(20));
    }

    private void waitForWorker() throws InterruptedException {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) AppContext.getAppContext().get(SwingWorker.class);
        while (executorService.getCompletedTaskCount() == 0 || executorService.getActiveCount() > 0) {
            Thread.sleep(100L);
        }
    }

    private DomainEvent<Long, SecuritySummary> newEvent(SecuritySummary... summaries) {
        return new SecuritySummaryEvent(this, EventType.CHANGED, Arrays.asList(summaries));
    }
}
