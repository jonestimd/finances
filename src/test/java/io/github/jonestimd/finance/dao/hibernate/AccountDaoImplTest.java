package io.github.jonestimd.finance.dao.hibernate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.CompanyDao;
import io.github.jonestimd.finance.dao.CurrencyDao;
import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.account.Company;
import org.hibernate.Hibernate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountDaoImplTest extends HsqlTestFixture {

    private CurrencyDao currencyDao;
    private CompanyDao companyDao;
    private AccountDao accountDao;

    @Before
    public void setUpDao() throws Exception {
        currencyDao = daoContext.getCurrencyDao();
        companyDao = daoContext.getCompanyDao();
        accountDao = daoContext.getAccountDao();
    }

    protected List<QueryBatch> getInsertQueries() {
        return Collections.singletonList(ACCOUNT_BATCH);
    }

    @Test
    public void getAccountReturnsExistingAccount() throws Exception {
        String name = "Cash";
        Account account = accountDao.getAccount(null, name);

        assertEquals(1052, account.getId().intValue());
        assertSame(AccountType.CASH, account.getType());
        assertEquals(name, account.getName());
        assertEquals("Cash Account", account.getDescription());
        assertFalse(account.isClosed());
        assertNull(account.getCompany());
    }

    @Test
    public void testRemoveAccountsFromCompanies() throws Exception {
        Company company = companyDao.get(1001L);
        Account account = new Account();
        account.setCurrency(currencyDao.get((Long) ASSET_BATCH.getValue(0, "id")));
        account.setCompany(company);
        account.setName("Test account");
        account.setType(AccountType.BANK);
        accountDao.save(account);

        accountDao.removeAccountsFromCompanies(Arrays.asList(company));

        account = accountDao.get(account.getId());
        assertNull(account.getCompany());
    }

    @Test
    public void testGetAccountsWithCompany() throws Exception {
        List<Account> accounts = accountDao.getAll();

        for (Account account : accounts) {
            assertTrue(Hibernate.isInitialized(account.getCompany()));
        }
    }

    @Test
    public void getAccountSummaries() throws Exception {
        List<AccountSummary> accounts = accountDao.getAccountSummaries();

        assertFalse(accounts.isEmpty());
        assertEquals(AccountSummary.class, accounts.get(0).getClass());
    }
}