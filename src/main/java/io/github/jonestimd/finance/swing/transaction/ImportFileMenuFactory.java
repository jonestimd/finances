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

import javax.swing.JMenu;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.AccountSelector;
import io.github.jonestimd.finance.swing.transaction.action.ImportFileAction;
import io.github.jonestimd.swing.BackgroundTask;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.util.Streams;

public class ImportFileMenuFactory {
    public static final String MENU_KEY = "menu.file.import.mnemonicAndName";
    private final ServiceLocator serviceLocator;
    private final List<AccountListener> accountListeners = new ArrayList<>();
    private List<ImportFileAction> importFileActions;

    public ImportFileMenuFactory(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    /**
     * This method is not thread safe and should only be called from the Swing event thread.
     */
    public JMenu createImportMenu(AccountSelector accountSelector) {
        AccountListener accountListener = new AccountListener(accountSelector, ComponentFactory.newMenu(BundleType.LABELS.get(), MENU_KEY));
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
                importFileActions.stream().filter(action -> action.getImportFile().getAccount().equals(account)).forEach(menu::add);
            }
        }
    }
}