package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;
import java.util.Collections;

import javax.swing.Icon;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.collection.MapBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.typesafe.config.ConfigValueFactory.*;
import static io.github.jonestimd.finance.stockquote.StockQuotePlugin.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IexTradingQuoteServiceTest extends HttpServerTest {
    @Mock
    private StockQuoteService.Callback callback;

    @Test
    public void enabledByFlagInConfig() throws Exception {
        Config config = ConfigFactory.parseMap(Collections.singletonMap("iextrading.enabled", "true"));

        assertThat(IexTradingQuoteService.FACTORY.isEnabled(config)).isTrue();
    }

    @Test
    public void disabledByDefault() throws Exception {
        assertThat(IexTradingQuoteService.FACTORY.isEnabled(getConfig())).isFalse();
    }

    @Test
    public void factoryCreatesIexTradingService() throws Exception {
        StockQuoteService service = IexTradingQuoteService.FACTORY.create(getConfig());

        assertThat(service).isInstanceOf(IexTradingQuoteService.class);
    }

    @Test
    public void getPricesInvokesCallback() throws Exception {
        String url = BUNDLE.getString("quote.service.iex.attribution.url");
        String message = BUNDLE.getString("quote.service.iex.attribution.tooltip");
        StockQuoteService service = IexTradingQuoteService.FACTORY.create(getConfig());

        service.getPrices(ImmutableList.of("S1", "S2"), callback);

        verify(callback).accept(eq(new MapBuilder<String, BigDecimal>()
                .put("S1", new BigDecimal("45.89"))
                .put("S2", new BigDecimal("55.67")).get()), eq(url), notNull(Icon.class), eq(message));
    }

    @Test
    public void getPricesDoesNotInvokesCallbackOnError() throws Exception {
        StockQuoteService service = IexTradingQuoteService.FACTORY.create(getConfig());

        service.getPrices(ImmutableList.of("S1", "S3"), callback);

        verifyZeroInteractions(callback);
    }

    private Config getConfig() {
        return ConfigFactory.load().getConfig("finances.stockquote")
                .withValue("iextrading.urlFormat", fromAnyRef(getUrl("iextrading-${symbols}.json")))
                .withValue("iextrading.iconUrl", fromAnyRef(getUrl("icon-16x16.png")));
    }
}