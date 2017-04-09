package io.github.jonestimd.finance.dao.hibernate;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class EventBuilderTest {
    private static final Function<DomainEvent, Class<?>> GET_CLASS = DomainEvent::getDomainClass;
    private static final Function<DomainEvent, EventType> GET_TYPE = DomainEvent::getType;

    @SuppressWarnings("unchecked")
    private Collection<Transaction> getTransactions(DomainEvent<?, ?> event) {
        return (Collection<Transaction>) event.getDomainObjects();
    }

    @SuppressWarnings("unchecked")
    private Collection<TransactionDetail> getDetails(DomainEvent<?, ?> event) {
        return (Collection<TransactionDetail>) event.getDomainObjects();
    }

    @Test
    public void addedTransaction() throws Exception {
        EventBuilder eventBuilder = new EventBuilder(this);
        Transaction transaction = new Transaction(1L);

        eventBuilder.added(transaction);

        List<DomainEvent<?, ?>> events = eventBuilder.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(Transaction.class);
        assertThat(events.get(0).getType()).isEqualTo(EventType.ADDED);
        assertThat(getTransactions(events.get(0))).containsOnly(transaction);
    }

    @Test
    public void addedTransactionDetail() throws Exception {
        EventBuilder eventBuilder = new EventBuilder(this);
        TransactionDetail detail = new TransactionDetailBuilder().nextId().get();
        detail.setTransaction(new Transaction(1L));

        eventBuilder.added(detail);

        List<DomainEvent<?, ?>> events = eventBuilder.getEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getDomainClass()).isEqualTo(TransactionDetail.class);
        assertThat(events.get(0).getType()).isEqualTo(EventType.ADDED);
        assertThat(getDetails(events.get(0))).containsOnly(detail);
        assertThat(events.get(1).getDomainClass()).isEqualTo(Transaction.class);
        assertThat(events.get(1).getType()).isEqualTo(EventType.CHANGED);
        assertThat(getTransactions(events.get(1))).containsOnly(detail.getTransaction());
    }

    @Test
    public void changedTransaction() throws Exception {
        EventBuilder eventBuilder = new EventBuilder(this);
        Transaction transaction = new Transaction(1L);

        eventBuilder.changed(transaction, null, null);

        List<DomainEvent<?, ?>> events = eventBuilder.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(Transaction.class);
        assertThat(events.get(0).getType()).isEqualTo(EventType.CHANGED);
        assertThat(getTransactions(events.get(0))).containsOnly(transaction);
    }

    @Test
    public void changedTransactionDetail() throws Exception {
        EventBuilder eventBuilder = new EventBuilder(this);
        TransactionDetail detail = new TransactionDetailBuilder().nextId().get();
        detail.setTransaction(new Transaction(1L));

        eventBuilder.changed(detail, null, null);

        List<DomainEvent<?, ?>> events = eventBuilder.getEvents();
        assertThat(events).hasSize(2);
        assertThat(Iterables.transform(events, GET_CLASS)).containsOnly(TransactionDetail.class, Transaction.class);
        assertThat(Iterables.transform(events, GET_TYPE)).containsOnly(EventType.CHANGED);
    }

    @Test
    public void deletedTransaction() throws Exception {
        EventBuilder eventBuilder = new EventBuilder(this);
        TransactionDetail detail = new TransactionDetailBuilder().nextId().get();
        detail.setTransaction(new Transaction(1L));

        eventBuilder.deleted(detail.getTransaction(), null, null);
        eventBuilder.deleted(detail, null, null);

        List<DomainEvent<?, ?>> events = eventBuilder.getEvents();
        assertThat(events).hasSize(2);
        assertThat(Iterables.transform(events, GET_CLASS)).containsOnly(TransactionDetail.class, Transaction.class);
        assertThat(Iterables.transform(events, GET_TYPE)).containsOnly(EventType.DELETED);
    }

    @Test
    public void deletedTransactionDetail() throws Exception {
        EventBuilder eventBuilder = new EventBuilder(this);
        TransactionDetail detail = new TransactionDetailBuilder().nextId().get();
        Transaction transaction = new Transaction(1L);

        eventBuilder.deleted(detail, new String[] {"id", TransactionDetail.TRANSACTION}, new Object[] {detail.getId(), transaction});

        List<DomainEvent<?, ?>> events = eventBuilder.getEvents();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getDomainClass()).isEqualTo(Transaction.class);
        assertThat(events.get(0).getType()).isEqualTo(EventType.CHANGED);
        assertThat(getTransactions(events.get(0))).containsOnly(transaction);
        assertThat(events.get(1).getDomainClass()).isEqualTo(TransactionDetail.class);
        assertThat(events.get(1).getType()).isEqualTo(EventType.DELETED);
        assertThat(getDetails(events.get(1))).containsOnly(detail);
    }
}
