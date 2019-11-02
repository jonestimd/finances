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
package io.github.jonestimd.finance.swing.transaction;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.event.AccountSelector;
import io.github.jonestimd.finance.swing.fileimport.FileImportDialog;
import io.github.jonestimd.finance.swing.transaction.action.ImportFileAction;
import io.github.jonestimd.swing.BackgroundTask;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.action.DialogAction;
import io.github.jonestimd.swing.window.StatusFrame;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ImportFileMenuFactory {
    public static final String MENU_KEY = "menu.file.import.mnemonicAndName";

    private final ServiceLocator serviceLocator;
    private final FinanceTableFactory tableFactory;
    private final List<AccountListener> accountListeners = new ArrayList<>();
    private List<ImportFileAction> importFileActions;
    private Action editImportsAction = new DialogAction(LABELS.get(), "menu.file.editImports") {
        private List<Account> accounts;
        private List<Payee> payees;
        private List<ImportFile> importFiles;

        @Override
        protected void loadDialogData() {
            accounts = serviceLocator.getAccountOperations().getAllAccounts();
            payees = serviceLocator.getPayeeOperations().getAllPayees();
            importFiles = serviceLocator.getImportFileDao().getAll();
        }

        @Override
        protected boolean displayDialog(JComponent owner) {
            StatusFrame window = ComponentTreeUtils.findAncestor(owner, StatusFrame.class);
            FileImportDialog dialog = new FileImportDialog(window, accounts, payees, importFiles, tableFactory);
            return dialog.showDialog();
        }

        @Override
        protected void saveDialogData() {
            // TODO
        }

        @Override
        protected void setSaveResultOnUI() {
            // TODO
        }
    };

    public ImportFileMenuFactory(ServiceLocator serviceLocator, FinanceTableFactory tableFactory) {
        this.serviceLocator = serviceLocator;
        this.tableFactory = tableFactory;
    }

    public Action getEditImportsAction() {
        return editImportsAction;
    }

    /**
     * This method is not thread safe and should only be called from the Swing event thread.
     */
    public JMenu createImportMenu(AccountSelector accountSelector) {
        AccountListener accountListener = new AccountListener(accountSelector, ComponentFactory.newMenu(LABELS.get(), MENU_KEY));
        if (importFileActions == null) {
            accountListeners.add(accountListener);
            if (accountListeners.size() == 1) {
                BackgroundTask.task(serviceLocator.getImportFileDao()::getAll, this::createMenus).run();
            }
        }
        else {
            accountListener.updateMenu();
        }
        accountSelector.addAccountListener(accountListener);
        return accountListener.menu;
    }

    private void createMenus(List<ImportFile> importFiles) {
        importFileActions = Streams.map(importFiles, this::newImportFileAction);
        accountListeners.forEach(AccountListener::updateMenu);
        accountListeners.clear();
    }

    private ImportFileAction newImportFileAction(ImportFile importFile) {
        return new ImportFileAction(importFile, serviceLocator);
    }

    private class AccountListener implements PropertyChangeListener {
        private final AccountSelector accountSelector;
        private final JMenu menu;

        public AccountListener(AccountSelector accountSelector, JMenu menu) {
            this.accountSelector = accountSelector;
            this.menu = menu;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateMenu();
        }

        private void updateMenu() {
            menu.removeAll();
            Account account = accountSelector.getSelectedAccount();
            if (importFileActions != null && account != null) {
                importFileActions.stream().filter(action -> action.getImportFile().getAccount().equals(account))
                        .map(JMenuItem::new).forEach(menu::add);
            }
        }
    }
}
