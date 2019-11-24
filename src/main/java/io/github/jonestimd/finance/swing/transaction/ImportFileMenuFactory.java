// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.operations.FileImportOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.event.AccountSelector;
import io.github.jonestimd.finance.swing.fileimport.FileImportsDialog;
import io.github.jonestimd.finance.swing.fileimport.FileImportsModel;
import io.github.jonestimd.finance.swing.fileimport.ImportFileModel;
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
    private final Map<AccountSelector, AccountListener> accountListeners = new WeakHashMap<>();
    private List<ImportFileAction> importFileActions;

    private Action editImportsAction = new DialogAction(LABELS.get(), "menu.file.editImports") {
        private List<Account> accounts;
        private List<TransactionCategory> categories;
        private List<Payee> payees;
        private FileImportsModel importsModel;

        @Override
        protected void loadDialogData() {
            accounts = serviceLocator.getAccountOperations().getAllAccounts();
            categories = serviceLocator.getTransactionCategoryOperations().getAllTransactionCategories();
            payees = serviceLocator.getPayeeOperations().getAllPayees();
            importsModel = new FileImportsModel(serviceLocator.getFileImportOperations().getAll());
        }

        @Override
        protected boolean displayDialog(JComponent owner) {
            StatusFrame window = ComponentTreeUtils.findAncestor(owner, StatusFrame.class);
            FileImportsDialog dialog = new FileImportsDialog(window, accounts, categories, payees, importsModel,
                    tableFactory, this::saveDialogData);
            return dialog.showDialog();
        }

        @Override
        protected void saveDialogData() {
            FileImportOperations fileImportOperations = serviceLocator.getFileImportOperations();
            List<ImportFile> deletes = importsModel.getDeletes();
            if (!deletes.isEmpty()) fileImportOperations.deleteAll(deletes);
            Set<ImportFile> changedImports = importsModel.getChanges();
            fileImportOperations.saveAll(changedImports);
        }

        @Override
        protected void setSaveResultOnUI() {
            createMenus(Streams.map(importsModel, ImportFileModel::getBean));
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
     * This method is not thread safe and should only be called from the Swing event thread.  Creates a menu
     * that will be updated when the selected account changes.
     */
    public JMenu createImportMenu(AccountSelector accountSelector) {
        AccountListener accountListener = new AccountListener(ComponentFactory.newMenu(LABELS.get(), MENU_KEY));
        accountListeners.put(accountSelector, accountListener);
        if (importFileActions == null) {
            if (accountListeners.size() == 1) {
                BackgroundTask.task(serviceLocator.getFileImportOperations()::getAll, this::createMenus).run();
            }
        }
        else accountListener.updateMenu(accountSelector);
        accountSelector.addAccountListener(accountListener);
        return accountListener.menu;
    }

    private void createMenus(List<ImportFile> importFiles) {
        importFileActions = Streams.map(importFiles, this::newImportFileAction);
        accountListeners.forEach((key, value) -> value.updateMenu(key));
    }

    private ImportFileAction newImportFileAction(ImportFile importFile) {
        return new ImportFileAction(importFile, serviceLocator);
    }

    private class AccountListener implements PropertyChangeListener {
        private final JMenu menu;

        public AccountListener(JMenu menu) {
            this.menu = menu;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            accountListeners.entrySet().stream().filter(entry -> entry.getValue() == this).findFirst()
                    .ifPresent(entry -> updateMenu(entry.getKey()));
        }

        private void updateMenu(AccountSelector accountSelector) {
            menu.removeAll();
            Account account = accountSelector.getSelectedAccount();
            if (importFileActions != null && account != null) {
                importFileActions.stream().filter(action -> action.getImportFile().getAccount().equals(account))
                        .map(JMenuItem::new).forEach(menu::add);
            }
        }
    }
}
