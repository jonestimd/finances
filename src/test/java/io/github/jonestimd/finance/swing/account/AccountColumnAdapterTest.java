package io.github.jonestimd.finance.swing.account;

import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class AccountColumnAdapterTest {
    @Test
    public void testIsTypeEditable() throws Exception {
        assertThat(AccountColumnAdapter.TYPE_ADAPTER.isEditable(new AccountSummary(new Account(), 0L, BigDecimal.ZERO))).isTrue();
        assertThat(AccountColumnAdapter.TYPE_ADAPTER.isEditable(new AccountSummary(TestDomainUtils.createAccount("existing account"), 0L, BigDecimal.ZERO))).isFalse();
    }
}