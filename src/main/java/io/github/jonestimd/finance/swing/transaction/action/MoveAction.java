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
package io.github.jonestimd.finance.swing.transaction.action;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.SelectionDialog;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.AccountFormat;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.swing.action.DialogAction;

public class MoveAction extends DialogAction {
    private final TransactionTable table;
    private final TransactionService transactionService;
    private final Supplier<Stream<Account>> accountsSupplier;
    private final DomainEventPublisher domainEventPublisher;
    private final SelectionDialog<Account> dialog;
    private List<? extends DomainEvent<?, ?>> domainEvents;

    public MoveAction(TransactionTable table, TransactionService transactionService, DomainEventPublisher domainEventPublisher,
                      Supplier<Stream<Account>> accountsSupplier) {
        this(table, transactionService, domainEventPublisher, accountsSupplier,
                new SelectionDialog<>(table, "dialog.selectAccount.", "accountList", new AccountFormat()));
    }

    protected MoveAction(TransactionTable table, TransactionService transactionService, DomainEventPublisher domainEventPublisher,
                      Supplier<Stream<Account>> accountsSupplier, SelectionDialog<Account> dialog) {
        super(BundleType.LABELS.get(), "action.transaction.move");
        this.table = table;
        this.domainEventPublisher = domainEventPublisher;
        this.accountsSupplier = accountsSupplier;
        this.transactionService = transactionService;
        this.dialog = dialog;
        table.getSelectionModel().addListSelectionListener(this::updateEnabled);
    }

    private boolean isNotSelectedAccount(Account account) {
        return ! account.getId().equals(table.getModel().getAccount().getId());
    }

    private boolean isValidAccount(Account account) {
        return account.getType().isSecurity() || ! table.getSelectedTransaction().isSecurity();
    }

    @Override
    protected void loadDialogData() {
    }

    @Override
    protected boolean displayDialog(JComponent owner) {
        List<Account> accounts = accountsSupplier.get()
                .filter(this::isNotSelectedAccount).filter(this::isValidAccount)
                .collect(Collectors.toList());
        Collections.sort(accounts);
        dialog.pack();
        return dialog.show(accounts);
    }

    @Override
    protected void saveDialogData() {
        domainEvents = transactionService.moveTransaction(table.getSelectedTransaction(), dialog.getSelectedItem());
    }

    @Override
    protected void setSaveResultOnUI() {
        domainEvents.stream().forEach(domainEventPublisher::publishEvent);
    }

    private void updateEnabled(ListSelectionEvent event) {
        Transaction transaction = table.getSelectedTransaction();
        setEnabled(table.getSelectedRowCount() == 1 && transaction != null && !transaction.isNew());
    }
}
