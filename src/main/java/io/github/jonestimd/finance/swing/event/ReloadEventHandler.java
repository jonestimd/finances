// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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

import java.awt.Component;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.swing.BackgroundTask;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.table.model.BeanTableModel;
import io.github.jonestimd.swing.window.StatusFrame;

import static io.github.jonestimd.finance.swing.BundleType.*;

/**
 * Domain event handler for {@link EventType#RELOAD} events.
 */
public class ReloadEventHandler<ID, T extends UniqueId<ID>> implements DomainEventListener<ID, T> {
    private final Component component;
    private final BackgroundTask<List<T>> task;

    public ReloadEventHandler(Component component, String messageKey, Supplier<List<T>> getTableData, Supplier<BeanTableModel<T>> modelSupplier) {
        this(component, messageKey, getTableData, modelSupplier, UniqueId::isSameId);
    }

    public ReloadEventHandler(Component component, String messageKey, Supplier<List<T>> getTableData, Supplier<BeanTableModel<T>> modelSupplier, BiPredicate<T, T> isEqual) {
        this.component = component;
        this.task = BackgroundTask.task(LABELS.getString(messageKey), getTableData, beans -> modelSupplier.get().updateBeans(beans, isEqual));
    }

    @Override
    public void onDomainEvent(DomainEvent<ID, T> event) {
        if (event.isReload()) {
            StatusFrame window = ComponentTreeUtils.findAncestor(component, StatusFrame.class);
            if (window != null) task.run(component);
        }
    }
}
