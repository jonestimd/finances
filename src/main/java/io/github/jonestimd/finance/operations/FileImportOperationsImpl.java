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
package io.github.jonestimd.finance.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import io.github.jonestimd.finance.dao.DaoRepository;
import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.dao.ImportFileDao;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.service.ServiceContext;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.service.TransactionService;

import static io.github.jonestimd.finance.config.ApplicationConfig.*;
import static io.github.jonestimd.finance.swing.FinanceApplication.*;

public class FileImportOperationsImpl implements FileImportOperations { // TODO move to client (import action?)
    private final ImportFileDao importFileDao;
    private final TransactionService transactionService;
    private final PayeeOperations payeeOperations;
    private final AssetOperations assetOperations;
    private final TransactionCategoryOperations categoryOperations;

    public FileImportOperationsImpl(ImportFileDao importFileDao, ServiceLocator serviceLocator) {
        this.importFileDao = importFileDao;
        this.transactionService = serviceLocator.getTransactionService();
        this.payeeOperations = serviceLocator.getPayeeOperations();
        this.assetOperations = serviceLocator.getAssetOperations();
        this.categoryOperations = serviceLocator.getTransactionCategoryOperations();
    }

    @Override
    public void importTransactions(String importName, InputStream source) {
        ImportFile importFile = importFileDao.findOneByName(importName);
        try {
            transactionService.saveTransactions(toTransactions(source, importFile));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Transaction> toTransactions(InputStream source, ImportFile importFile) throws Exception {
        return importFile.newContext(payeeOperations.getAllPayees(), assetOperations.getAllSecurities(), categoryOperations.getAllTransactionCategories())
                .parseTransactions(source);
    }

    public static void main(String args[]) {
        JFileChooser fileChooser = new JFileChooser("/home/tim/temp");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.getActionMap().get("viewTypeDetails").actionPerformed(null);
        fileChooser.showOpenDialog(JOptionPane.getRootFrame());
        try {
            DaoRepository daoContext = new HibernateDaoContext(CONNECTION_CONFIG.loadDriver(), CONFIG);
            ServiceContext serviceContext = new ServiceContext(daoContext);
            FileImportOperationsImpl fileImportOperations = new FileImportOperationsImpl(daoContext.getImportFileDao(), serviceContext);
            for (File file : fileChooser.getSelectedFiles()) {
                fileImportOperations.importTransactions("Discover Statement", new FileInputStream(file));
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
