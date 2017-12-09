package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;
import java.util.Collections;

import javax.swing.Icon;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.typesafe.config.ConfigValueFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AlphaVantageQuoteServiceTest extends HttpServerTest {
    public static final String MY_API_KEY = "my-api-key";
    @Mock
    private StockQuoteService.Callback callback;

    @Test
    public void enabledForConfigWithApiKey() throws Exception {
        Config config = ConfigFactory.parseMap(Collections.singletonMap("alphavantage.apiKey", "key"))
                .withValue("alphavantage.iconUrl", fromAnyRef(getUrl("icon-16x16.png")));

        assertThat(AlphaVantageQuoteService.FACTORY.isEnabled(config)).isTrue();
    }

    @Test
    public void disabledForConfigWithoutApiKey() throws Exception {
        Config config = ConfigFactory.empty();

        assertThat(AlphaVantageQuoteService.FACTORY.isEnabled(config)).isFalse();
    }

    @Test
    public void factoryCreatesAlphaVantageService() throws Exception {
        StockQuoteService service = AlphaVantageQuoteService.FACTORY.create(getConfig());

        assertThat(service).isInstanceOf(AlphaVantageQuoteService.class);
    }

    @Test
    public void getPricesInvokesCallback() throws Exception {
        String url = getUrl("alphavantage-S1.json");
        StockQuoteService service = AlphaVantageQuoteService.FACTORY.create(getConfig());

        service.getPrices(Collections.singletonList("S1"), callback);

        verify(callback).accept(eq(Collections.singletonMap("S1", new BigDecimal("87.65"))), eq(url), notNull(Icon.class), eq(url));
    }

    @Test
    public void getPricesDoesNotInvokeCallbackForErrorResponse() throws Exception {
        StockQuoteService service = AlphaVantageQuoteService.FACTORY.create(getConfig());

        service.getPrices(Collections.singletonList("S3"), callback);

        verifyZeroInteractions(callback);
    }

    @Test
    public void getPricesDoesNotInvokeCallbackForNotFound() throws Exception {
        StockQuoteService service = AlphaVantageQuoteService.FACTORY.create(getConfig());

        service.getPrices(Collections.singletonList("S2"), callback);

        verifyZeroInteractions(callback);
    }

    private Config getConfig() {
        return ConfigFactory.load().getConfig("finances.stockquote")
                .withValue("alphavantage.urlFormat", fromAnyRef(getUrl("alphavantage-${symbol}.json")))
                .withValue("alphavantage.iconUrl", fromAnyRef(getUrl("icon-16x16.png")))
                .withValue("alphavantage.apiKey", fromAnyRef(MY_API_KEY));
    }
}