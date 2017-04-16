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
package io.github.jonestimd.finance.swing.account;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToolBar;

import com.google.common.collect.Iterables;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.event.AccountEvent;
import io.github.jonestimd.finance.domain.event.CompanyEvent;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.AccountSelector;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.event.TransactionsWindowEvent;
import io.github.jonestimd.finance.swing.transaction.TransactionSummaryTablePanel;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.action.DialogAction;
import io.github.jonestimd.swing.table.TableFactory;
import io.github.jonestimd.swing.window.FrameAction;
import io.github.jonestimd.swing.window.WindowEventPublisher;

import static io.github.jonestimd.finance.swing.account.AccountTableModel.*;
import static org.apache.commons.lang.StringUtils.*;

public class AccountsPanel extends TransactionSummaryTablePanel<Account, AccountSummary> implements AccountSelector {
    private final AccountOperations accountOperations;
    private final AssetOperations assetOperations;
    private final CompanyCellEditor companyCellEditor;
    private final DomainEventPublisher eventPublisher;
    // need strong reference to avoid garbage collection
    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, Company> companyDomainEventListener = this::onDomainEvent;

    private Action openAction;
    private Action companyAction;

    public AccountsPanel(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher, FinanceTableFactory tableFactory,
                         WindowEventPublisher<WindowType> windowEventPublisher) {
        super(domainEventPublisher, tableFactory.createValidatedTable(new AccountTableModel(domainEventPublisher), COMPANY_INDEX, NAME_INDEX), "account");
        this.accountOperations = serviceLocator.getAccountOperations();
        this.assetOperations = serviceLocator.getAssetOperations();
        this.eventPublisher = domainEventPublisher;
        this.companyCellEditor = new CompanyCellEditor(serviceLocator);
        domainEventPublisher.register(Company.class, companyDomainEventListener);
        getTable().getColumn(AccountColumnAdapter.COMPANY_ADAPTER).setCellEditor(companyCellEditor);
        openAction = new FrameAction<>(bundle, "account.action.open", windowEventPublisher, new TransactionsWindowEvent(this));
        openAction.setEnabled(false);
        companyAction = new CompanyDialogAction(tableFactory);
        TableFactory.addDoubleClickHandler(getTable(), this::tableDoubleClicked);
    }

    @Override
    protected void addActions(JToolBar toolbar) {
        toolbar.add(ComponentFactory.newToolbarButton(openAction));
        super.addActions(toolbar);
        toolbar.add(ComponentFactory.newMenuBarSeparator());
        toolbar.add(ComponentFactory.newToolbarButton(companyAction));
    }

    @Override
    protected void addActions(JMenu menu) {
        menu.add(new JMenuItem(openAction));
        super.addActions(menu);
        menu.add(new JSeparator());
        menu.add(new JMenuItem(companyAction));
    }

    private void onDomainEvent(DomainEvent<Long, Company> event) {
        if (event.isAdd()) {
            companyCellEditor.addListItems(event.getDomainObjects());
        }
    }

    @Override
    protected boolean isMatch(AccountSummary tableRow, String criteria) {
        Account account = tableRow.getTransactionAttribute();
        return criteria.isEmpty() || containsIgnoreCase(account.qualifiedName(" "), criteria) || containsIgnoreCase(account.getDescription(), criteria);
    }

    @Override
    public AccountTableModel getTableModel() {
        return (AccountTableModel) super.getTableModel();
    }

    public Account getSelectedAccount() {
        return getSelectedBean().getTransactionAttribute();
    }

    @Override
    public void addAccountListener(PropertyChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAccountListener(PropertyChangeListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<AccountSummary> getTableData() {
        return accountOperations.getAccountSummaries();
    }

    @Override
    protected AccountSummary newBean() {
        return new AccountSummary(assetOperations.getCurrency(Currency.getInstance(Locale.getDefault()).getCurrencyCode()));
    }

    @Override
    protected List<? extends DomainEvent<?, ?>> saveChanges(List<Account> changedAccounts, List<Account> deletedAccounts) {
        List<DomainEvent<?, ?>> events = new ArrayList<>();
        if (!changedAccounts.isEmpty()) {
            List<Company> newCompanies = changedAccounts.stream().map(Account::getCompany).filter(Objects::nonNull).filter(Company::isNew).collect(Collectors.toList());
            accountOperations.saveAll(changedAccounts);
            events.add(new AccountEvent(this, EventType.CHANGED, changedAccounts));
            if (!newCompanies.isEmpty()) {
                events.add(new CompanyEvent(this, EventType.ADDED, newCompanies));
            }
        }
        if (!deletedAccounts.isEmpty()) {
            accountOperations.deleteAll(deletedAccounts);
            events.add(new AccountEvent(this, EventType.DELETED, deletedAccounts));
        }
        return events;
    }

    @Override
    protected void tableSelectionChanged() {
        super.tableSelectionChanged();
        openAction.setEnabled(isSingleRowSelected() && getSelectedAccount().getId() != null);
    }

    private void tableDoubleClicked(MouseEvent event) {
        if (openAction.isEnabled()) {
            openAction.actionPerformed(new ActionEvent(this, -1, null));
        }
    }

    public class CompanyDialogAction extends DialogAction {
        private final FinanceTableFactory tableFactory;
        private CompanyDialog dialog;
        private List<Company> updatedCompanies;
        private List<AccountSummary> updatedAccounts;

        public CompanyDialogAction(FinanceTableFactory tableFactory) {
            super(BundleType.LABELS.get(), "action.editCompanies");
            this.tableFactory = tableFactory;
        }

        protected void loadDialogData() {
            dialog = new CompanyDialog((JFrame) getTopLevelAncestor(), tableFactory, accountOperations.getAllCompanies());
        }

        protected boolean displayDialog(JComponent owner) {
            updatedAccounts = null;
            dialog.setVisible(true);
            return ! dialog.isCancelled();
        }

        protected void saveDialogData() {
            if (! dialog.getDeletes().isEmpty()) {
                accountOperations.deleteCompanies(dialog.getDeletes());
            }
            if (! dialog.getUpdates().isEmpty()) {
                accountOperations.saveCompanies(dialog.getUpdates());
            }
            updatedCompanies = accountOperations.getAllCompanies();
            updatedAccounts = accountOperations.getAccountSummaries();
        }

        protected void setSaveResultOnUI() {
            if (updatedAccounts != null) {
                companyCellEditor.setListItems(updatedCompanies);
                getTableModel().setBeans(updatedAccounts);
                eventPublisher.publishEvent(new AccountEvent(this, EventType.CHANGED, Iterables.transform(updatedAccounts, AccountSummary::getTransactionAttribute)));
            }
        }
    }
}