package io.github.jonestimd.finance.service;

import io.github.jonestimd.finance.dao.ImportFileDao;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;

import static org.mockito.Mockito.*;

public class MockServiceContext implements ServiceLocator {
    private AccountOperations accountOperations = mock(AccountOperations.class);
    private PayeeOperations payeeOperations = mock(PayeeOperations.class);
    private AssetOperations assetOperations = mock(AssetOperations.class);
    private TransactionGroupOperations transactionGroupOperations = mock(TransactionGroupOperations.class);
    private TransactionCategoryOperations TransactionCategoryOperations = mock(TransactionCategoryOperations.class);
    private TransactionService transactionService = mock(TransactionService.class);
    private ImportFileDao importFileDao = mock(ImportFileDao.class);

    @Override
    public AccountOperations getAccountOperations() {
        return accountOperations;
    }

    @Override
    public PayeeOperations getPayeeOperations() {
        return payeeOperations;
    }

    @Override
    public AssetOperations getAssetOperations() {
        return assetOperations;
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
    public TransactionCategoryOperations getTransactionCategoryOperations() {
        return TransactionCategoryOperations;
    }

    @Override
    public ImportFileDao getImportFileDao() {
        return importFileDao;
    }

    @Override
    public <I,T extends I> I transactional(T target, Class<I> iface) {
        return target;
    }
}