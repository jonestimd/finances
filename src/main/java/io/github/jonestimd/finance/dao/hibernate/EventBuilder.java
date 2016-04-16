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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.swing.event.EventType;

@SuppressWarnings("rawtypes")
public class EventBuilder implements EventHandlerEventHolder {
    private static final String[] EMPTY_PROPERTY_NAMES = new String[0];
    private static final Object[] EMPTY_STATE = new Object[0];

    private final Object eventSource;
    private final SetMultimap<Class<? extends UniqueId>, UniqueId<?>> added = HashMultimap.create();
    private final SetMultimap<Class<? extends UniqueId>, UniqueId<?>> changed = HashMultimap.create();
    private final SetMultimap<Class<? extends UniqueId>, UniqueId<?>> deleted = HashMultimap.create();

    public EventBuilder(Object eventSource) {
        this.eventSource = eventSource;
    }

    public void added(UniqueId<?> domainObject) {
        added.put(domainObject.getClass(), domainObject);
        rollupDetail(domainObject, EMPTY_PROPERTY_NAMES, EMPTY_STATE);
    }

    public void changed(UniqueId<?> domainObject, String[] propertyNames, Object[] previousState) {
        changed.put(domainObject.getClass(), domainObject);
        rollupDetail(domainObject, propertyNames, previousState);
    }

    public void deleted(UniqueId<?> domainObject, String[] propertyNames, Object[] previousState) {
        deleted.put(domainObject.getClass(), domainObject);
        rollupDetail(domainObject, propertyNames, previousState);
    }

    private void rollupDetail(UniqueId<?> domainObject, String[] propertyNames, Object[] previousState) {
        if (domainObject instanceof TransactionDetail) {
            TransactionDetail detail = (TransactionDetail) domainObject;
            Transaction transaction = detail.getTransaction() == null ? getTransaction(propertyNames, previousState) : detail.getTransaction();
            if (transaction != null) {
                changed.put(Transaction.class, transaction);
            }
        }
    }

    private Transaction getTransaction(String[] propertyNames, Object[] state) {
        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(TransactionDetail.TRANSACTION)) {
                return (Transaction) state[i];
            }
        }
        return null;
    }

    public List<DomainEvent<?, ?>> getEvents() {
        for (Entry<Class<? extends UniqueId>, Collection<UniqueId<?>>> entry : deleted.asMap().entrySet()) {
            changed.get(entry.getKey()).removeAll(entry.getValue());
        }
        List<DomainEvent<?, ?>> events = new ArrayList<>();
        addEvents(events, EventType.ADDED, added);
        addEvents(events, EventType.CHANGED, changed);
        addEvents(events, EventType.DELETED, deleted);
        return events;
    }

    @SuppressWarnings("unchecked")
    private void addEvents(List<DomainEvent<?, ?>> events, EventType type, Multimap<Class<? extends UniqueId>, UniqueId<?>> map) {
        if (! map.isEmpty()) {
            for (Entry<Class<? extends UniqueId>, Collection<UniqueId<?>>> entry : map.asMap().entrySet()) {
                events.add(newEvent(type, Class.class.cast(entry.getKey()), Collection.class.cast(entry.getValue())));
            }
        }
    }

    private <ID, T extends UniqueId<ID>> DomainEvent<ID, T> newEvent(EventType type, Class<T> domainClass, Collection<T> domainObjects) {
        return new DomainEvent<>(eventSource, type, domainObjects, domainClass);
    }
}
