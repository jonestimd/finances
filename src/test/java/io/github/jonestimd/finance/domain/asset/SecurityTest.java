package io.github.jonestimd.finance.domain.asset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

public class SecurityTest {
    @Test
    public void getSplitRatioReturnsOneForNullSplits() throws Exception {
        assertThat(new Security().getSplitRatio(newDate(-1), null).getRatio(2).toString()).isEqualTo("1.00");
    }

    @Test
    public void getSplitRatioReturnsOneForEmptySplits() throws Exception {
        Security security = new SecurityBuilder().get();
        security.setSplits(new ArrayList<>());

        assertThat(security.getSplitRatio(newDate(-1), null).getRatio(2).toString()).isEqualTo("1.00");
    }

    @Test
    public void getSplitRatioReturnsOneForNullFromDate() throws Exception {
        Security security = new SecurityBuilder().splits(new StockSplit()).get();

        assertThat(security.getSplitRatio(null, null).getRatio(2).toString()).isEqualTo("1.00");
    }

    @Test
    public void getSplitRationReturnsOneForNullFromDate() throws Exception {
        Security security = new SecurityBuilder().splits(
                newStockSplit(-2, 3L)).get();

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

    @Test
    public void applySplitsMultipliesBySplitsBetweenDates() throws Exception {
        Security security = new SecurityBuilder().splits(
                newStockSplit(-1, 2L)).get();

        assertThat(security.applySplits(BigDecimal.TEN, newDate(-1), null).toString()).isEqualTo("20.000000");
    }

    @Test
    public void reverseSplitsDividesBySplitsBetweenDates() throws Exception {
        Security security = new SecurityBuilder().splits(
                newStockSplit(-1, 2L)).get();

        assertThat(security.revertSplits(BigDecimal.TEN, newDate(-1), null).toString()).isEqualTo("5.000000");
    }

    @Test
    public void updateSplitsAddsNewSplit() throws Exception {
        Security security = new SecurityBuilder().splits().get();
        StockSplit split = new StockSplit(security, new Date(), new SplitRatio());

        security.updateSplits(singletonList(split));

        assertThat(security.getSplits()).containsExactly(split);
    }

    @Test
    public void updateSplitsAddsUnknownSplit() throws Exception {
        Security security = new SecurityBuilder().splits().get();
        StockSplit split = TestDomainUtils.setId(new StockSplit());

        security.updateSplits(singletonList(split));

        assertThat(security.getSplits()).containsExactly(split);
    }

    @Test
    public void updateSplitsUpdatesKnownSplit() throws Exception {
        StockSplit split = TestDomainUtils.setId(new StockSplit());
        Security security = new SecurityBuilder().splits(split).get();
        StockSplit updated = TestDomainUtils.setId(newStockSplit(-1, 2), split.getId());

        security.updateSplits(singletonList(updated));

        assertThat(security.getSplits()).containsExactly(split);
        assertThat(split.getDate()).isEqualTo(updated.getDate());
        assertThat(split.getSharesIn()).isEqualTo(updated.getSharesIn());
        assertThat(split.getSharesOut()).isEqualTo(updated.getSharesOut());
    }

    @Test
    public void updateSplitsRemovesOldSplit() throws Exception {
        Security security = new SecurityBuilder().splits(newStockSplit(-1, 2)).get();

        security.updateSplits(emptyList());

        assertThat(security.getSplits()).isEmpty();
    }

    private StockSplit newStockSplit(int dateOffset, long sharesOut) {
        return new StockSplit(null, newDate(dateOffset), new SplitRatio(BigDecimal.ONE, new BigDecimal(sharesOut)));
    }

    private Date newDate(int dateOffset) {
        return DateUtils.truncate(DateUtils.addDays(new Date(), dateOffset), Calendar.DAY_OF_MONTH);
    }
}