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
package io.github.jonestimd.finance.swing.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import org.apache.log4j.Logger;

/**
 * This class publishes {@link DomainEvent}s to registered listeners.  This class only maintains
 * {@link WeakReference}s to the listeners.  <strong>Once there are no strong refereces to a listener, it will no
 * longer receive events.</strong>
 * <p>
 * This class is not thread-safe.  It is intendend for use only on the AWT event thread.
 */
public class DomainEventPublisher {
    private static final Logger logger = Logger.getLogger(DomainEventPublisher.class);
    private List<EventListener<?, ?>> listeners = new ArrayList<>();

    /**
     * Register a listener for domain events.
     */
    public <ID, T extends UniqueId<ID>> void register(Class<T> domainClass, DomainEventListener<ID, T> listener) {
        logger.debug("register " + domainClass.getSimpleName() + ", " + listener);
        listeners.add(new EventListener<>(domainClass, listener));
    }

    /**
     * Unregister a listener for domain events.
     */
    public <ID, T extends UniqueId<ID>> void unregister(Class<T> domainClass, DomainEventListener<ID, T> listener) {
        listeners.removeIf(eventListener -> eventListener.isInactive() || eventListener.matches(domainClass, listener));
    }

    @SuppressWarnings("unchecked")
    public <ID, T extends UniqueId<ID>, E extends DomainEvent<ID, T>> void publishEvent(E event) {
        Iterator<EventListener<?, ?>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            EventListener<?, ?> eventListener = iterator.next();
            if (eventListener.isInactive()) {
                logger.debug("reaped " + eventListener.domainClass.getSimpleName());
                iterator.remove();
            }
            else if (eventListener.handlesEvent(event)) eventListener.sendEvent(DomainEvent.class.cast(event));
        }
    }

    private class EventListener<ID, T extends UniqueId<ID>> {
        private final Class<T> domainClass;
        private final WeakReference<DomainEventListener<ID, T>> listener;

        public EventListener(Class<T> domainClass, DomainEventListener<ID, T> listener) {
            this.domainClass = domainClass;
            this.listener = new WeakReference<>(listener);
        }

        public boolean isInactive() {
            return listener.get() == null;
        }

        public boolean matches(Class<?> domainClass, DomainEventListener<?,?> listener) {
            return this.domainClass == domainClass && this.listener.get() == listener;
        }

        public boolean handlesEvent(DomainEvent<?, ?> event) {
            Class<?> domainClass = event.getDomainClass();
            return domainClass == null || this.domainClass.isAssignableFrom(domainClass);
        }

        public void sendEvent(DomainEvent<ID, T> event) {
            listener.get().onDomainEvent(event);
        }
    }
}