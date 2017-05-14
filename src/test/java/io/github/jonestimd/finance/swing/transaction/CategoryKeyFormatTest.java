package io.github.jonestimd.finance.swing.transaction;

import java.text.ParseException;
import java.util.stream.Stream;

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

    @Test(expected = ParseException.class)
    public void parseFailsForEmptyString() throws Exception {
        new CategoryKeyFormat().parseObject("");
    }

    @Test(expected = ParseException.class)
    public void parseFailsForWhiteSpace() throws Exception {
        new CategoryKeyFormat().parseObject(" ");
    }

    @Test(expected = ParseException.class)
    public void parseFailsForEmptyCodes() throws Exception {
        new CategoryKeyFormat().parseObject(SEPARATOR + SEPARATOR);
    }

    @Test
    public void parseCreatesCategory() throws Exception {
        CategoryKey key = new CategoryKeyFormat().parseObject("code");

        assertThat(key.getCode()).isEqualTo("code");
        assertThat(key.getParent()).isNull();
    }

    @Test
    public void parseCreatesNestedCategories() throws Exception {
        CategoryKey key = new CategoryKeyFormat().parseObject(NESTED_CODE);

        assertThat(key.getCode()).isEqualTo(CHILD_CODE);
        assertThat(key.getParent().getCode()).isEqualTo(PARENT_CODE);
        assertThat(key.getParent().getParent()).isNull();
    }

    @Test
    public void parseReusesCategory() throws Exception {
        TransactionCategory category = new TransactionCategory("code");

        CategoryKey key = new CategoryKeyFormat(Stream.of(category)).parseObject(category.getCode());

        assertThat(key).isSameAs(category.getKey());
    }

    @Test
    public void parseReusesNestedCategories() throws Exception {
        TransactionCategory parent = new TransactionCategory(PARENT_CODE);

        CategoryKey key = new CategoryKeyFormat(Stream.of(parent)).parseObject(NESTED_CODE);

        assertThat(key.getCode()).isEqualTo(CHILD_CODE);
        assertThat(key.getParent()).isSameAs(parent);
        assertThat(key.getParent().getParent()).isNull();
    }

    @Test
    public void parseTrimsCodes() throws Exception {
        TransactionCategory parent = new TransactionCategory(PARENT_CODE);

        CategoryKey key = new CategoryKeyFormat(Stream.of(parent)).parseObject(PARENT_CODE + " " + SEPARATOR + " " + CHILD_CODE + " ");

        assertThat(key.getCode()).isEqualTo(CHILD_CODE);
        assertThat(key.getParent()).isSameAs(parent);
        assertThat(key.getParent().getParent()).isNull();
    }
}