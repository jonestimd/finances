package io.github.jonestimd.finance.swing.transaction;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TransactionTypeFormatTest {
    private final TransactionTypeFormat format = new TransactionTypeFormat();

    @Test
    public void formatsTransactionCategory() throws Exception {
        TransactionCategory category = new TransactionCategory(new TransactionCategory("parent"), "child");

        assertThat(format.format(category)).isEqualTo(new CategoryFormat().format(category));
    }

    @Test
    public void formatsAccount() throws Exception {
        Account account = new Account(new Company("company"), "account");

        assertThat(format.format(account)).isEqualTo(new AccountFormat().format(account));
    }

    @Test
    public void parsesTransactionCategory() throws Exception {
        assertThat(format.parseObject("code")).isEqualTo(new TransactionCategory("code"));
    }
}