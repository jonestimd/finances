package io.github.jonestimd.finance.operations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.github.jonestimd.finance.dao.ImportFileDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.file.ImportContext;
import io.github.jonestimd.finance.service.MockServiceContext;
import io.github.jonestimd.finance.service.ServiceLocator;
import org.junit.Before;
import org.junit.Test;

import static io.github.jonestimd.finance.domain.fileimport.AmountFormat.*;
import static io.github.jonestimd.finance.domain.fileimport.FieldType.*;
import static org.assertj.swing.assertions.Assertions.*;
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
    public void getAll() throws Exception {
        List<ImportFile> importFiles = new ArrayList<>();
        when(importFileDao.getAll()).thenReturn(importFiles);

        assertThat(fileImportOperations.getAll()).isSameAs(importFiles);

        verify(importFileDao).getAll();
    }

    @Test
    public void saveAll() throws Exception {
        List<ImportFile> importFiles = new ArrayList<>();
        List<ImportFile> result = new ArrayList<>();
        when(importFileDao.saveAll(any(List.class))).thenReturn(result);

        assertThat(fileImportOperations.saveAll(importFiles)).isSameAs(result);

        verify(importFileDao).saveAll(same(importFiles));
    }

    @Test
    public void saveAllSavesPageRegions() throws Exception {
        ImportFile importFile = new ImportFile("PDF", ImportType.SINGLE_DETAIL_ROWS, FileType.PDF, "yyyy/mm/dd");
        PageRegion region = new PageRegion();
        importFile.setFields(Sets.newHashSet(newImportField(DATE, "column1", region)));
        importFile.setPageRegions(Sets.newHashSet(region));
        List<ImportFile> importFiles = Collections.singletonList(importFile);

        fileImportOperations.saveAll(importFiles);

        verify(importFileDao).saveAll(same(importFiles));
        assertThat(importFile.getPageRegions()).containsExactly(region);
        assertThat(importFile.getFields().iterator().next().getRegion()).isSameAs(region);
    }

    @Test
    public void saveAllResetsPageRegions() throws Exception {
        ImportFile importFile = new ImportFile("not PDF", ImportType.SINGLE_DETAIL_ROWS, FileType.CSV, "yyyy/mm/dd");
        PageRegion region = new PageRegion();
        importFile.setPageRegions(Sets.newHashSet(region));
        importFile.setFields(Sets.newHashSet(newImportField(DATE, "column1", region)));
        List<ImportFile> importFiles = Collections.singletonList(importFile);

        fileImportOperations.saveAll(importFiles);

        verify(importFileDao).saveAll(same(importFiles));
        assertThat(importFile.getPageRegions()).isEmpty();
        assertThat(importFile.getFields().iterator().next().getRegion()).isNull();
    }

    @Test
    public void saveAllRemovesDetailPropsFromTransactionFields() throws Exception {
        ImportFile importFile = new ImportFile("PDF", ImportType.MULTI_DETAIL_ROWS, FileType.PDF, "yyyy/mm/dd");
        PageRegion region = new PageRegion();
        importFile.setPageRegions(Sets.newHashSet(region));
        TransactionCategory category = new TransactionCategory();
        Account account = new Account();
        importFile.setFields(Sets.newHashSet(
                newImportField(DATE, "column1", region, category, account, DECIMAL, "keep", "ignore", "memo"),
                newImportField(CATEGORY, "column2", region, category, account, DECIMAL, "keep", "ignore", "memo"),
                newImportField(TRANSFER_ACCOUNT, "column2", region, category, account, DECIMAL, "keep", "ignore", "memo")));

        fileImportOperations.saveAll(Collections.singletonList(importFile));

        for (ImportField field : importFile.getFields()) {
            assertThat(field.getRegion()).isEqualTo(region);
            assertThat(field.getAcceptRegex()).isEqualTo(field.getType().isTransaction() ? null : "keep");
            assertThat(field.getIgnoredRegex()).isEqualTo(field.getType().isTransaction() ? null : "ignore");
            assertThat(field.getMemo()).isEqualTo(field.getType().isTransaction() ? null : "memo");
            assertThat(field.getCategory()).isEqualTo(field.getType() == CATEGORY ? category : null);
            assertThat(field.getTransferAccount()).isEqualTo(field.getType() == TRANSFER_ACCOUNT ? account : null);
        }
    }

    @Test
    public void saveAll_notMultiDetail_removesTransactionTypeFromFields() throws Exception {
        ImportFile importFile = new ImportFile("PDF", ImportType.SINGLE_DETAIL_ROWS, FileType.PDF, "yyyy/mm/dd");
        TransactionCategory category = new TransactionCategory();
        Account account = new Account();
        importFile.setFields(Sets.newHashSet(
                newImportField(DATE, "column1", null, category, account, DECIMAL, "keep", "ignore", "memo"),
                newImportField(CATEGORY, "column2", null, category, account, DECIMAL, "keep", "ignore", "memo"),
                newImportField(TRANSFER_ACCOUNT, "column2", null, category, account, DECIMAL, "keep", "ignore", "memo")));

        fileImportOperations.saveAll(Collections.singletonList(importFile));

        for (ImportField field : importFile.getFields()) {
            assertThat(field.getCategory()).isNull();
            assertThat(field.getTransferAccount()).isNull();
        }
    }

    private ImportField newImportField(FieldType type, String label, PageRegion region) {
        return newImportField(type, label, region, null, null, null, null, null, null);
    }

    private ImportField newImportField(FieldType type, String label, PageRegion region, TransactionCategory category, Account account,
            AmountFormat amountFormat, String acceptRegex, String ignoreRegex, String memo) {
        ImportField field = new ImportField(Collections.singletonList(label), region);
        field.setType(type);
        field.setCategory(category);
        field.setTransferAccount(account);
        field.setAmountFormat(amountFormat);
        field.setNegate(true);
        field.setAcceptRegex(acceptRegex);
        field.setIgnoredRegex(ignoreRegex);
        field.setMemo(memo);
        return field;
    }

    @Test
    public void deleteAll() throws Exception {
        List<ImportFile> importFiles = new ArrayList<>();

        fileImportOperations.deleteAll(importFiles);

        verify(importFileDao).deleteAll(same(importFiles));
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

        verify(serviceLocator.getTransactionService()).saveTransactions(anyCollection());
    }
}