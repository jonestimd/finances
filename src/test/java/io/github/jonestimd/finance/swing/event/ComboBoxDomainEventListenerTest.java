package io.github.jonestimd.finance.swing.event;

import java.util.Collections;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.event.PayeeEvent;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.swing.component.BeanListModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ComboBoxDomainEventListenerTest {
    @Mock
    private BeanListModel<Payee> model;

    @Test
    public void addEventInsertsInSortOrder() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(new Payee("aaa"));
        when(model.getElementAt(2)).thenReturn(new Payee("zzz"));
        ComboBoxDomainEventListener<Long, Payee> listener = new ComboBoxDomainEventListener<>(model);

        Payee payee = new Payee(1L, "bbb");
        listener.onDomainEvent(new PayeeEvent(this, EventType.ADDED, payee));

        verify(model).insertElementAt(payee, 2);
    }

    @Test
    public void deleteEventRemovesItem() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(TestDomainUtils.createPayee(1L, "aaa"));
        when(model.getElementAt(2)).thenReturn(TestDomainUtils.createPayee(2L, "bbb"));
        ComboBoxDomainEventListener<Long, Payee> listener = new ComboBoxDomainEventListener<>(model);

        listener.onDomainEvent(new PayeeEvent(this, EventType.DELETED, TestDomainUtils.create(Payee.class, 2L)));

        verify(model).removeElementAt(2);
    }

    @Test
    public void updateEventRemovesThenInsertsInSortOrder() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(TestDomainUtils.createPayee(1L, "bbb"));
        when(model.getElementAt(2)).thenReturn(TestDomainUtils.createPayee(2L, "ccc"));
        ComboBoxDomainEventListener<Long, Payee> listener = new ComboBoxDomainEventListener<>(model);

        Payee payee = TestDomainUtils.createPayee(2L, "aaa");
        listener.onDomainEvent(new PayeeEvent(this, EventType.CHANGED, payee));

        verify(model).removeElementAt(2);
        verify(model).insertElementAt(payee, 1);
    }

    @Test
    public void replaceEventDeletesItem() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(TestDomainUtils.createPayee(1L, "bbb"));
        when(model.getElementAt(2)).thenReturn(TestDomainUtils.createPayee(2L, "ccc"));
        ComboBoxDomainEventListener<Long, Payee> listener = new ComboBoxDomainEventListener<>(model);

        Payee payee = TestDomainUtils.createPayee(2L, "ccc");
        listener.onDomainEvent(new PayeeEvent(this, Collections.singletonList(payee), null));

        verify(model).removeElementAt(2);
    }
}
