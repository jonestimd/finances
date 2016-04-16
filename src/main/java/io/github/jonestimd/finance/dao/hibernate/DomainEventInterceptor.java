// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.dao.hibernate;

import java.io.Serializable;

import com.google.common.base.Supplier;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEventHolder;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public class DomainEventInterceptor extends EmptyInterceptor implements DomainEventRecorder {
    private final Supplier<EventHandlerEventHolder> handlerSupplier;
    private ThreadLocal<EventHandlerEventHolder> eventHandlerHolder = new ThreadLocal<EventHandlerEventHolder>();

    public DomainEventInterceptor(Supplier<EventHandlerEventHolder> handlerSupplier) {
        this.handlerSupplier = handlerSupplier;
    }

    public void afterTransactionCompletion(Transaction tx) {
        super.afterTransactionCompletion(tx);
        eventHandlerHolder.set(null);
    }

    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
            String[] propertyNames, Type[] types) {
        EventHandlerEventHolder eventHandler = eventHandlerHolder.get();
        if (eventHandler != null && entity instanceof UniqueId) {
            eventHandler.changed((UniqueId<?>) entity, propertyNames, previousState);
        }
        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        EventHandlerEventHolder eventHandler = eventHandlerHolder.get();
        if (eventHandler != null && entity instanceof UniqueId) {
            eventHandler.added((UniqueId<?>) entity);
        }
        return super.onSave(entity, id, state, propertyNames, types);
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        EventHandlerEventHolder eventHandler = eventHandlerHolder.get();
        if (eventHandler != null && entity instanceof UniqueId) {
            eventHandler.deleted((UniqueId<?>) entity, propertyNames, state);
        }
        super.onDelete(entity, id, state, propertyNames, types);
    }

    @Override
    public DomainEventHolder beginRecording() {
        EventHandlerEventHolder eventHandler = handlerSupplier.get();
        eventHandlerHolder.set(eventHandler);
        return eventHandler;
    }
}
