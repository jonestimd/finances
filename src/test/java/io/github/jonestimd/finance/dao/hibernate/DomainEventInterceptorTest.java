package io.github.jonestimd.finance.dao.hibernate;

import com.google.common.base.Supplier;
import io.github.jonestimd.finance.domain.event.DomainEventHolder;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class DomainEventInterceptorTest {
    private final EventHandlerEventHolder handler = new EventBuilder(this);
    private final Supplier<EventHandlerEventHolder> handlerSupplier = new Supplier<EventHandlerEventHolder>() {
        @Override
        public EventHandlerEventHolder get() {
            return handler;
        }
    };
    private DomainEventInterceptor interceptor = new DomainEventInterceptor(handlerSupplier);

    @Test
    public void afterTransactionCompleteResetsEventBuilder() throws Exception {
        Transaction transaction = new Transaction(1L);
        DomainEventHolder eventHolder = interceptor.beginRecording();
        interceptor.onFlushDirty(transaction, 1L, null, null, null, null);

        interceptor.afterTransactionCompletion(null);
        interceptor.onFlushDirty(new Transaction(-1L), -1L, null, null, null, null);

        assertThat(eventHolder.getEvents()).hasSize(1);
        assertThat(eventHolder.getEvents().get(0).isChange()).isTrue();
        assertThat(eventHolder.getEvents().get(0).getDomainObject(transaction.getId())).isSameAs(transaction);
    }

    @Test
    public void onFlushDirtyHandlesNoEventBuilder() throws Exception {
        interceptor.afterTransactionCompletion(null);

        interceptor.onFlushDirty(new Transaction(), -1L, null, null, null, null);
    }

    @Test
    public void onFlushDirtyIgnoresNullEntity() throws Exception {
        DomainEventHolder eventHolder = interceptor.beginRecording();

        interceptor.onFlushDirty(null, -1L, null, null, null, null);

        assertThat(eventHolder.getEvents()).isEmpty();
    }

    @Test
    public void onFlushDirtyRecordsChangedEntity() throws Exception {
        Transaction transaction = new Transaction(1L);
        DomainEventHolder eventHolder = interceptor.beginRecording();

        interceptor.onFlushDirty(transaction, 1L, null, null, null, null);

        assertThat(eventHolder.getEvents()).hasSize(1);
        assertThat(eventHolder.getEvents().get(0).isChange()).isTrue();
        assertThat(eventHolder.getEvents().get(0).getDomainObject(transaction.getId())).isSameAs(transaction);
    }

    @Test
    public void onSaveHandlesNoEventBuilder() throws Exception {
        interceptor.afterTransactionCompletion(null);

        interceptor.onSave(new Transaction(), -1L, null, null, null);
    }

    @Test
    public void onSaveIgnoresNullEntity() throws Exception {
        DomainEventHolder eventHolder = interceptor.beginRecording();

        interceptor.onSave(null, -1L, null, null, null);

        assertThat(eventHolder.getEvents()).isEmpty();
    }

    @Test
    public void onSaveRecordsAddedEntity() throws Exception {
        Transaction transaction = new Transaction(1L);
        DomainEventHolder eventHolder = interceptor.beginRecording();

        interceptor.onSave(transaction, transaction.getId(), null, null, null);

        assertThat(eventHolder.getEvents()).hasSize(1);
        assertThat(eventHolder.getEvents().get(0).isAdd()).isTrue();
        assertThat(eventHolder.getEvents().get(0).getDomainObject(transaction.getId())).isSameAs(transaction);
    }

    @Test
    public void onDeleteHandlesNoEventBuilder() throws Exception {
        interceptor.afterTransactionCompletion(null);

        interceptor.onDelete(new Transaction(), -1L, null, null, null);
    }

    @Test
    public void onDeleteIgnoresNullEntity() throws Exception {
        DomainEventHolder eventHolder = interceptor.beginRecording();

        interceptor.onDelete(null, -1L, null, null, null);

        assertThat(eventHolder.getEvents()).isEmpty();
    }

    @Test
    public void onDeleteRecordsAddedEntity() throws Exception {
        Transaction transaction = new Transaction(1L);
        DomainEventHolder eventHolder = interceptor.beginRecording();

        interceptor.onDelete(transaction, transaction.getId(), null, null, null);

        assertThat(eventHolder.getEvents()).hasSize(1);
        assertThat(eventHolder.getEvents().get(0).isDelete()).isTrue();
        assertThat(eventHolder.getEvents().get(0).getDomainObject(transaction.getId())).isSameAs(transaction);
    }
}
