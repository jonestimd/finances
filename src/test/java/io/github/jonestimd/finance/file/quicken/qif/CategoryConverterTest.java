package io.github.jonestimd.finance.file.quicken.qif;

import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import org.junit.Test;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CategoryConverterTest {

    private TransactionCategoryOperations txTypeOperations = mock(TransactionCategoryOperations.class);
    private CategoryConverter converter = new CategoryConverter(txTypeOperations);

    @Test
    public void getTypesIncludesCategories() throws Exception {
        assertThat(converter.getTypes().contains("Type:Cat")).isTrue();
    }

    @Test
    public void importRecordCallsGetOrCreateTransactionCategory() throws Exception {
        String code = "code";
        String description = "description";
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, code);
        record.setValue(DESCRIPTION, description);

        converter.importRecord(null, record);

        verify(txTypeOperations).getOrCreateTransactionCategory(description, false, code);
    }

    @Test
    public void importRecordCreatesSubType() throws Exception {
        String description = "description";
        String parentCode = "parent";
        String childCode = "child";
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, parentCode + ':' + childCode);
        record.setValue(DESCRIPTION, description);

        converter.importRecord(null, record);

        verify(txTypeOperations).getOrCreateTransactionCategory(description, false, parentCode, childCode);
    }

    @Test
    public void incomeOverridesExpense() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, "code");
        record.setValue(INCOME, "");
        record.setValue(EXPENSE, "");

        converter.importRecord(null, record);

        verify(txTypeOperations).getOrCreateTransactionCategory(null, true, "code");
    }
}
