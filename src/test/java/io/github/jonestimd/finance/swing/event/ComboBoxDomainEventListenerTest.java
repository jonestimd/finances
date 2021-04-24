package io.github.jonestimd.finance.swing.event;

import java.util.Collections;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ComboBoxDomainEventListenerTest {
    @Mock
    private BeanListComboBoxModel<TransactionType> model;

    @Test
    public void addEventInsertsInSortOrder() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(new Account(1L, "aaa"));
        when(model.getElementAt(2)).thenReturn(new Account(2L, "zzz"));
        ComboBoxDomainEventListener<Long, TransactionType> listener = new ComboBoxDomainEventListener<>(model);

        Account account = new Account(3L, "bbb");
        listener.onDomainEvent(new DomainEvent<>(this, EventType.ADDED, account, TransactionType.class));

        verify(model).insertElementAt(account, 2);
    }

    @Test
    public void addEventAppendsList() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(new Account(1L, "aaa"));
        when(model.getElementAt(2)).thenReturn(new Account(2L, "bbb"));
        ComboBoxDomainEventListener<Long, TransactionType> listener = new ComboBoxDomainEventListener<>(model);

        Account account = new Account(3L, "zzz");
        listener.onDomainEvent(new DomainEvent<>(this, EventType.ADDED, account, TransactionType.class));

        verify(model).insertElementAt(account, 3);
    }

    @Test
    public void deleteEventRemovesItem() throws Exception {
        when(model.getSize()).thenReturn(4);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(new Account(1L, "aaa"));
        when(model.getElementAt(2)).thenReturn(new TransactionCategory(2L));
        when(model.getElementAt(3)).thenReturn(new Account(2L, "zzz"));
        ComboBoxDomainEventListener<Long, TransactionType> listener = new ComboBoxDomainEventListener<>(model);

        listener.onDomainEvent(new DomainEvent<>(this, EventType.DELETED, new Account(2L), TransactionType.class));

        verify(model).removeElementAt(3);
    }

    @Test
    public void deleteEventIgnoresUnknownItem() throws Exception {
        when(model.getSize()).thenReturn(4);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(new Account(1L, "aaa"));
        when(model.getElementAt(2)).thenReturn(new TransactionCategory(2L));
        when(model.getElementAt(3)).thenReturn(new Account(2L, "zzz"));
        ComboBoxDomainEventListener<Long, TransactionType> listener = new ComboBoxDomainEventListener<>(model);

        listener.onDomainEvent(new DomainEvent<>(this, EventType.DELETED, new Account(3L), TransactionType.class));

        verify(model, never()).removeElementAt(anyInt());
    }

    @Test
    public void updateEventRemovesThenInsertsInSortOrder() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(new Account(1L, "bbb"));
        when(model.getElementAt(2)).thenReturn(new Account(2L, "ccc"));
        ComboBoxDomainEventListener<Long, TransactionType> listener = new ComboBoxDomainEventListener<>(model);

        Account account = new Account(2L, "aaa");
        listener.onDomainEvent(new DomainEvent<>(this, EventType.CHANGED, account, TransactionType.class));

        verify(model).removeElementAt(2);
        verify(model).insertElementAt(account, 1);
    }

    @Test
    public void replaceEventDeletesItem() throws Exception {
        when(model.getSize()).thenReturn(3);
        when(model.getElementAt(0)).thenReturn(null);
        when(model.getElementAt(1)).thenReturn(new Account(1L, "bbb"));
        when(model.getElementAt(2)).thenReturn(new Account(2L, "ccc"));
        ComboBoxDomainEventListener<Long, TransactionType> listener = new ComboBoxDomainEventListener<>(model);

        Account account = new Account(2L, "ccc");
        listener.onDomainEvent(new DomainEvent<>(this, Collections.singletonList(account), null, TransactionType.class));

        verify(model).removeElementAt(2);
    }
}
