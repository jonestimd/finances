// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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
package io.github.jonestimd.finance.swing.transaction;

import javax.swing.JToolBar;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.swing.window.FrameManager;

import static io.github.jonestimd.finance.swing.event.SingletonFrameActions.*;

/**
 * Extends {@link TransactionSummaryTablePanel} to include the {@code Accounts} window button in the toolbar.
 */
public abstract class AccountAccessPanel<S extends UniqueId<Long> & Comparable<? super S>, T extends TransactionSummary<S>>
        extends TransactionSummaryTablePanel<S, T> {
    protected final FrameManager<WindowType> frameManager;

    public AccountAccessPanel(DomainEventPublisher domainEventPublisher, DecoratedTable<T, ? extends ValidatedBeanListTableModel<T>> table,
            String resourceGroup, FrameManager<WindowType> frameManager) {
        super(domainEventPublisher, table, resourceGroup);
        this.frameManager = frameManager;
    }

    @Override
    protected void addActions(JToolBar toolbar) {
        toolbar.add(ComponentFactory.newToolbarButton(forAccounts(toolbar, frameManager)));
        toolbar.add(ComponentFactory.newMenuBarSeparator());
        super.addActions(toolbar);
    }
}
