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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
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
        assertEquals(new BigDecimal(35d), model.getValueAt(0, BALANCE_COLUMN));
        assertNull(model.getValueAt(1, BALANCE_COLUMN));
        assertNull(model.getValueAt(2, BALANCE_COLUMN));
        assertEquals(new BigDecimal(105d), model.getValueAt(3, BALANCE_COLUMN));
        assertNull(model.getValueAt(4, BALANCE_COLUMN));
        assertNull(model.getValueAt(5, BALANCE_COLUMN));
        assertEquals(new BigDecimal(100d), model.getValueAt(6, BALANCE_COLUMN));
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
        assertEquals(new BigDecimal(35d), model.getValueAt(0, BALANCE_COLUMN));
        assertNull(model.getValueAt(1, BALANCE_COLUMN));
        assertNull(model.getValueAt(2, BALANCE_COLUMN));
        assertEquals(new BigDecimal(105d), model.getValueAt(3, BALANCE_COLUMN));
        assertNull(model.getValueAt(4, BALANCE_COLUMN));
        assertNull(model.getValueAt(5, BALANCE_COLUMN));
        assertEquals(new BigDecimal(100d), model.getValueAt(6, BALANCE_COLUMN));
    }

    @Test
    public void testRemoveAll() throws Exception {
        model.setBeans(Arrays.asList(
                createTransaction(createDetail(10d), createDetail(25d)),
                createTransaction(createDetail(11d), createDetail(32d)),
                createTransaction(createDetail(-5d))));
        model.addTableModelListener(listener);

        model.removeAll(new ArrayList<Transaction>(model.getBeans().subList(0, 2)));

        ArgumentCaptor<TableModelEvent> capture = ArgumentCaptor.forClass(TableModelEvent.class);
        verify(listener, times(5)).tableChanged(capture.capture());
        assertEquals(TableModelEvent.DELETE, capture.getAllValues().get(0).getType());
        assertEquals(0, capture.getAllValues().get(0).getFirstRow());
        assertEquals(2, capture.getAllValues().get(0).getLastRow());

        assertEquals(TableModelEvent.UPDATE, capture.getAllValues().get(1).getType());
        assertEquals(0, capture.getAllValues().get(1).getFirstRow());
        assertEquals(0, capture.getAllValues().get(1).getLastRow());
        assertEquals(BALANCE_COLUMN, capture.getAllValues().get(1).getColumn());

        assertEquals(TableModelEvent.UPDATE, capture.getAllValues().get(2).getType());
        assertEquals(3, capture.getAllValues().get(2).getFirstRow());
        assertEquals(3, capture.getAllValues().get(2).getLastRow());
        assertEquals(BALANCE_COLUMN, capture.getAllValues().get(2).getColumn());

        assertEquals(TableModelEvent.DELETE, capture.getAllValues().get(3).getType());
        assertEquals(0, capture.getAllValues().get(3).getFirstRow());
        assertEquals(2, capture.getAllValues().get(3).getLastRow());

        assertEquals(TableModelEvent.UPDATE, capture.getAllValues().get(4).getType());
        assertEquals(0, capture.getAllValues().get(4).getFirstRow());
        assertEquals(0, capture.getAllValues().get(4).getLastRow());
        assertEquals(BALANCE_COLUMN, capture.getAllValues().get(4).getColumn());

        assertEquals(new BigDecimal(-5d), model.getValueAt(0, BALANCE_COLUMN));
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
        assertEquals(new BigDecimal(35d), model.getValueAt(0, BALANCE_COLUMN));
        assertEquals(new BigDecimal(30d), model.getValueAt(3, BALANCE_COLUMN));
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
        assertEquals(new BigDecimal(35d), model.getValueAt(0, BALANCE_COLUMN));
        assertEquals(new BigDecimal(90d), model.getValueAt(3, BALANCE_COLUMN));
    }

    @Test
    public void testSetValueAt() throws Exception {
        model.setBeans(Arrays.asList(
                createTransaction(createDetail(10d), createDetail(25d)),
                createTransaction(createDetail(11d), createDetail(32d)),
                createTransaction(createDetail(-5d))));
        model.addTableModelListener(listener);

        model.setValueAt(new BigDecimal(15d), 1, AMOUNT_COLUMN);

        ArgumentCaptor<TableModelEvent> capture = ArgumentCaptor.forClass(TableModelEvent.class);
        verify(listener, times(19)).tableChanged(capture.capture());
        verifyEvent(capture.getAllValues().get(0), TableModelEvent.UPDATE, 1, 1, AMOUNT_COLUMN);
        // should validate other detail columns
        verifyEvent(capture.getAllValues().get(1), TableModelEvent.UPDATE, 1, 1, 0);
        verifyEvent(capture.getAllValues().get(2), TableModelEvent.UPDATE, 1, 1, 1);
        verifyEvent(capture.getAllValues().get(3), TableModelEvent.UPDATE, 1, 1, 2);
        verifyEvent(capture.getAllValues().get(4), TableModelEvent.UPDATE, 1, 1, 3);
        verifyEvent(capture.getAllValues().get(5), TableModelEvent.UPDATE, 1, 1, 4);
        verifyEvent(capture.getAllValues().get(6), TableModelEvent.UPDATE, 1, 1, 5);
        verifyEvent(capture.getAllValues().get(7), TableModelEvent.UPDATE, 1, 1, BALANCE_COLUMN);
        // should validate header row
        verifyEvent(capture.getAllValues().get(8), TableModelEvent.UPDATE, 0, 0, 0);
        verifyEvent(capture.getAllValues().get(9), TableModelEvent.UPDATE, 0, 0, 1);
        verifyEvent(capture.getAllValues().get(10), TableModelEvent.UPDATE, 0, 0, 2);
        verifyEvent(capture.getAllValues().get(11), TableModelEvent.UPDATE, 0, 0, 3);
        verifyEvent(capture.getAllValues().get(12), TableModelEvent.UPDATE, 0, 0, 4);
        verifyEvent(capture.getAllValues().get(13), TableModelEvent.UPDATE, 0, 0, 5);
        verifyEvent(capture.getAllValues().get(14), TableModelEvent.UPDATE, 0, 0, AMOUNT_COLUMN);
        verifyEvent(capture.getAllValues().get(15), TableModelEvent.UPDATE, 0, 0, BALANCE_COLUMN);
        // update balances
        verifyEvent(capture.getAllValues().get(16), TableModelEvent.UPDATE, 0, 0, BALANCE_COLUMN);
        verifyEvent(capture.getAllValues().get(17), TableModelEvent.UPDATE, 3, 3, BALANCE_COLUMN);
        verifyEvent(capture.getAllValues().get(18), TableModelEvent.UPDATE, 6, 6, BALANCE_COLUMN);
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