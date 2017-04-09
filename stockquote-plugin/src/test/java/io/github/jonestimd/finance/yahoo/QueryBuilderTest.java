package io.github.jonestimd.finance.yahoo;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class QueryBuilderTest {
    @Test
    public void constructorBuildsSelectFrom() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder(new JsonMapper<StockQuote>(StockQuote.class));

        assertThat(queryBuilder.get()).isEqualTo("select LastTradePriceOnly,symbol from yahoo.finance.quote");
    }

    @Test
    public void emptyWhereIn() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder(new JsonMapper<StockQuote>(StockQuote.class))
                .where(StockQuote.SYMBOL).in(Collections.<String>emptyList());

        assertThat(queryBuilder.get()).isEqualTo("select LastTradePriceOnly,symbol from yahoo.finance.quote");
    }

    @Test
    public void singleWhereIn() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder(new JsonMapper<StockQuote>(StockQuote.class))
                .where(StockQuote.SYMBOL).in(Arrays.asList("YHOO", "GOOG"));

        assertThat(queryBuilder.get()).isEqualTo("select LastTradePriceOnly,symbol from yahoo.finance.quote" +
                " where symbol in (\"YHOO\",\"GOOG\")");
    }

    @Test
    public void multipleWhereIn() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder(new JsonMapper<StockQuote>(StockQuote.class))
                .where(StockQuote.SYMBOL).in(Arrays.asList("YHOO", "GOOG"))
                .where(StockQuote.SYMBOL).in(Arrays.asList("YHOO", "GOOG"));

        assertThat(queryBuilder.get()).isEqualTo("select LastTradePriceOnly,symbol from yahoo.finance.quote" +
                " where symbol in (\"YHOO\",\"GOOG\") and symbol in (\"YHOO\",\"GOOG\")");
    }
}
