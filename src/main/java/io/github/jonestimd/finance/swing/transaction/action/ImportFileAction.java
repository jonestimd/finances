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

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JRootPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.file.ImportContext;
import io.github.jonestimd.finance.file.Reconciler;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.fileimport.FileImportDialog;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.action.ActionAdapter;
import io.github.jonestimd.swing.action.DialogAction;
import io.github.jonestimd.swing.window.StatusFrame;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ImportFileAction extends AbstractAction {
    private final ImportFile importFile;
    private final ServiceLocator serviceLocator;
    private final FinanceTableFactory tableFactory;
    private final JFileChooser fileChooser = new JFileChooser();
    private final Action editAction = new EditImportAction();
    private final Action deleteAction = new ActionAdapter(this::onDeleteImport, LABELS.get(), "action.file.import.delete");

    public ImportFileAction(ImportFile importFile, ServiceLocator serviceLocator, FinanceTableFactory tableFactory) {
        super(importFile.getName());
        this.importFile = importFile;
        this.serviceLocator = serviceLocator;
        this.tableFactory = tableFactory;
        FileFilter fileFilter = new FileNameExtensionFilter("*." + importFile.getFileType().extension, importFile.getFileType().extension);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(fileFilter);
        fileChooser.setFileFilter(fileFilter);
    }

    public ImportFile getImportFile() {
        return importFile;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        int result = fileChooser.showDialog(ComponentTreeUtils.findAncestor((Component) event.getSource(), JRootPane.class), null);
        if (result == JFileChooser.APPROVE_OPTION) {
            Window window = ComponentTreeUtils.findAncestor((JComponent) event.getSource(), Window.class);
            File selectedFile = fileChooser.getSelectedFile();
            ImportContext importContext = importFile.newContext(
                    serviceLocator.getPayeeOperations().getAllPayees(),
                    serviceLocator.getAssetOperations().getAllSecurities(),
                    serviceLocator.getTransactionCategoryOperations().getAllTransactionCategories());
            try (FileInputStream inputStream = new FileInputStream(selectedFile)) { // TODO do in background thread
                List<Transaction> transactions = importContext.parseTransactions(inputStream);
                TransactionTableModel tableModel = ComponentTreeUtils.findComponent(window, TransactionTable.class).getModel();
                if (importFile.isReconcile()) {
                    new Reconciler(tableModel).reconcile(transactions);
                }
                else {
                    transactions.forEach(transaction -> tableModel.queueAdd(tableModel.getBeanCount()-1, transaction));
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Action getEditAction() {
        return editAction;
    }

    public Action getDeleteAction() {
        return deleteAction;
    }

    private void onDeleteImport(ActionEvent event) {
    }

    private class EditImportAction extends DialogAction {
        private List<Account> accounts;
        private List<Payee> payees;

        public EditImportAction() {
            super(LABELS.get(), "action.file.import.edit");
        }

        @Override
        protected void loadDialogData() {
            accounts = serviceLocator.getAccountOperations().getAllAccounts();
            payees = serviceLocator.getPayeeOperations().getAllPayees();
        }

        @Override
        protected boolean displayDialog(JComponent owner) {
            StatusFrame window = ComponentTreeUtils.findAncestor(owner, StatusFrame.class);
            return new FileImportDialog(window, accounts, payees, tableFactory).show(importFile);
        }

        @Override
        protected void saveDialogData() {
        }

        @Override
        protected void setSaveResultOnUI() {
        }
    }
}
