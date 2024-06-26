package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class SecurityTransactionTableModelTest {
    private static final int AMOUNT_COLUMN = 6;
    private static final int BALANCE_COLUMN = 7;

    private SecurityTransactionTableModel model = new SecurityTransactionTableModel(new Account());
    private TableModelListener listener = mock(TableModelListener.class);

    @Test
    public void testSetBeans() throws Exception {
        model.addTableModelListener(listener);

        model.setBeans(Arrays.asList(
                createTransaction(createDetail(10d), createDetail(25d)),
                createTransaction(createDetail(30d), createDetail(40d)),
                createTransaction(createDetail(-5d))));

        verify(listener, times(4)).tableChanged(isA(TableModelEvent.class));
        assertThat(model.getValueAt(0, BALANCE_COLUMN)).isEqualTo(new BigDecimal(35d));
        assertThat(model.getValueAt(1, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(2, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(3, BALANCE_COLUMN)).isEqualTo(new BigDecimal(105d));
        assertThat(model.getValueAt(4, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(5, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(6, BALANCE_COLUMN)).isEqualTo(new BigDecimal(100d));
    }

    @Test
    public void testInsertBean() throws Exception {
        model.setBeans(Arrays.asList(
                createTransaction(createDetail(10d), createDetail(25d)),
                createTransaction(createDetail(-5d))));
        model.addTableModelListener(listener);

        model.addBean(1, createTransaction(createDetail(30d), createDetail(40d)));

        ArgumentCaptor<TableModelEvent> capture = ArgumentCaptor.forClass(TableModelEvent.class);
        verify(listener, times(4)).tableChanged(capture.capture());
        verifyEvent(capture.getAllValues().get(0), TableModelEvent.INSERT, 3, 5, -1);
        verifyEvent(capture.getAllValues().get(1), TableModelEvent.UPDATE, 3, 5, -1);
        verifyEvent(capture.getAllValues().get(2), TableModelEvent.UPDATE, 3, 3, BALANCE_COLUMN);
        verifyEvent(capture.getAllValues().get(3), TableModelEvent.UPDATE, 6, 6, BALANCE_COLUMN);
        assertThat(model.getValueAt(0, BALANCE_COLUMN)).isEqualTo(new BigDecimal(35d));
        assertThat(model.getValueAt(1, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(2, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(3, BALANCE_COLUMN)).isEqualTo(new BigDecimal(105d));
        assertThat(model.getValueAt(4, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(5, BALANCE_COLUMN)).isNull();
        assertThat(model.getValueAt(6, BALANCE_COLUMN)).isEqualTo(new BigDecimal(100d));
    }

    @Test
    public void testRemoveAll() throws Exception {
        model.setBeans(Arrays.asList(
                createTransaction(createDetail(10d), createDetail(25d)),
                createTransaction(createDetail(11d), createDetail(32d)),
                createTransaction(createDetail(-5d))));
        model.addTableModelListener(listener);

        model.removeAll(new ArrayList<>(model.getBeans().subList(0, 2)));

        ArgumentCaptor<TableModelEvent> capture = ArgumentCaptor.forClass(TableModelEvent.class);
        verify(listener, times(5)).tableChanged(capture.capture());
        assertThat(capture.getAllValues().get(0).getType()).isEqualTo(TableModelEvent.DELETE);
        assertThat(capture.getAllValues().get(0).getFirstRow()).isEqualTo(0);
        assertThat(capture.getAllValues().get(0).getLastRow()).isEqualTo(2);

        assertThat(capture.getAllValues().get(1).getType()).isEqualTo(TableModelEvent.UPDATE);
        assertThat(capture.getAllValues().get(1).getFirstRow()).isEqualTo(0);
        assertThat(capture.getAllValues().get(1).getLastRow()).isEqualTo(0);
        assertThat(capture.getAllValues().get(1).getColumn()).isEqualTo(BALANCE_COLUMN);

        assertThat(capture.getAllValues().get(2).getType()).isEqualTo(TableModelEvent.UPDATE);
        assertThat(capture.getAllValues().get(2).getFirstRow()).isEqualTo(3);
        assertThat(capture.getAllValues().get(2).getLastRow()).isEqualTo(3);
        assertThat(capture.getAllValues().get(2).getColumn()).isEqualTo(BALANCE_COLUMN);

        assertThat(capture.getAllValues().get(3).getType()).isEqualTo(TableModelEvent.DELETE);
        assertThat(capture.getAllValues().get(3).getFirstRow()).isEqualTo(0);
        assertThat(capture.getAllValues().get(3).getLastRow()).isEqualTo(2);

        assertThat(capture.getAllValues().get(4).getType()).isEqualTo(TableModelEvent.UPDATE);
        assertThat(capture.getAllValues().get(4).getFirstRow()).isEqualTo(0);
        assertThat(capture.getAllValues().get(4).getLastRow()).isEqualTo(0);
        assertThat(capture.getAllValues().get(4).getColumn()).isEqualTo(BALANCE_COLUMN);

        assertThat(model.getValueAt(0, BALANCE_COLUMN)).isEqualTo(new BigDecimal(-5d));
    }

    @Test
    public void testRemoveBean() throws Exception {
        model.setBeans(Arrays.asList(
                createTransaction(createDetail(10d), createDetail(25d)),
                createTransaction(createDetail(11d), createDetail(32d)),
                createTransaction(createDetail(-5d))));
        model.addTableModelListener(listener);

        model.removeBean(model.getBean(1));

        ArgumentCaptor<TableModelEvent> capture = ArgumentCaptor.forClass(TableModelEvent.class);
        verify(listener, times(2)).tableChanged(capture.capture());
        verifyEvent(capture.getAllValues().get(0), TableModelEvent.DELETE, 3, 5, -1);
        verifyEvent(capture.getAllValues().get(1), TableModelEvent.UPDATE, 3, 3, BALANCE_COLUMN);
        assertThat(model.getValueAt(0, BALANCE_COLUMN)).isEqualTo(new BigDecimal(35d));
        assertThat(model.getValueAt(3, BALANCE_COLUMN)).isEqualTo(new BigDecimal(30d));
    }

    @Test
    public void testSetBean() throws Exception {
        model.setBeans(Arrays.asList(
                createTransaction(createDetail(10d), createDetail(25d)),
                createTransaction(createDetail(11d), createDetail(32d)),
                createTransaction(createDetail(-5d))));
        model.addTableModelListener(listener);

        model.setBean(1, createTransaction(createDetail(22d), createDetail(33d)));

        ArgumentCaptor<TableModelEvent> capture = ArgumentCaptor.forClass(TableModelEvent.class);
        verify(listener, times(3)).tableChanged(capture.capture());
        verifyEvent(capture.getAllValues().get(0), TableModelEvent.UPDATE, 3, 5, -1);
        verifyEvent(capture.getAllValues().get(1), TableModelEvent.UPDATE, 3, 3, BALANCE_COLUMN);
        assertThat(model.getValueAt(0, BALANCE_COLUMN)).isEqualTo(new BigDecimal(35d));
        assertThat(model.getValueAt(3, BALANCE_COLUMN)).isEqualTo(new BigDecimal(90d));
    }

    private void verifyEvent(TableModelEvent event, int type, int firstRow, int lastRow, int column) {
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getFirstRow()).isEqualTo(firstRow);
        assertThat(event.getLastRow()).isEqualTo(lastRow);
        assertThat(event.getColumn()).isEqualTo(column);
    }

    private TransactionDetail createDetail(double amount) {
        TransactionDetail detail = new TransactionDetail();
        detail.setAmount(new BigDecimal(amount));
        return detail;
    }

    private Transaction createTransaction(TransactionDetail ... details) {
        return new TransactionBuilder().nextId().details(details).get();
    }
}