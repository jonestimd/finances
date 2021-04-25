package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;
import java.util.Collections;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.typesafe.config.ConfigValueFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QuandlQuoteServiceTest extends HttpServerTest {
    public static final String MY_API_KEY = "my-api-key";
    @Mock
    private StockQuoteService.Callback callback;

    @Test
    public void disabledForIncompleteConfig() throws Exception {
        Config config = ConfigFactory.parseMap(Collections.singletonMap("quandl.apiKey", "key"))
                .withValue("quandl.iconUrl", fromAnyRef(getUrl("icon-16x16.png")));

        assertThat(QuandlQuoteService.FACTORY.create(config)).isEmpty();
    }

    @Test
    public void disabledForConfigWithoutApiKey() throws Exception {
        Config config = ConfigFactory.empty();

        assertThat(QuandlQuoteService.FACTORY.create(config)).isEmpty();
    }

    @Test
    public void factoryCreatesQuandlService() throws Exception {
        StockQuoteService service = QuandlQuoteService.FACTORY.create(getConfig()).get();

        assertThat(service).isInstanceOf(QuandlQuoteService.class);
    }

    @Test
    public void getPricesInvokesCallback() throws Exception {
        String url = getUrl("quandl-S1.json");
        StockQuoteService service = QuandlQuoteService.FACTORY.create(getConfig()).get();

        service.getPrices(Collections.singletonList("S1"), callback);

        verify(callback).accept(eq(Collections.singletonMap("S1", new BigDecimal("35.00"))), eq(url), notNull(), eq(url));
    }

    @Test
    public void getPricesDoesNotInvokeCallbackWhenNoPrice() throws Exception {
        StockQuoteService service = QuandlQuoteService.FACTORY.create(getConfig()).get();

        service.getPrices(Collections.singletonList("S2"), callback);

        verifyNoInteractions(callback);
    }

    private Config getConfig() {
        return ConfigFactory.load().getConfig("finances.stockquote")
                .withValue("quandl.urlFormat", fromAnyRef(getUrl("quandl-${symbol}.json")))
                .withValue("quandl.iconUrl", fromAnyRef(getUrl("icon-16x16.png")))
                .withValue("quandl.apiKey", fromAnyRef(MY_API_KEY));
    }
}