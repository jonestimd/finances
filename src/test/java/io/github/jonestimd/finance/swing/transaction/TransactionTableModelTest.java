package io.github.jonestimd.finance.swing.transaction;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Arrays;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import org.fest.util.Objects;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class TransactionTableModelTest {
    @Test
    public void appendDetail() throws Exception {
        TableModelListener listener = mock(TableModelListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Arrays.asList(new Transaction(null, null, null, false, null)));
        model.addTableModelListener(listener);

        model.queueAppendSubRow(model.getRowCount());

        assertThat(model.getBean(0).getDetails().size()).isEqualTo(1);
        assertThat(model.getRowCount()).isEqualTo(2);
        ArgumentCaptor<TableModelEvent> eventCaptor = ArgumentCaptor.forClass(TableModelEvent.class);
        verify(listener).tableChanged(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getFirstRow()).isEqualTo(1);
        assertThat(eventCaptor.getValue().getLastRow()).isEqualTo(1);
    }

    @Test
    public void setBeansUpdatesClearedBalance() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.addPropertyChangeListener(TransactionTableModel.CLEARED_BALANCE_PROPERTY, listener);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ZERO);

        model.setBeans(Lists.newArrayList(newTransaction(true, BigDecimal.TEN)));
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.TEN);
        verify(listener).propertyChange(event(BigDecimal.ZERO, BigDecimal.TEN));

        model.setBeans(Lists.newArrayList(newTransaction(true, BigDecimal.ONE), newTransaction(false, BigDecimal.TEN)));
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ONE);
        verify(listener).propertyChange(event(BigDecimal.TEN, BigDecimal.ONE));
    }

    @Test
    public void addBeanUpdatesClearedBalance() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.addPropertyChangeListener(TransactionTableModel.CLEARED_BALANCE_PROPERTY, listener);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ZERO);

        model.addBean(newTransaction(true, BigDecimal.TEN));
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.TEN);
        verify(listener).propertyChange(event(BigDecimal.ZERO, BigDecimal.TEN));

        model.addBean(0, newTransaction(true, BigDecimal.TEN));
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("20"));
        verify(listener).propertyChange(event(BigDecimal.TEN, new BigDecimal("20")));

        model.addBean(0, newTransaction(false, BigDecimal.TEN));
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("20"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void removeBeanUpdatesClearedBalance() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Lists.newArrayList(
                newTransaction(true, BigDecimal.TEN),
                newTransaction(true, BigDecimal.ONE),
                newTransaction(false, BigDecimal.TEN),
                newTransaction(false, BigDecimal.ONE)));
        model.addPropertyChangeListener(TransactionTableModel.CLEARED_BALANCE_PROPERTY, listener);
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("11"));

        model.removeBean(model.getBeans().get(0));
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ONE);
        verify(listener).propertyChange(event(new BigDecimal("11"), BigDecimal.ONE));
        
        model.removeBean(model.getBeans().get(2));
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ONE);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void setBeanUpdatesClearedBalance() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Lists.newArrayList(
                newTransaction(true, BigDecimal.TEN),
                newTransaction(true, BigDecimal.ONE),
                newTransaction(false, BigDecimal.TEN),
                newTransaction(false, BigDecimal.ONE)));
        model.addPropertyChangeListener(TransactionTableModel.CLEARED_BALANCE_PROPERTY, listener);
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("11"));

        model.setBean(2, newTransaction(true, BigDecimal.TEN));
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("21")).as("replace not cleared with cleared");
        verify(listener).propertyChange(event(new BigDecimal("11"), new BigDecimal("21")));

        model.setBean(2, newTransaction(true, BigDecimal.ONE));
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("12")).as("replace cleared with new amount");
        verify(listener).propertyChange(event(new BigDecimal("21"), new BigDecimal("12")));

        model.setBean(2, newTransaction(false, BigDecimal.TEN));
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("11")).as("replace cleared with not cleared");
        verify(listener).propertyChange(event(new BigDecimal("12"), new BigDecimal("11")));
        
        model.setBean(2, newTransaction(false, BigDecimal.ONE));
        assertThat(model.getClearedBalance()).isEqualTo(new BigDecimal("11")).as("replace not cleared with not cleared");
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void changeAmountUpdatesClearedBalance() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Lists.newArrayList(
                newTransaction(true, BigDecimal.TEN),
                newTransaction(false, BigDecimal.TEN)));
        model.addPropertyChangeListener(TransactionTableModel.CLEARED_BALANCE_PROPERTY, listener);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.TEN);

        model.setValueAt(BigDecimal.ONE, 1, 5);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ONE);
        verify(listener).propertyChange(event(BigDecimal.TEN, BigDecimal.ONE));

        model.setValueAt(BigDecimal.ONE, 3, 5);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ONE);
        verifyNoMoreInteractions(listener);
        
        model.setValueAt(null, 1, 5);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ZERO);
        verify(listener).propertyChange(event(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    public void removeDetailUpdatesClearedBalance() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Lists.newArrayList(
                newTransaction(true, BigDecimal.TEN),
                newTransaction(false, BigDecimal.TEN)));
        model.addPropertyChangeListener(TransactionTableModel.CLEARED_BALANCE_PROPERTY, listener);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.TEN);
        
        model.queueAppendSubRow(0);
        model.setValueAt(BigDecimal.ONE, 2, 5);
        model.queueDelete(2);

        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.TEN);
        verify(listener).propertyChange(event(BigDecimal.TEN, new BigDecimal("11")));
        verify(listener).propertyChange(event(new BigDecimal("11"), BigDecimal.TEN));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void changeClearedUpdatesClearedBalance() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Lists.newArrayList(
                newTransaction(false, BigDecimal.TEN),
                newTransaction(false, BigDecimal.TEN)));
        model.addPropertyChangeListener(TransactionTableModel.CLEARED_BALANCE_PROPERTY, listener);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ZERO);

        model.setValueAt(true, 0, 4);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.TEN);
        verify(listener).propertyChange(event(BigDecimal.ZERO, BigDecimal.TEN));

        model.setValueAt(false, 0, 4);
        assertThat(model.getClearedBalance()).isEqualTo(BigDecimal.ZERO);
        verify(listener).propertyChange(event(BigDecimal.TEN, BigDecimal.ZERO));
    }

    @Test
    public void changePayeeOnNewTransactionFiresPropertyChangeEvent() throws Exception {
        PropertyChangeListener listener = mock(PropertyChangeListener.class);
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Lists.newArrayList(
                newTransaction(false, BigDecimal.TEN),
                newTransaction(false, BigDecimal.TEN)));
        model.addPropertyChangeListener(TransactionTableModel.NEW_TRANSACTION_PAYEE_PROPERTY, listener);
        Payee payee1 = new Payee("payee1 1");
        Payee payee2 = new Payee("payee1 2");

        model.setValueAt(payee1, 0, 2);
        model.setValueAt(payee2, 2, 2);

        verify(listener).propertyChange(event(TransactionTableModel.NEW_TRANSACTION_PAYEE_PROPERTY, null, payee2));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void isUnsavedChanges() throws Exception {
        TransactionTableModel model = new TransactionTableModel(new Account());
        model.setBeans(Lists.newArrayList(
                newTransaction(false, BigDecimal.TEN),
                newTransaction(false, BigDecimal.TEN)));
        assertThat(model.isUnsavedChanges()).isFalse();

        model.setValueAt(BigDecimal.ONE, 1, model.amountColumn);

        assertThat(model.isUnsavedChanges()).isTrue();
    }

    private Transaction newTransaction(boolean cleared, BigDecimal amount) throws Exception {
        return new TransactionBuilder()
            .nextId()
            .cleared(cleared)
            .details(new TransactionDetailBuilder().amount(amount).get()).get();
    }

    private PropertyChangeEvent event(Object oldValue, Object newValue) {
        return argThat(new PropertyChangeMatcher(null, oldValue, newValue));
    }

    private PropertyChangeEvent event(String propertyName, Object oldValue, Object newValue) {
        return argThat(new PropertyChangeMatcher(propertyName, oldValue, newValue));
    }

    private static class PropertyChangeMatcher extends BaseMatcher<PropertyChangeEvent> {
        private final String name;
        private final Object oldValue;
        private final Object newValue;

        public PropertyChangeMatcher(String name, Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.name = name;
        }

        @Override
        public boolean matches(Object argument) {
            PropertyChangeEvent event = (PropertyChangeEvent) argument;
            return (name == null || Objects.areEqual(name, event.getPropertyName()))
                    && Objects.areEqual(oldValue, event.getOldValue())
                    && Objects.areEqual(newValue, event.getNewValue());
        }

        @Override
        public void describeTo(Description description) {
            description
                .appendText("propertyName=").appendValue(name)
                .appendText(" oldValue=").appendValue(oldValue)
                .appendText(" newValue=").appendValue(newValue);
        }
    }
}
