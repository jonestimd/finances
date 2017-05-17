package io.github.jonestimd.finance.swing.transaction;

import io.github.jonestimd.finance.domain.transaction.CategoryKey;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import org.junit.Test;

import static io.github.jonestimd.finance.swing.transaction.CategoryKeyFormat.*;
import static org.assertj.core.api.Assertions.*;

public class CategoryKeyFormatTest {
    public static final String PARENT_CODE = "parent";
    public static final String CHILD_CODE = "child";
    public static final String NESTED_CODE = PARENT_CODE + SEPARATOR + CHILD_CODE;

    @Test
    public void formatIgnoresNull() throws Exception {
        assertThat(new CategoryKeyFormat().format(null)).isEqualTo("");
    }

    @Test
    public void formatsSimpleCategory() throws Exception {
        CategoryKey key = new CategoryKey(null, "code");

        assertThat(new CategoryKeyFormat().format(key)).isEqualTo(key.getCode());
    }

    @Test
    public void formatsChildCategory() throws Exception {
        CategoryKey key = new CategoryKey(new TransactionCategory(PARENT_CODE), CHILD_CODE);

        assertThat(new CategoryKeyFormat().format(key)).isEqualTo(NESTED_CODE);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void parseThrowsException() throws Exception {
        new CategoryKeyFormat().parseObject("code");
    }
}