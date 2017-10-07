package io.github.jonestimd.finance.domain.fileimport;

import java.util.Collections;

import io.github.jonestimd.finance.domain.fileimport.csv.CsvImportFile;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ImportFileTest {
    @Test
    public void isNegateReturnsFalseForNullCategory() throws Exception {
        ImportFile importFile = new CsvImportFile();

        assertThat(importFile.isNegate(null)).isFalse();
    }

    @Test
    public void isNegateReturnsFalseForUnmappedCategory() throws Exception {
        ImportFile importFile = new CsvImportFile();
        importFile.setImportCategoryMap(Collections.emptyMap());

        assertThat(importFile.isNegate(new TransactionCategory("category code"))).isFalse();
    }

    @Test
    public void isNegateReturnsFalseForCategory() throws Exception {
        TransactionCategory category = new TransactionCategory("category code");
        ImportFile importFile = new CsvImportFile();
        importFile.setImportCategoryMap(Collections.singletonMap("alias", new ImportCategory(category, false)));

        assertThat(importFile.isNegate(category)).isFalse();
    }

    @Test
    public void isNegateReturnstrueForCategory() throws Exception {
        TransactionCategory category = new TransactionCategory("category code");
        ImportFile importFile = new CsvImportFile();
        importFile.setImportCategoryMap(Collections.singletonMap("alias", new ImportCategory(category, true)));

        assertThat(importFile.isNegate(category)).isTrue();
    }

    @Test
    public void getTransferAccountReturnsNullForNoMatch() throws Exception {
        CsvImportFile importFile = new CsvImportFile();
        importFile.setImportTransferMap(Collections.emptyMap());

        assertThat(importFile.getTransferAccount("")).isNull();
    }
}