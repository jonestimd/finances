package io.github.jonestimd.finance.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.CompanyDao;
import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.mockito.MockitoHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AccountOperationsImplTest {
    private CompanyDao companyDao = mock(CompanyDao.class);
    private AccountDao accountDao = mock(AccountDao.class);
    private AccountOperationsImpl accountOperations = new AccountOperationsImpl(companyDao, accountDao);

    @Test
    public void getAllAccountsWithCompany() throws Exception {
        List<Account> expectedAccounts = Collections.singletonList(new Account());
        when(accountDao.getAll()).thenReturn(expectedAccounts);

        List<Account> accounts = accountOperations.getAllAccounts();

        assertThat(accounts).isSameAs(expectedAccounts);
    }

    @Test
    public void getAllCompaniesCallsDao() throws Exception {
        List<Company> expectedCompanies = Collections.singletonList(new Company());
        when(companyDao.getAll(Company.ACCOUNTS)).thenReturn(expectedCompanies);

        List<Company> companies = accountOperations.getAllCompanies();

        assertThat(companies).isSameAs(expectedCompanies);
    }

    @Test
    public void getAccountCallsDao() throws Exception {
        String accountName = "name";
        Account expectedAccount = new Account();
        when(accountDao.getAccount(null, accountName)).thenReturn(expectedAccount);

        Account account = accountOperations.getAccount(null, accountName);

        assertThat(account).isSameAs(expectedAccount);
    }

    @Test
    public void saveAccountCallsDao() throws Exception {
        Account account = new Account();
        Account expectedAccount = new Account();
        when(accountDao.save(account)).thenReturn(expectedAccount);

        assertThat(accountOperations.save(account)).isSameAs(expectedAccount);

        verify(accountDao).save(account);
    }

    @Test
    public void saveCompanyCallsDao() throws Exception {
        Company company = new Company();
        Company expectedCompany = new Company();
        when(companyDao.save(company)).thenReturn(expectedCompany);

        assertThat(accountOperations.save(company)).isSameAs(expectedCompany);

        verify(companyDao).save(company);
    }

    @Test
    public void saveCompaniesCallsDao() throws Exception {
        List<Company> companies = Collections.singletonList(new Company());
        List<Company> expectedCompanies = Collections.singletonList(new Company());
        when(companyDao.saveAll(companies)).thenReturn(expectedCompanies);

        assertThat(accountOperations.saveCompanies(companies)).isSameAs(expectedCompanies);

        verify(companyDao).saveAll(companies);
    }

    @Test
    public void testDeleteCompaniesUpdatesAccounts() throws Exception {
        List<Company> inCompanies = Collections.singletonList(new Company());
        accountDao.removeAccountsFromCompanies(inCompanies);

        accountOperations.deleteCompanies(inCompanies);

        verify(companyDao).deleteAll(inCompanies);
    }

    @Test
    public void testSaveAllAccountsWithReparentAndRename() throws Exception {
        Company newCompany = new Company("new company");
        Company oldCompany = new Company(1L);
        Account account1 = TestDomainUtils.createAccount("account 1");
        Account account2 = new AccountBuilder().name("account 2").company(newCompany).get();
        Account account3 = new AccountBuilder().name("account 3").company(oldCompany).get();
        List<Account> accounts = Arrays.asList(account1, account2, account3);
        when(accountDao.saveAll(accounts)).thenReturn(accounts);

        accountOperations.saveAll(accounts);

        ArgumentCaptor<Stream<Company>> captor = MockitoHelper.captor();
        verify(companyDao).saveAll(captor.capture());
        assertThat(captor.getValue().collect(Collectors.toList())).containsExactly(newCompany);
        verify(accountDao).saveAll(accounts);
    }

    @Test
    public void deleteAllCallsDao() throws Exception {
        List<Account> accounts = new ArrayList<>();

        accountOperations.deleteAll(accounts);

        verify(accountDao).deleteAll(same(accounts));
    }
}