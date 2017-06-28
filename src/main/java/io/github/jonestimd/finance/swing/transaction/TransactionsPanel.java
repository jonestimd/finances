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
package io.github.jonestimd.finance.swing.transaction;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.SystemProperty;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.asset.SecurityTransactionTableModel;
import io.github.jonestimd.finance.swing.event.AccountSelector;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.ReloadEventHandler;
import io.github.jonestimd.finance.swing.event.TransactionsWindowEvent;
import io.github.jonestimd.finance.swing.transaction.action.AutofillTask;
import io.github.jonestimd.finance.swing.transaction.action.CommitAction;
import io.github.jonestimd.finance.swing.transaction.action.EditLotsAction;
import io.github.jonestimd.finance.swing.transaction.action.MoveAction;
import io.github.jonestimd.finance.swing.transaction.action.RefreshAction;
import io.github.jonestimd.finance.swing.transaction.action.SaveAllAction;
import io.github.jonestimd.finance.swing.transaction.action.UpdateSharesAction;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.HighlightText;
import io.github.jonestimd.swing.action.FocusAction;
import io.github.jonestimd.swing.component.AutosizeTextField;
import io.github.jonestimd.swing.component.ComponentBinder;
import io.github.jonestimd.swing.component.FilterField;
import io.github.jonestimd.swing.component.MenuActionPanel;
import io.github.jonestimd.swing.dialog.Dialogs;
import io.github.jonestimd.swing.window.ConfirmCloseAdapter;
import io.github.jonestimd.swing.window.WindowEventPublisher;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.event.SingletonWindowEvent.*;

public class TransactionsPanel extends MenuActionPanel implements AccountSelector, HighlightText {
    private static final AccountFormat ACCOUNT_FORMAT = new AccountFormat();
    private static final String LOADING_MESSAGE_KEY = "action.refreshTransactions.status.initialize";
    private final Format currencyFormat = FormatFactory.currencyFormat();
    private final TransactionService transactionService;
    private final AssetOperations assetOperations;
    private final TransactionCategoryOperations transactionCategoryOperations;
    private final TransactionTableModelCache transactionModelCache;
    private Account selectedAccount;
    private final TransactionTable transactionTable;
    private final FilterField<Transaction> filterField = new ComponentFactory().newFilterField(TransactionFilter::new, 4, 2);
    private final JTextField clearedBalance = new AutosizeTextField(false);
    private final FinanceTableFactory tableFactory;
    private final WindowEventPublisher<WindowType> windowEventPublisher;
    private final AccountsMenuFactory accountsMenuFactory;
    private final ImportFileMenuFactory importFileMenuFactory;
    private final SaveAllAction saveAllAction;
    private final RefreshAction refreshAction;
    private final DomainEventPublisher domainEventPublisher;
    private final DomainEventListener<Long, Account> accountEventListener = new DomainEventListener<Long, Account>() {
        public void onDomainEvent(DomainEvent<Long, Account> event) {
            if (selectedAccount != null) {
                Account domainObject = event.getDomainObject(selectedAccount.getId());
                if (domainObject != null) {
                    selectedAccount = domainObject;
                    setTitle(selectedAccount);
                }
            }
        }
    };
    @SuppressWarnings("FieldCanBeLocal")
    private final ReloadEventHandler<Long, Transaction> reloadHandler;
    private final TableModelListener tableModelListener = new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            TransactionTableModel model = (TransactionTableModel) e.getSource();
            saveAllAction.setEnabled(model.isUnsavedChanges() && model.isNoErrors());
        }
    };
    private final PropertyChangeListener payeeChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getOldValue() == null && evt.getNewValue() != null) {
                TransactionTableModel model = (TransactionTableModel) evt.getSource();
                AutofillTask.execute(transactionService, model.getBean(model.getBeanCount() - 1), transactionTable);
            }
        }
    };

    public TransactionsPanel(ServiceLocator serviceLocator, TransactionTableModelCache transactionModelCache,
                             FinanceTableFactory tableFactory, WindowEventPublisher<WindowType> windowEventPublisher, AccountsMenuFactory accountsMenuFactory,
                             ImportFileMenuFactory importFileMenuFactory, DomainEventPublisher eventPublisher) {
        this.transactionService = serviceLocator.getTransactionService();
        this.assetOperations = serviceLocator.getAssetOperations();
        this.transactionCategoryOperations = serviceLocator.getTransactionCategoryOperations();
        this.transactionModelCache = transactionModelCache;
        this.tableFactory = tableFactory;
        this.windowEventPublisher = windowEventPublisher;
        this.accountsMenuFactory = accountsMenuFactory;
        this.importFileMenuFactory = importFileMenuFactory;
        this.domainEventPublisher = eventPublisher;
        eventPublisher.register(Account.class, accountEventListener);
        // TODO use CurrencyTransactionTableModel
        this.transactionTable = tableFactory.newTransactionTable(new SecurityTransactionTableModel(getSelectedAccount()));
        filterField.addPropertyChangeListener(FilterField.PREDICATE_PROPERTY, event -> transactionTable.getRowSorter().setRowFilter(filterField.getFilter()));
        FocusAction.install(filterField, transactionTable, LABELS.get(), "table.filterField.accelerator");
        this.saveAllAction = new SaveAllAction(transactionTable, transactionService, domainEventPublisher);
        this.refreshAction = new RefreshAction(transactionTable, transactionService);
        this.reloadHandler = new ReloadEventHandler<>(this, LOADING_MESSAGE_KEY, this::getTransactions, transactionTable::getModel);
        eventPublisher.register(Transaction.class, this.reloadHandler);
        buildPanel();
    }

    private void buildPanel() {
        setLayout(new BorderLayout());
        installTableAction(TransactionTableAction.NEXT_TRANSACTION, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                new CommitAction(transactionTable, transactionService, domainEventPublisher));
        add(new JScrollPane(transactionTable), BorderLayout.CENTER);
        add(ComponentFactory.newTableSummaryPanel(LABELS.getString("panel.transactions.clearedBalance"), clearedBalance), BorderLayout.SOUTH);
    }

    private void installTableAction(Object key, KeyStroke keyStroke, Action action) {
        transactionTable.getActionMap().put(key, action);
        transactionTable.getInputMap().put(keyStroke, key);
    }

    private List<Transaction> getTransactions() {
        try {
            return transactionService.getTransactions(transactionTable.getModel().getAccount().getId());
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Collection<String> getHighlightText() {
        return filterField.getTerms();
    }

    public Account getSelectedAccount() {
        return selectedAccount;
    }

    @Override
    public void addAccountListener(PropertyChangeListener listener) {
        addPropertyChangeListener(ACCOUNT_PROPERTY, listener);
    }

    @Override
    public void removeAccountListener(PropertyChangeListener listener) {
        removePropertyChangeListener(ACCOUNT_PROPERTY, listener);
    }

    public void setSelectedAccount(Account account) {
        if (this.selectedAccount == null || ! this.selectedAccount.getId().equals(account.getId())) {
            this.selectedAccount = account;
            setTitle(account);
            transactionTable.getModel().removeTableModelListener(tableModelListener);
            transactionTable.getModel().removePropertyChangeListener(TransactionTableModel.NEW_TRANSACTION_PAYEE_PROPERTY, payeeChangeListener);
            TransactionTableModel newModel = transactionModelCache.getModel(account);
            transactionTable.setModel(newModel);
            transactionTable.getRowSorter().setSortKeys(Collections.singletonList(new SortKey(0, SortOrder.ASCENDING)));
            newModel.addTableModelListener(tableModelListener);
            newModel.addPropertyChangeListener(TransactionTableModel.NEW_TRANSACTION_PAYEE_PROPERTY, payeeChangeListener);
            ComponentBinder.bind(newModel, TransactionTableModel.CLEARED_BALANCE_PROPERTY, newModel.getClearedBalance(), clearedBalance, currencyFormat);
            if (transactionTable.getModel().getBeans().isEmpty()) {
                refreshAction.actionPerformed(new ActionEvent(this, RefreshAction.INITIAL_LOAD_ACTION_ID, null));
            }
            else {
                transactionTable.selectLastTransaction();
            }
            firePropertyChange(ACCOUNT_PROPERTY, null, selectedAccount);
        }
    }

    private void setTitle(Account account) {
        JFrame window = (JFrame) getTopLevelAncestor();
        if (window != null) {
            String title = account.getName();
            if (account.getCompany() != null) {
                title = account.getCompany().getName() + ":" + title;
            }
            window.setTitle(title);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        JFrame frame = (JFrame) getTopLevelAncestor();
        ConfirmCloseAdapter.install(frame, this::confirmClose);
        if (selectedAccount != null) {
            frame.setTitle(ACCOUNT_FORMAT.format(selectedAccount));
        }
        SwingUtilities.invokeLater(transactionTable::requestFocusInWindow);
    }

    @Override
    protected void initializeMenu(JMenuBar menuBar) {
        JToolBar toolbar = ComponentFactory.newMenuToolBar();
        if (toolbar.getLayout().getClass().getSimpleName().equals("SynthToolBarLayoutManager")) {
            toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.LINE_AXIS));
        }
        menuBar.add(createTransactionsMenu(toolbar), 0);
        menuBar.add(accountsMenuFactory.createAccountsMenu(), 0);
        menuBar.add(createFileMenu(), 0);
        menuBar.add(ComponentFactory.newMenuBarSeparator());
        menuBar.add(toolbar);
        toolbar.add(Box.createHorizontalStrut(5));
        toolbar.add(filterField);
    }

    private JMenu createFileMenu() {
        final JMenu menu = ComponentFactory.newMenu(LABELS.get(), "menu.file.mnemonicAndName");
        menu.add(importFileMenuFactory.createImportMenu(this));
        return menu;
    }

    private JMenu createTransactionsMenu(JToolBar toolbar) {
        JMenu menu = ComponentFactory.newMenu(LABELS.get(), "menu.transactions.mnemonicAndName");
        addTransactionAction(menu, toolbar, TransactionsWindowEvent.frameAction(this, "action.newTransactionsWindow", windowEventPublisher));
        menu.addSeparator();
        toolbar.add(ComponentFactory.newMenuBarSeparator());
        addTransactionAction(menu, toolbar, new MoveAction(transactionTable, transactionService, domainEventPublisher, accountsMenuFactory::getAccounts));
        addTransactionAction(menu, toolbar, transactionTable.getActionMap().get(TransactionTableAction.INSERT_DETAIL));
        addTransactionAction(menu, toolbar, transactionTable.getActionMap().get(TransactionTableAction.DELETE_DETAIL));
        addTransactionAction(menu, toolbar, saveAllAction);
        addTransactionAction(menu, toolbar, refreshAction);
        toolbar.add(ComponentFactory.newMenuBarSeparator());
        addTransactionAction(menu, toolbar, new EditLotsAction(transactionTable, transactionService, tableFactory));
        addTransactionAction(menu, toolbar, new UpdateSharesAction(transactionTable, assetOperations, transactionCategoryOperations));
        menu.addSeparator();
        toolbar.add(ComponentFactory.newMenuBarSeparator());
        toolbar.add(ComponentFactory.newToolbarButton(accountsFrameAction(menu, windowEventPublisher)));
        addTransactionAction(menu, toolbar, frameAction(menu, WindowType.CATEGORIES, "action.viewCategories", windowEventPublisher));
        addTransactionAction(menu, toolbar, frameAction(menu, WindowType.TRANSACTION_GROUPS, "action.viewTransactionGroups", windowEventPublisher));
        addTransactionAction(menu, toolbar, frameAction(menu, WindowType.PAYEES, "action.viewPayees", windowEventPublisher));
        addTransactionAction(menu, toolbar, frameAction(menu, WindowType.SECURITIES, "action.viewSecurities", windowEventPublisher));
        addTransactionAction(menu, toolbar, frameAction(menu, WindowType.ACCOUNT_SECURITIES, "action.viewAccountSecurities", windowEventPublisher));
        return menu;
    }

    private void addTransactionAction(JMenu menu, JToolBar toolbar, Action action) {
        menu.add(action);
        toolbar.add(ComponentFactory.newToolbarButton(action));
    }

    public void removeNotify() {
        super.removeNotify();
        if (selectedAccount != null) {
            System.setProperty(SystemProperty.LAST_ACCOUNT.key(), selectedAccount.getId().toString());
        }
        domainEventPublisher.unregister(Account.class, accountEventListener);
    }

    private boolean confirmClose(WindowEvent event) {
        return ! transactionTable.getModel().isUnsavedChanges() || Dialogs.confirmDiscardChanges(this);
    }
}