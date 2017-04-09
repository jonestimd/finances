package io.github.jonestimd.finance.swing.transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.Company;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class AccountMenuSortTest {
    private Account createAccount(String companyName, String accountName) {
        Account account = new Account();
        account.setName(accountName);
        if (companyName != null) {
            account.setCompany(new Company());
            account.getCompany().setName(companyName);
        }
        return account;
    }

    @Test
    public void testNoCompany() throws Exception {
        List<Account> accounts = Arrays.asList(
                createAccount(null, "account 2"),
                createAccount(null, "account 1"));

        List<Account> sorted = new ArrayList<Account>(accounts);
        Collections.sort(sorted, new AccountsMenuSort());

        assertThat(sorted.get(0)).isSameAs(accounts.get(1));
        assertThat(sorted.get(1)).isSameAs(accounts.get(0));
    }

    @Test
    public void testSameCompanyName() throws Exception {
        List<Account> accounts = Arrays.asList(
                createAccount("company", "account 2"),
                createAccount("company", "account 1"));

        List<Account> sorted = new ArrayList<Account>(accounts);
        Collections.sort(sorted, new AccountsMenuSort());

        assertThat(sorted.get(0)).isSameAs(accounts.get(1));
        assertThat(sorted.get(1)).isSameAs(accounts.get(0));
    }

    @Test
    public void testMixedCompanyName() throws Exception {
        List<Account> accounts = Arrays.asList(
                createAccount(null, "company 1:account 2"),
                createAccount(null, "company 1"),
                createAccount("company 1", "company 3"),
                createAccount("company 1", "company 0"),
                createAccount("company 2", "account 3"),
                createAccount("company 1", "company 1"));

        List<Account> sorted = new ArrayList<Account>(accounts);
        Collections.sort(sorted, new AccountsMenuSort());

        assertThat(sorted.get(0)).isSameAs(accounts.get(3));
        assertThat(sorted.get(1)).isSameAs(accounts.get(5));
        assertThat(sorted.get(2)).isSameAs(accounts.get(2));
        assertThat(sorted.get(3)).isSameAs(accounts.get(1));
        assertThat(sorted.get(4)).isSameAs(accounts.get(0));
    }
}