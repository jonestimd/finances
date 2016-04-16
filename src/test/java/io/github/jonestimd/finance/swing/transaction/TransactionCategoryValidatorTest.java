package io.github.jonestimd.finance.swing.transaction;

import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionCategoryValidatorTest {
    @Test
    public void testValidateCode() throws Exception {
        TransactionCategoryValidator validator = new TransactionCategoryValidator(false);
        assertEquals("Category code must not be blank.", validator.validate(":"));
        assertEquals("Category code must not be blank.", validator.validate(":a"));
        assertEquals("Category code must not be blank.", validator.validate("a:"));
        assertEquals("Category code must not be blank.", validator.validate("a:a"));
        assertEquals("Category code must not be blank.", validator.validate(" "));

        assertNull(validator.validate("a "));
        assertNull(validator.validate(" a"));
        assertNull(validator.validate(" a "));
        assertNull(validator.validate("a"));
    }

    @Test
    public void testValidateNestedCodes() throws Exception {
        TransactionCategoryValidator validator = new TransactionCategoryValidator(true);
        assertEquals("Category codes must not be blank.", validator.validate(":"));
        assertEquals("Category codes must not be blank.", validator.validate("a:a:"));
        assertEquals("Category codes must not be blank.", validator.validate(":a:a"));
        assertEquals("Category codes must not be blank.", validator.validate("a:  :a"));
        assertEquals("Category codes must not be blank.", validator.validate(":a:"));
        assertEquals("Category codes must not be blank.", validator.validate(" :a"));
        assertEquals("Category codes must not be blank.", validator.validate(" : "));
        assertEquals("Category codes must not be blank.", validator.validate("a: "));

        assertNull(validator.validate(""));
        assertNull(validator.validate(" a "));
        assertNull(validator.validate(" a : a "));
        assertNull(validator.validate("a : a: a"));
        assertNull(validator.validate("a: a:a"));
        assertNull(validator.validate("a:a:a"));
    }
}