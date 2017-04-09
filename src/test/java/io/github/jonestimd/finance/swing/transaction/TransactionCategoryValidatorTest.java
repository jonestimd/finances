package io.github.jonestimd.finance.swing.transaction;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TransactionCategoryValidatorTest {
    @Test
    public void testValidateCode() throws Exception {
        TransactionCategoryValidator validator = new TransactionCategoryValidator(false);
        assertThat(validator.validate(":")).isEqualTo("Category code must not be blank.");
        assertThat(validator.validate(":a")).isEqualTo("Category code must not be blank.");
        assertThat(validator.validate("a:")).isEqualTo("Category code must not be blank.");
        assertThat(validator.validate("a:a")).isEqualTo("Category code must not be blank.");
        assertThat(validator.validate(" ")).isEqualTo("Category code must not be blank.");

        assertThat(validator.validate("a ")).isNull();
        assertThat(validator.validate(" a")).isNull();
        assertThat(validator.validate(" a ")).isNull();
        assertThat(validator.validate("a")).isNull();
    }

    @Test
    public void testValidateNestedCodes() throws Exception {
        TransactionCategoryValidator validator = new TransactionCategoryValidator(true);
        assertThat(validator.validate(":")).isEqualTo("Category codes must not be blank.");
        assertThat(validator.validate("a:a:")).isEqualTo("Category codes must not be blank.");
        assertThat(validator.validate(":a:a")).isEqualTo("Category codes must not be blank.");
        assertThat(validator.validate("a:  :a")).isEqualTo("Category codes must not be blank.");
        assertThat(validator.validate(":a:")).isEqualTo("Category codes must not be blank.");
        assertThat(validator.validate(" :a")).isEqualTo("Category codes must not be blank.");
        assertThat(validator.validate(" : ")).isEqualTo("Category codes must not be blank.");
        assertThat(validator.validate("a: ")).isEqualTo("Category codes must not be blank.");

        assertThat(validator.validate("")).isNull();
        assertThat(validator.validate(" a ")).isNull();
        assertThat(validator.validate(" a : a ")).isNull();
        assertThat(validator.validate("a : a: a")).isNull();
        assertThat(validator.validate("a: a:a")).isNull();
        assertThat(validator.validate("a:a:a")).isNull();
    }
}