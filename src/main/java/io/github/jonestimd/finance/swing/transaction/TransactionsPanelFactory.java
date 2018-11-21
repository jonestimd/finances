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

import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.SwingContext;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.SelectAccountAction;
import io.github.jonestimd.swing.window.PanelFactory;

public class TransactionsPanelFactory implements PanelFactory<SelectAccountAction> {
    private final ServiceLocator serviceLocator;
    private final TransactionTableModelCache transactionModelCache;
    private final SwingContext context;
    private final DomainEventPublisher eventPublisher;
    private final AccountsMenuFactory accountsMenuFactory;
    private final ImportFileMenuFactory importFileMenuFactory;

    public TransactionsPanelFactory(ServiceLocator serviceLocator, SwingContext context, DomainEventPublisher eventPublisher) {
        this.serviceLocator = serviceLocator;
        this.transactionModelCache = new TransactionTableModelCache(eventPublisher);
        this.context = context;
        this.accountsMenuFactory = new AccountsMenuFactory(serviceLocator.getAccountOperations(), context, eventPublisher);
        this.importFileMenuFactory = new ImportFileMenuFactory(serviceLocator, context.getTableFactory());
        this.eventPublisher = eventPublisher;
    }

    public TransactionsPanel createPanel(SelectAccountAction action) {
        TransactionsPanel transactionsPanel = createPanel();
        transactionsPanel.setSelectedAccount(action.getAccount());
        return transactionsPanel;
    }

    public TransactionsPanel createPanel() {
        return new TransactionsPanel(serviceLocator, transactionModelCache, context, accountsMenuFactory, importFileMenuFactory, eventPublisher);
    }
}