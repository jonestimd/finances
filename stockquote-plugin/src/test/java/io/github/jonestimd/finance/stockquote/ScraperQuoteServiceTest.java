package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Collections.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ScraperQuoteServiceTest extends HttpServerTest {
    @Mock
    private StockQuoteService.Callback callback;

    @Test
    public void isEnabledWhenScrapersIsNotEmpty() throws Exception {
        Config config = ConfigFactory.parseString("{scrapers = [{urlFormat = \"http://localhost\"}]}");

        assertThat(ScraperQuoteService.FACTORY.isEnabled(config)).isTrue();
    }

    @Test
    public void isDisabledWhenScrapersIsMissing() throws Exception {
        Config config = ConfigFactory.empty();

        assertThat(ScraperQuoteService.FACTORY.isEnabled(config)).isFalse();
    }

    @Test
    public void isDisabledWhenScrapersIsEmpty() throws Exception {
        Config config = ConfigFactory.parseString("{scrapers = []}");

        assertThat(ScraperQuoteService.FACTORY.isEnabled(config)).isFalse();
    }

    @Test
    public void factoryCreatesHierarchicalService() throws Exception {
        Config scraper = getConfig("#quote > span:nth-child(1)", "#quote > span:nth-child(2)");
        Config config = ConfigFactory.parseMap(singletonMap("scrapers", ConfigValueFactory.fromIterable(singleton(scraper.root()))));

        StockQuoteService service = ScraperQuoteService.FACTORY.create(config);

        assertThat(service).isInstanceOf(HierarchicalQuoteService.class);
    }

    @Test
    public void getPricesUsesTagContent() throws Exception {
        String url = getUrl("quote-S1.html");
        ScraperQuoteService service = new ScraperQuoteService(getConfig("#quote > span:nth-child(1)", "#quote > span:nth-child(2)"));

        service.getPrices(Collections.singletonList("S1"), callback);

        verify(callback).accept(singletonMap("S1", new BigDecimal("25.00")), url, IconLoader.DEFAULT_ICON, url);
    }

    @Test
    public void getPricesUsesAttributes() throws Exception {
        String url = getUrl("quote-S1.html");
        Config config = getConfig("#quote > meta[itemprop=symbol]", "content", "#quote > meta[itemprop=price]", "content");
        ScraperQuoteService service = new ScraperQuoteService(config);

        service.getPrices(Collections.singletonList("S1"), callback);

        verify(callback).accept(singletonMap("S-1", new BigDecimal("15.00")), url, IconLoader.DEFAULT_ICON, url);
    }

    @Test
    public void callbackNotInvokedWhenSymbolNotFound() throws Exception {
        ScraperQuoteService service = new ScraperQuoteService(getConfig("#quote > span:nth-child(3)", "#quote > span:nth-child(2)"));

        service.getPrices(Collections.singletonList("S1"), callback);

        verifyZeroInteractions(callback);
    }

    @Test
    public void callbackNotifiedWhenPriceNotFound() throws Exception {
        String url = getUrl("quote-S1.html");
        ScraperQuoteService service = new ScraperQuoteService(getConfig("#quote > span:nth-child(1)", "#quote > span:nth-child(3)"));

        service.getPrices(Collections.singletonList("S1"), callback);

        verify(callback).accept(singletonMap("S1", null), url, IconLoader.DEFAULT_ICON, url);
    }

    private Config getConfig(String symbolQuery, String symbolAttr, String priceQuery, String priceAttr) {
        return getConfig(symbolQuery, priceQuery).withValue("priceSelector.attribute", ConfigValueFactory.fromAnyRef(priceAttr))
                .withValue("symbolSelector.attribute", ConfigValueFactory.fromAnyRef(symbolAttr));
    }

    private Config getConfig(String symbolQuery, String priceQuery) {
        return  ConfigFactory.parseMap(ImmutableMap.of(
                "urlFormat", getUrl("quote-${symbol}.html"),
                "symbolSelector.query", symbolQuery,
                "priceSelector.query", priceQuery
        ));
    }
}