package io.github.jonestimd.finance.dao.hibernate;

import java.util.Arrays;

import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

public class CompositeEventHandlerTest {
    @Mock
    private EventHandler handler1 = mock(EventHandler.class);
    @Mock
    private EventHandlerEventHolder handler2 = mock(EventHandlerEventHolder.class);

    @Test
    public void addedDelegatesToHandlers() throws Exception {
        CompositeEventHandler compositeHandler = new CompositeEventHandler(handler1, handler2);

        Transaction entity = new Transaction();
        compositeHandler.added(entity);

        InOrder inOrder = inOrder(handler1, handler2);
        inOrder.verify(handler1).added(entity);
        inOrder.verify(handler2).added(entity);
    }

    @Test
    public void deletedDelegatesToHandlers() throws Exception {
        CompositeEventHandler compositeHandler = new CompositeEventHandler(handler1, handler2);

        Transaction entity = new Transaction();
        String[] propertyNames = new String[0];
        Object[] state = new Object[0];
        compositeHandler.deleted(entity, propertyNames, state);

        InOrder inOrder = inOrder(handler1, handler2);
        inOrder.verify(handler1).deleted(entity, propertyNames, state);
        inOrder.verify(handler2).deleted(entity, propertyNames, state);
    }

    @Test
    public void changedDelegatesToHandlers() throws Exception {
        CompositeEventHandler compositeHandler = new CompositeEventHandler(handler1, handler2);

        Transaction entity = new Transaction();
        String[] propertyNames = new String[0];
        Object[] previousState = new Object[0];
        compositeHandler.changed(entity, propertyNames, previousState);

        InOrder inOrder = inOrder(handler1, handler2);
        inOrder.verify(handler1).changed(entity, propertyNames, previousState);
        inOrder.verify(handler2).changed(entity, propertyNames, previousState);
    }

    @Test
    public void getEventsCollectsEventsFromHolders() throws Exception {
        CompositeEventHandler compositeHandler = new CompositeEventHandler(handler1, handler2);
        DomainEvent<Long, Transaction> event = new DomainEvent<>(this, EventType.ADDED, new Transaction(1L), Transaction.class);
        doReturn(Arrays.<DomainEvent<?, ?>>asList(event)).when(handler2).getEvents();

        assertThat(compositeHandler.getEvents()).containsExactly(event);

        verify(handler2).getEvents();
    }
}
