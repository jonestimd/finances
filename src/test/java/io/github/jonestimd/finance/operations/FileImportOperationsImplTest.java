package io.github.jonestimd.finance.operations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.ImportFileDao;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.file.ImportContext;
import io.github.jonestimd.finance.service.MockServiceContext;
import io.github.jonestimd.finance.service.ServiceLocator;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class FileImportOperationsImplTest {
    private ImportFileDao importFileDao = mock(ImportFileDao.class);
    private ServiceLocator serviceLocator = new MockServiceContext();
    private PayeeOperations payeeOperations = serviceLocator.getPayeeOperations();
    private AssetOperations assetOperations = serviceLocator.getAssetOperations();
    private TransactionCategoryOperations transactionCategoryOperations = serviceLocator.getTransactionCategoryOperations();
    private FileImportOperationsImpl fileImportOperations;

    @Before
    public void injectMocks() throws Exception {
        fileImportOperations = new FileImportOperationsImpl(importFileDao, serviceLocator);
    }

    @Test
    public void importTransactions() throws Exception {
        final String importName = "test import";
        final InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        final ImportFile importFile = mock(ImportFile.class);
        final ImportContext importContext = mock(ImportContext.class);
        final ArrayList<Payee> payees = new ArrayList<>();
        final ArrayList<Security> securities = new ArrayList<>();
        final ArrayList<TransactionCategory> categories = new ArrayList<>();
        when(payeeOperations.getAllPayees()).thenReturn(payees);
        when(assetOperations.getAllSecurities()).thenReturn(securities);
        when(transactionCategoryOperations.getAllTransactionCategories()).thenReturn(categories);
        when(importFileDao.findOneByName(importName)).thenReturn(importFile);
        when(importFile.newContext(same(payees), same(securities), same(categories))).thenReturn(importContext);
        when(importContext.parseTransactions(any(InputStream.class))).thenReturn(Lists.newArrayList());

        fileImportOperations.importTransactions(importName, inputStream);

        verify(serviceLocator.getTransactionService()).saveTransactions(anyCollectionOf(Transaction.class));
    }
}