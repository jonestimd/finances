package io.github.jonestimd.finance.yahoo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class JsonMapperTest {
    @Test
    public void getTableReturnsAnnotationValue() throws Exception {
        assertThat(new JsonMapper<StockQuote>(StockQuote.class).getTable()).isEqualTo("yahoo.finance.quote");
    }

    @Test
    public void getResultNameReturnsAnnotationValue() throws Exception {
        assertThat(new JsonMapper<StockQuote>(StockQuote.class).getResultName()).isEqualTo("quote");
    }

    @Test
    public void getColumsReturnsAnnotationValues() throws Exception {
        assertThat(new JsonMapper<StockQuote>(StockQuote.class).getColumns()).containsOnly(StockQuote.SYMBOL, StockQuote.LAST_PRICE);
    }

    @Test
    public void fromResultParsesObject() throws Exception {
        JsonMapper<StockQuote> jsonMapper = new JsonMapper<StockQuote>(StockQuote.class);
        InputStream stream = new ByteArrayInputStream(("{\"query\": {\"results\": {\"quote\": " + toJson("SS", "123.456") + "}}}").getBytes());

        List<StockQuote> stockQuotes = jsonMapper.fromResult(stream);

        assertThat(stockQuotes).hasSize(1);
        assertThat(stockQuotes.get(0).getSymbol()).isEqualTo("SS");
        assertThat(stockQuotes.get(0).getLastPrice()).isEqualTo(new BigDecimal("123.456"));
    }

    @Test
    public void fromResultParsesList() throws Exception {
        JsonMapper<StockQuote> jsonMapper = new JsonMapper<StockQuote>(StockQuote.class);
        InputStream stream = new ByteArrayInputStream(("{\"query\": {\"results\": {\"quote\": ["
                + toJson("SS", "123.456") + ","
                + toJson("XX", "234.561") + "]}}}").getBytes());

        List<StockQuote> stockQuotes = jsonMapper.fromResult(stream);

        assertThat(stockQuotes).hasSize(2);
        assertThat(stockQuotes.get(0).getSymbol()).isEqualTo("SS");
        assertThat(stockQuotes.get(0).getLastPrice()).isEqualTo(new BigDecimal("123.456"));
        assertThat(stockQuotes.get(1).getSymbol()).isEqualTo("XX");
        assertThat(stockQuotes.get(1).getLastPrice()).isEqualTo(new BigDecimal("234.561"));
    }

    private String toJson(String symbol, String lastPrice) {
        return String.format("{\"symbol\": \"%s\", \"LastTradePriceOnly\": \"%s\"}", symbol, lastPrice);
    }
}
