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
package io.github.jonestimd.finance.service;

import io.github.jonestimd.finance.dao.DaoRepository;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.AccountOperationsImpl;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.AssetOperationsImpl;
import io.github.jonestimd.finance.operations.FileImportOperations;
import io.github.jonestimd.finance.operations.FileImportOperationsImpl;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.finance.operations.PayeeOperationsImpl;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperationsImpl;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;
import io.github.jonestimd.finance.operations.TransactionGroupOperationsImpl;
import io.github.jonestimd.finance.operations.TransactionOperations;
import io.github.jonestimd.finance.operations.TransactionOperationsImpl;

public class ServiceContext implements ServiceLocator {
    private final DaoRepository daoContext;
    private final AccountOperations accountOperations;
    private final PayeeOperations payeeOperations;
    private final TransactionCategoryOperations TransactionCategoryOperations;
    private final TransactionGroupOperations transactionGroupOperations;
    private final TransactionOperations transactionOperations;
    private final TransactionService transactionService;
    private final AssetOperations assetOperations;
    private final FileImportOperations fileImportOperations;

    public ServiceContext(DaoRepository daoContext) {
        this.daoContext = daoContext;
        accountOperations = transactional(new AccountOperationsImpl(daoContext.getCompanyDao(), daoContext.getAccountDao()), AccountOperations.class);
        payeeOperations = transactional(new PayeeOperationsImpl(daoContext.getPayeeDao(), daoContext.getTransactionDao()), PayeeOperations.class);
        TransactionCategoryOperations = transactional(new TransactionCategoryOperationsImpl(daoContext.getTransactionCategoryDao(), daoContext.getTransactionDetailDao()), TransactionCategoryOperations.class);
        transactionGroupOperations = transactional(new TransactionGroupOperationsImpl(daoContext.getTransactionGroupDao()), TransactionGroupOperations.class);
        transactionOperations = transactional(new TransactionOperationsImpl(daoContext), TransactionOperations.class);
        transactionService = new TransactionServiceImpl(transactionOperations, daoContext.getDomainEventRecorder());
        assetOperations = transactional(new AssetOperationsImpl(daoContext), AssetOperations.class);
        fileImportOperations = transactional(new FileImportOperationsImpl(daoContext.getImportFileDao(), this), FileImportOperations.class);
    }

    @Override
    public <I,T extends I> I transactional(T target, Class<I> iface) {
        return iface.cast(daoContext.transactional(target, iface));
    }

    @Override
    public AccountOperations getAccountOperations() {
        return accountOperations;
    }

    @Override
    public PayeeOperations getPayeeOperations() {
        return payeeOperations;
    }

    @Override
    public TransactionCategoryOperations getTransactionCategoryOperations() {
        return TransactionCategoryOperations;
    }

    @Override
    public TransactionGroupOperations getTransactionGroupOperations() {
        return transactionGroupOperations;
    }

    @Override
    public TransactionService getTransactionService() {
        return transactionService;
    }

    @Override
    public AssetOperations getAssetOperations() {
        return assetOperations;
    }

    @Override
    public FileImportOperations getFileImportOperations() {
        return fileImportOperations;
    }
}