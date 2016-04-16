package io.github.jonestimd.finance.swing.transaction;

import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class CategoryFormatTest {
    private CategoryFormat format = new CategoryFormat();

    @Test
    public void formatsUsingCategoryKey() throws Exception {
        TransactionCategory category = new TransactionCategory(new TransactionCategory("parent"), "child");

        assertThat(format.format(category)).isEqualTo(new CategoryKeyFormat().format(category.getKey()));
    }

    @Test
    public void formatIgnoresNonCategory() throws Exception {
        assertThat(format.format(new Object())).isEqualTo("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void parseThrowsUnsupportedOperationException() throws Exception {
        format.parseObject("code");
    }
}