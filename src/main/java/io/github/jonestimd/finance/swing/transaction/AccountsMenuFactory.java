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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton.ToggleButtonModel;

import com.google.common.collect.Ordering;
import io.github.jonestimd.collection.ReferenceIterator;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.SingletonWindowEvent;
import io.github.jonestimd.swing.BackgroundTask;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.action.ActionAdapter;
import io.github.jonestimd.swing.window.WindowEventPublisher;
import io.github.jonestimd.util.Streams;

public class AccountsMenuFactory {
    private static final String HIDE_CLOSED_ACCOUNTS_PROPERTY = "AccountsMenuFactory.hideClosedAccounts";
    public static final String HIDE_CLOSED_ACCOUNTS_MENU_KEY = "menu.accounts.closedFilter.mnemonicAndName";
    public static final String ACCOUNTS_MENU_KEY = "menu.transactions.accounts.mnemonicAndName";
    public static final String ACCOUNTS_WINDOW_MENU_KEY = "action.viewAccounts.mnemonicAndName";
    private static final int FIRST_ACCOUNT_INDEX = 3;
    private static final Ordering<AccountAction> ACTION_ORDERING = Ordering.from(new AccountsMenuSort()).onResultOf(AccountAction::getAccount);
    private static final String SEPARATOR = BundleType.LABELS.getString("io.github.jonestimd.finance.companyAccount.separator");
    private final AccountOperations accountOperations;
    private final WindowEventPublisher<WindowType> windowEventPublisher;
    private final ToggleButtonModel filterModel = new ToggleButtonModel();
    private final Action filterAction;
    private Map<Long, AccountAction> accountActionMap;
    private final List<MenuAction> menuActions = new ArrayList<>();
    private final List<WeakReference<JMenu>> menus = new ArrayList<>();

    @SuppressWarnings("FieldCanBeLocal")
    private final DomainEventListener<Long, Account> accountEventListener = event -> {
        if (accountActionMap != null) {
            if (event.isDelete()) event.getDomainObjects().forEach(account -> accountActionMap.remove(account.getId()));
            else updateAccountActions(event);
            updateMenus();
        }
    };

    public AccountsMenuFactory(AccountOperations accountOperations, WindowEventPublisher<WindowType> windowEventPublisher, DomainEventPublisher domainEventPublisher) {
        this.accountOperations = accountOperations;
        this.windowEventPublisher = windowEventPublisher;
        this.filterAction = ActionAdapter.forMnemonicAndName(new FilterActionHandler(), BundleType.LABELS.getString(HIDE_CLOSED_ACCOUNTS_MENU_KEY));
        filterModel.setSelected(Boolean.getBoolean(HIDE_CLOSED_ACCOUNTS_PROPERTY));
        domainEventPublisher.register(Account.class, accountEventListener);
    }

    public Stream<Account> getAccounts() {
        return accountActionMap.values().stream().map(AccountAction::getAccount);
    }

    private void updateAccountActions(DomainEvent<Long, Account> event) {
        for (Account account : event.getDomainObjects()) {
            if (accountActionMap.containsKey(account.getId())) {
                accountActionMap.get(account.getId()).setAccount(account);
            }
            else {
                accountActionMap.put(account.getId(), new AccountAction(account));
            }
        }
    }

    public JMenu createAccountsMenu() {
        JMenu accountsMenu = ComponentFactory.newMenu(BundleType.LABELS.get(), ACCOUNTS_MENU_KEY);
        accountsMenu.add(createFilterMenuItem());
        accountsMenu.add(SingletonWindowEvent.accountsFrameAction(accountsMenu, windowEventPublisher));
        accountsMenu.addSeparator();
        menus.add(new WeakReference<>(accountsMenu));
        if (accountActionMap == null) {
            BackgroundTask.task(accountOperations::getAllAccounts, this::updateMenus).run();
        }
        else {
            addAccounts(accountsMenu);
        }
        return accountsMenu;
    }

    private void updateMenus(List<Account> accounts) {
        accountActionMap = new HashMap<>();
        for (Account account : accounts) {
            accountActionMap.put(account.getId(), new AccountAction(account));
        }
        updateMenus();
    }

    private JCheckBoxMenuItem createFilterMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(filterAction);
        menuItem.setModel(filterModel);
        return menuItem;
    }

    private void resetMenu(JMenu menu) {
        while (menu.getMenuComponentCount() > FIRST_ACCOUNT_INDEX) {
            menu.remove(FIRST_ACCOUNT_INDEX);
        }
        addAccounts(menu);
    }

    private void addAccounts(JMenu menu) {
        for (MenuAction menuAction : menuActions) {
            menuAction.addToMenu(menu);
        }
    }

    private void updateMenus() {
        buildMenuActions();
        ReferenceIterator<JMenu> iterator = ReferenceIterator.iterator(menus);
        while (iterator.hasNext()) {
            resetMenu(iterator.next());
        }
    }

    private void buildMenuActions() {
        List<Character> menuMnemonics = new ArrayList<>();
        menuMnemonics.add(BundleType.LABELS.getString(HIDE_CLOSED_ACCOUNTS_MENU_KEY).charAt(0));
        menuMnemonics.add(BundleType.LABELS.getString(ACCOUNTS_WINDOW_MENU_KEY).charAt(0));
        menuActions.clear();
        CompanyAction companyAction = null;
        List<Character> companyAccountMnemonics = new ArrayList<>();
        for (AccountAction accountAction : getSortedAccountActions()) {
            Account account = accountAction.getAccount();
            if (account.getCompany() == null) {
                assignMnemonic(accountAction, menuMnemonics);
                menuActions.add(accountAction);
            }
            else {
                if (companyAction == null || ! companyAction.company.getId().equals(account.getCompany().getId())) {
                    companyAction = new CompanyAction(account.getCompany());
                    companyAccountMnemonics.clear();
                    assignMnemonic(companyAction, menuMnemonics);
                    menuActions.add(companyAction);
                }
                assignMnemonic(accountAction, companyAccountMnemonics);
                companyAction.accountActions.add(accountAction);
            }
        }
    }

    private List<AccountAction> getSortedAccountActions() {
        List<AccountAction> accountActions = new ArrayList<>(accountActionMap.values());
        accountActions.sort(ACTION_ORDERING);
        return accountActions;
    }

    private void assignMnemonic(MenuAction action, List<Character> usedMnemonics) {
        String name = ((String) action.getValue(Action.NAME)).toUpperCase();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isLetterOrDigit(ch) && ! usedMnemonics.contains(ch)) {
                usedMnemonics.add(ch);
                action.putValue(Action.MNEMONIC_KEY, Integer.valueOf(ch));
                break;
            }
        }
    }

    private class FilterActionHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updateMenus();
            System.setProperty(HIDE_CLOSED_ACCOUNTS_PROPERTY, Boolean.toString(filterModel.isSelected()));
        }
    }

    private abstract class MenuAction extends AbstractAction {
        protected MenuAction(String name) {
            super(name);
        }

        public abstract void addToMenu(JMenu menu);
    }

    private class CompanyAction extends MenuAction {
        private Company company;
        private List<AccountAction> accountActions = new ArrayList<>();
        private AccountAction singleAccount;

        public CompanyAction(Company company) {
            super(company.getName());
            this.company = company;
        }

        public void addToMenu(JMenu menu) {
            List<AccountAction> visibleAccounts = Streams.filter(accountActions, AccountAction::isVisible);
            if (visibleAccounts.size() == 1) {
                singleAccount = visibleAccounts.get(0);
                putValue(Action.NAME, singleAccount.getAccount().qualifiedName(SEPARATOR));
                menu.add(this);
            }
            else if (! visibleAccounts.isEmpty()) {
                singleAccount = null;
                putValue(Action.NAME, company.getName());
                JMenu companyMenu = new JMenu(this);
                for (AccountAction accountAction : visibleAccounts) {
                    accountAction.addToMenu(companyMenu);
                }
                menu.add(companyMenu);
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (singleAccount != null) {
                singleAccount.actionPerformed(e);
            }
        }
    }

    private class AccountAction extends MenuAction {
        private Account account;

        public AccountAction(Account account) {
            super(account.getName());
            this.account = account;
        }

        public Account getAccount() {
            return account;
        }

        public void setAccount(Account account) {
            this.account = account;
            putValue(NAME, account.getName());
        }

        public void addToMenu(JMenu menu) {
            if (isVisible()) {
                menu.add(new JMenuItem(this));
            }
        }

        private boolean isVisible() {
            return ! filterModel.isSelected() || ! account.isClosed();
        }

        public void actionPerformed(ActionEvent e) {
            JFrame frame = ComponentTreeUtils.findAncestor((Component) e.getSource(), JFrame.class);
            TransactionsPanel transactionsPanel = (TransactionsPanel) frame.getContentPane();
            transactionsPanel.setSelectedAccount(account);
        }
    }
}