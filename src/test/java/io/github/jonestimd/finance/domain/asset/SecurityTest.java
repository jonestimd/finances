package io.github.jonestimd.finance.domain.asset;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class SecurityTest {
    @Test
    public void getSplitRatioReturnsOneForNullSplits() throws Exception {
        assertThat(new Security().getSplitRatio(null, null).getRatio(2).toString()).isEqualTo("1.00");
    }

    @Test
    public void getSplitRatioReturnsOneForEmptySplits() throws Exception {
        Security security = new SecurityBuilder().splits().get();

        assertThat(security.getSplitRatio(null, null).getRatio(2).toString()).isEqualTo("1.00");
    }

    @Test
    public void getSplitRatioReturnsOneForNullFromDate() throws Exception {
        Security security = new SecurityBuilder().splits(new StockSplit()).get();

        assertThat(security.getSplitRatio(null, null).getRatio(2).toString()).isEqualTo("1.00");
    }

    @Test
    public void getSplitRatioReturnsProductOfSplitsAfterFromDate() throws Exception {
        Security security = new SecurityBuilder().splits(
                newStockSplit(-4, 4L),
                newStockSplit(-1, 2L),
                newStockSplit(-2, 3L)).get();

        assertThat(security.getSplitRatio(newDate(-3), null).getRatio(2).toString()).isEqualTo("6.00");
    }

    @Test
    public void getSplitRatioReturnsProductOfSplitsBetweenDates() throws Exception {
        Security security = new SecurityBuilder().splits(
                newStockSplit(-4, 4L),
                newStockSplit(-3, 3L),
                newStockSplit(-2, 2L),
                newStockSplit(-1, 5L)).get();

        assertThat(security.getSplitRatio(newDate(-3), newDate(-2)).getRatio(2).toString()).isEqualTo("6.00");
    }

    private StockSplit newStockSplit(int dateOffset, long sharesOut) {
        return new StockSplit(null, newDate(dateOffset), new SplitRatio(BigDecimal.ONE, new BigDecimal(sharesOut)));
    }

    private Date newDate(int dateOffset) {
        return DateUtils.truncate(DateUtils.addDays(new Date(), dateOffset), Calendar.DAY_OF_MONTH);
    }
}