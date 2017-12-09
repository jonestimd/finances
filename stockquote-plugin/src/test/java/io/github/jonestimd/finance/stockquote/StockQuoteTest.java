package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;

import io.github.jonestimd.finance.stockquote.StockQuote.QuoteStatus;
import org.junit.Test;

import static io.github.jonestimd.finance.stockquote.StockQuote.QuoteStatus.*;
import static org.assertj.core.api.Assertions.*;

public class StockQuoteTest {
    @Test
    public void compareToSortsNullsFirst() throws Exception {
        assertThat(new StockQuote(null, null, null, null, null).compareTo(null)).isEqualTo(1);
    }

    @Test
    public void compareToSortsByStatus() throws Exception {
        assertThat(newStockQuote(PENDING).compareTo(newStockQuote(PENDING))).isEqualTo(0);
        assertThat(newStockQuote(PENDING).compareTo(newStockQuote(NOT_AVAILABLE))).isEqualTo(-1);
        assertThat(newStockQuote(NOT_AVAILABLE).compareTo(newStockQuote(PENDING))).isEqualTo(1);
        assertThat(newStockQuote(NOT_AVAILABLE).compareTo(newStockQuote(SUCCESSFUL))).isEqualTo(-1);
        assertThat(newStockQuote(SUCCESSFUL).compareTo(newStockQuote(NOT_AVAILABLE))).isEqualTo(1);
    }

    @Test
    public void compareToSortsByPrice() throws Exception {
        assertThat(newStockQuote(BigDecimal.ONE).compareTo(newStockQuote(BigDecimal.ONE))).isEqualTo(0);
        assertThat(newStockQuote(BigDecimal.ONE).compareTo(newStockQuote(BigDecimal.TEN))).isEqualTo(-1);
        assertThat(newStockQuote(BigDecimal.TEN).compareTo(newStockQuote(BigDecimal.ONE))).isEqualTo(1);
    }

    private StockQuote newStockQuote(QuoteStatus status) {
        return new StockQuote(null, status, null, null, null, null);
    }

    private StockQuote newStockQuote(BigDecimal price) {
        return new StockQuote(null, SUCCESSFUL, price, null, null, null);
    }
}