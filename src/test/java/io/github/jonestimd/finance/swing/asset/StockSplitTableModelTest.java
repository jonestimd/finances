package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.domain.asset.SplitRatio;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static io.github.jonestimd.mockito.MockitoHelper.matches;
import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StockSplitTableModelTest {
    @Mock
    private TableModelListener listener;
    private final Date date = new Date();

    @Test
    public void updatingDateValidatesAllDates() throws Exception {
        StockSplitTableModel model = new StockSplitTableModel(Arrays.asList(newSplit(0, 1, 2), newSplit(1, 1, 2)));
        model.addTableModelListener(listener);
        assertThat(model.isNoErrors()).isTrue();

        model.setValueAt(DateUtils.addDays(date, 1), 0, 0);

        assertThat(model.validateAt(0, 0)).isNotNull();
        assertThat(model.validateAt(1, 0)).isNotNull();
        verify(listener).tableChanged(matches(new TableModelEvent(model, 0, Integer.MAX_VALUE, StockSplitTableModel.DATE_INDEX)));
    }

    @Test
    public void updatingSharesInValidatesSharesOut() throws Exception {
        StockSplitTableModel model = new StockSplitTableModel(Arrays.asList(newSplit(0, 1, 2), newSplit(1, 1, 2)));
        model.addTableModelListener(listener);
        assertThat(model.isNoErrors()).isTrue();

        model.setValueAt(new BigDecimal(2), 0, 1);

        assertThat(model.validateAt(0, 1)).isNotNull();
        assertThat(model.validateAt(0, 2)).isNotNull();
        verify(listener).tableChanged(matches(new TableModelEvent(model, 0, 0, 1)));
        verify(listener).tableChanged(matches(new TableModelEvent(model, 0, 0, 2)));
    }

    @Test
    public void updatingSharesOutValidatesSharesIn() throws Exception {
        StockSplitTableModel model = new StockSplitTableModel(Arrays.asList(newSplit(0, 1, 2), newSplit(1, 1, 2)));
        model.addTableModelListener(listener);
        assertThat(model.isNoErrors()).isTrue();

        model.setValueAt(new BigDecimal(1), 0, 2);

        assertThat(model.validateAt(0, 1)).isNotNull();
        assertThat(model.validateAt(0, 2)).isNotNull();
        verify(listener).tableChanged(matches(new TableModelEvent(model, 0, 0, 1)));
        verify(listener).tableChanged(matches(new TableModelEvent(model, 0, 0, 2)));
    }

    @Test
    public void addingRowsValidatesAllDates() throws Exception {
        StockSplitTableModel model = new StockSplitTableModel(Arrays.asList(newSplit(0, 1, 2), newSplit(1, 1, 2)));
        model.addTableModelListener(listener);
        assertThat(model.isNoErrors()).isTrue();

        model.queueAdd(newSplit(1, 1, 2));

        assertThat(model.validateAt(1, 0)).isNotNull();
        assertThat(model.validateAt(2, 0)).isNotNull();
        verify(listener).tableChanged(matches(new TableModelEvent(model, 0, Integer.MAX_VALUE, StockSplitTableModel.DATE_INDEX)));
    }

    @Test
    public void removingRowsValidatesAllDates() throws Exception {
        StockSplitTableModel model = new StockSplitTableModel(Arrays.asList(newSplit(0, 1, 2), newSplit(1, 1, 2)));
        assertThat(model.isNoErrors()).isTrue();
        model.queueAdd(newSplit(1, 1, 2));
        assertThat(model.isNoErrors()).isFalse();
        model.addTableModelListener(listener);

        model.queueDelete(model.getBean(2));

        assertThat(model.isNoErrors()).isTrue();
        verify(listener).tableChanged(matches(new TableModelEvent(model, 0, Integer.MAX_VALUE, StockSplitTableModel.DATE_INDEX)));
    }

    private StockSplit newSplit(int dateOffset, int sharesIn, int sharesOut) {
        return new StockSplit(null, DateUtils.addDays(date, dateOffset), new SplitRatio(new BigDecimal(sharesIn), new BigDecimal(sharesOut)));
    }
}