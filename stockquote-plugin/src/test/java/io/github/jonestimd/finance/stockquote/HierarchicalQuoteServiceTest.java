package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.Sets;
import io.github.jonestimd.finance.stockquote.StockQuoteService.Callback;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HierarchicalQuoteServiceTest {
    @Mock
    private StockQuoteService service1;
    @Mock
    private StockQuoteService service2;
    @Mock
    private Callback callback;

    @Test
    public void callsServicesInOrder() throws Exception {
        doAnswer(getAnswer("S1", BigDecimal.ONE)).when(service1).getPrices(anyListOf(String.class), any(Callback.class));
        doAnswer(getAnswer("S3", BigDecimal.TEN)).when(service2).getPrices(anyListOf(String.class), any(Callback.class));
        HierarchicalQuoteService service = new HierarchicalQuoteService(Arrays.asList(service1, service2));

        service.getPrices(Arrays.asList("S1", "S2", "S3"), callback);

        verify(service1).getPrices(eq(Sets.newHashSet("S1", "S2", "S3")), any(Callback.class));
        verify(service2).getPrices(eq(Sets.newHashSet("S2", "S3")), any(Callback.class));
        verify(callback).accept(Collections.singletonMap("S1", BigDecimal.ONE), null, null, null);
        verify(callback).accept(Collections.singletonMap("S3", BigDecimal.TEN), null, null, null);
    }

    @Test
    public void quitsWhenAllPricesFound() throws Exception {
        doAnswer(getAnswer("S1", BigDecimal.ONE)).when(service1).getPrices(anyListOf(String.class), any(Callback.class));
        HierarchicalQuoteService service = new HierarchicalQuoteService(Arrays.asList(service1, service2));

        service.getPrices(Arrays.asList("S1"), callback);

        verify(service1).getPrices(eq(Sets.newHashSet("S1")), any(Callback.class));
        verifyZeroInteractions(service2);
        verify(callback).accept(Collections.singletonMap("S1", BigDecimal.ONE), null, null, null);
    }

    private Answer getAnswer(String symbol, BigDecimal price) {
        return invocation -> {
            Callback internal = (Callback) invocation.getArguments()[1];
            internal.accept(Collections.singletonMap(symbol, price), null, null, null);
            return null;
        };
    }
}