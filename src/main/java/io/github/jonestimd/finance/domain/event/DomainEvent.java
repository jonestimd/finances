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
package io.github.jonestimd.finance.domain.event;

import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import io.github.jonestimd.collection.ImmutableCollector;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.util.Streams;

public class DomainEvent<ID, T extends UniqueId<ID>> extends EventObject {
    private final EventType type;
    private final Map<Object, List<T>> domainObjects;
    private final T replacement;
    private final Class<T> domainClass;

    private static <ID, T extends UniqueId<ID>> Map<ID, List<T>> index(Iterable<T> items) {
        return Streams.of(items).collect(Collectors.groupingBy(UniqueId::getId, ImmutableCollector.toList()));
    }

    public DomainEvent(Object source, EventType type, T domainObject, Class<T> domainClass) {
        this(source, type, Collections.singletonMap(domainObject.getId(), Collections.singletonList(domainObject)), null, domainClass);
    }

    public DomainEvent(Object source, EventType type, Iterable<T> domainObjects, Class<T> domainClass) {
        this(source, type, index(domainObjects), null, domainClass);
    }

    public DomainEvent(Object source, Iterable<T> domainObjects, T replacement, Class<T> domainClass) {
        this(source, EventType.REPLACED, index(domainObjects), replacement, domainClass);
    }

    private DomainEvent(Object source, EventType type, Map<ID, List<T>> idMap, T replacement, Class<T> domainClass) {
        super(source);
        this.type = type;
        this.domainObjects = Collections.unmodifiableMap(idMap);
        this.replacement = replacement;
        this.domainClass = domainClass;
    }

    public EventType getType() {
        return type;
    }

    public boolean isAdd() {
        return type == EventType.ADDED;
    }

    public boolean isChange() {
        return type == EventType.CHANGED;
    }

    public boolean isDelete() {
        return type == EventType.DELETED;
    }

    public boolean isReplace() {
        return type == EventType.REPLACED;
    }

    public Class<T> getDomainClass() {
        return domainClass;
    }

    public Collection<T> getDomainObjects() {
        return domainObjects.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public T getDomainObject(Object id) {
        if (getDomainObjects(id).size() > 1) { // can be multiple events for a SecuritySummary
            throw new IllegalArgumentException("multiple objects for id: " + id.toString());
        }
        return Iterables.getFirst(getDomainObjects(id), null);
    }

    public Collection<T> getDomainObjects(Object id) {
        return domainObjects.getOrDefault(id, Collections.emptyList());
    }

    public T getReplacement() {
        return replacement;
    }

    public boolean contains(T entity) {
        return domainObjects.containsKey(entity.getId());
    }

    public boolean containsAttribute(TransactionSummary<?> summary) {
        return domainObjects.containsKey(summary.getTransactionAttribute().getId());
    }
}