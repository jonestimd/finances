package io.github.jonestimd.finance.swing.account;

import io.github.jonestimd.finance.domain.account.AccountType;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountTypeValidatorTest {
    @Test
    public void testTypeIsRequired() throws Exception {
        AccountTypeValidator validator = new AccountTypeValidator();

        assertEquals("The account type is required.", validator.validate(null));
        assertNull(validator.validate(AccountType.BANK));
    }
}