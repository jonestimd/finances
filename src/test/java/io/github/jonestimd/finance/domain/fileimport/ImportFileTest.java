package io.github.jonestimd.finance.domain.fileimport;

import java.util.Collections;

import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ImportFileTest {
    @Test
    public void isNegateReturnsFalseForNullCategory() throws Exception {
        ImportFile importFile = new ImportFile();

        assertThat(importFile.isNegate(null)).isFalse();
    }

    @Test
    public void isNegateReturnsFalseForUnmappedCategory() throws Exception {
        ImportFile importFile = new ImportFile();
        importFile.setImportCategories(Collections.emptySet());

        assertThat(importFile.isNegate(new TransactionCategory("category code"))).isFalse();
    }

    @Test
    public void isNegateReturnsFalseForCategory() throws Exception {
        TransactionCategory category = new TransactionCategory("category code");
        ImportFile importFile = new ImportFile();
        importFile.setImportCategories(Collections.singleton(new ImportCategory("alias", category, false)));

        assertThat(importFile.isNegate(category)).isFalse();
    }

    @Test
    public void isNegateReturnsTrueForCategory() throws Exception {
        TransactionCategory category = new TransactionCategory("category code");
        ImportFile importFile = new ImportFile();
        importFile.setImportCategories(Collections.singleton(new ImportCategory("alias", category, true)));

        assertThat(importFile.isNegate(category)).isTrue();
    }

    @Test
    public void getTransferAccountReturnsNullForNoMatch() throws Exception {
        ImportFile importFile = new ImportFile();
        importFile.setImportTransfers(Collections.emptySet());

        assertThat(importFile.getTransferAccount("")).isNull();
    }
}