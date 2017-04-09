package io.github.jonestimd.finance.swing.account;

import io.github.jonestimd.finance.domain.account.AccountType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class AccountTypeValidatorTest {
    @Test
    public void testTypeIsRequired() throws Exception {
        AccountTypeValidator validator = new AccountTypeValidator();

        assertThat(validator.validate(null)).isEqualTo("The account type is required.");
        assertThat(validator.validate(AccountType.BANK)).isNull();
    }
}