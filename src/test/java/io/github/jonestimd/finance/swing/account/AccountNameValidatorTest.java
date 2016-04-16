package io.github.jonestimd.finance.swing.account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.Company;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountNameValidatorTest {
    @Test
    public void testNameIsRequired() throws Exception {
        AccountNameValidator validator = new AccountNameValidator();
        ArrayList<AccountSummary> accounts = new ArrayList<AccountSummary>();

        assertEquals("The account name must not be blank.", validator.validate(null, null, accounts));
        assertEquals("The account name must not be blank.", validator.validate(null, " ", accounts));
        assertNull(validator.validate(null, "account", accounts));
    }

    @Test
    public void testNameMustBeUniqueForCompany() throws Exception {
        Company company1 = TestDomainUtils.create(Company.class);
        Company company2 = TestDomainUtils.create(Company.class);
        AccountSummary account = createAccount(company2, "account3");
        AccountNameValidator validator = new AccountNameValidator();
        List<AccountSummary> accounts = Arrays.asList(
                account,
                createAccount(company1, "account1"),
                createAccount(company1, "account2"),
                createAccount(company2, "account1"),
                createAccount(null, "account1"));

        assertEquals("The account name must be unique for the selected company.", validator.validate(account, "account1", accounts));
        assertEquals("The account name must be unique for the selected company.", validator.validate(account, "aCCount1", accounts));
        assertNull(validator.validate(account, "account2", accounts));

        account.getTransactionAttribute().setCompany(company1);
        assertEquals("The account name must be unique for the selected company.", validator.validate(account, "account1", accounts));
        assertEquals("The account name must be unique for the selected company.", validator.validate(account, "aCCount2", accounts));

        account.getTransactionAttribute().setCompany(company2);
        assertNull("The account name must be unique for the selected company.", validator.validate(account, "account3", accounts));
        assertNull("The account name must be unique for the selected company.", validator.validate(account, "aCCount3", accounts));
        assertEquals("The account name must be unique for the selected company.", validator.validate(account, "account1", accounts));
    }

    private AccountSummary createAccount(Company company, String accountName) {
        Account account = new Account();
        account.setName(accountName);
        account.setCompany(company);
        return new AccountSummary(account, 0L, BigDecimal.ZERO);
    }
}