package io.github.jonestimd.finance.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.CompanyDao;
import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AccountOperationsImplTest {
    private CompanyDao companyDao = mock(CompanyDao.class);
    private AccountDao accountDao = mock(AccountDao.class);
    private AccountOperationsImpl accountOperations = new AccountOperationsImpl(companyDao, accountDao);
    private long nextId = 1L;

    @Test
    public void getAllAccountsWithCompany() throws Exception {
        List<Account> expectedAccounts = Lists.newArrayList(
                new AccountBuilder().name("xyz").get(),
                new AccountBuilder().name("abc").get());
        when(accountDao.getAll()).thenReturn(expectedAccounts);

        List<Account> accounts = accountOperations.getAllAccounts();

        assertThat(accounts).isSameAs(expectedAccounts);
        assertThat(accounts.get(0).getName()).isEqualTo("abc");
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
    public void deleteCompaniesUpdatesAccounts() throws Exception {
        List<Company> inCompanies = Collections.singletonList(new Company());
        accountDao.removeAccountsFromCompanies(inCompanies);

        accountOperations.deleteCompanies(inCompanies);

        verify(companyDao).deleteAll(inCompanies);
    }

    private Iterable<Account> answerSaveAllAccounts(InvocationOnMock invocation) {
        List<Account> accounts = (List<Account>) invocation.getArguments()[0];
        accounts.forEach(account -> {
            if (account.getId() == null) TestDomainUtils.setId(account, nextId++);
        });
        return accounts;
    }

    @Test
    public void saveAllAccounts() throws Exception {
        Company oldCompany = new Company(1L);
        Account account1 = TestDomainUtils.createAccount("account 1");
        Account account2 = new AccountBuilder().name("account 2").get();
        Account account3 = new AccountBuilder().name("account 3").company(oldCompany).get();
        List<Account> accounts = Arrays.asList(account1, account2, account3);
        when(accountDao.saveAll(accounts)).thenAnswer(this::answerSaveAllAccounts);

        List<DomainEvent<?, ?>> events = accountOperations.saveAll(accounts);

        assertThat(events).hasSize(2);
        checkEvent((DomainEvent<Long, Account>) events.get(0), Account.class, EventType.ADDED, account2, account3);
        checkEvent((DomainEvent<Long, Account>) events.get(1), Account.class, EventType.CHANGED, account1);
        verifyNoInteractions(companyDao);
        verify(accountDao).saveAll(accounts);
    }

    private Iterable<Company> answerSaveAllCompanies(InvocationOnMock invocation) {
        Collection<Company> companies = (Collection<Company>) invocation.getArguments()[0];
        companies.forEach(company -> {
            if (company != null && company.isNew()) TestDomainUtils.setId(company, nextId++);
        });
        return companies;
    }

    @Test
    public void saveAllAccountsWithReparentAndRename() throws Exception {
        Company newCompany = new Company("new company");
        Company oldCompany = new Company(1L);
        Account account1 = TestDomainUtils.createAccount("account 1");
        Account account2 = new AccountBuilder().name("account 2").company(newCompany).get();
        Account account3 = new AccountBuilder().name("account 3").company(oldCompany).get();
        List<Account> accounts = Arrays.asList(account1, account2, account3);
        when(accountDao.saveAll(accounts)).thenAnswer(this::answerSaveAllAccounts);
        when(companyDao.saveAll(anyIterable())).thenAnswer(this::answerSaveAllCompanies);

        List<DomainEvent<?, ?>> events = accountOperations.saveAll(accounts);

        assertThat(events).hasSize(3);
        checkEvent((DomainEvent<Long, Company>) events.get(0), Company.class, EventType.ADDED, newCompany);
        checkEvent((DomainEvent<Long, Account>) events.get(1), Account.class, EventType.ADDED, account2, account3);
        checkEvent((DomainEvent<Long, Account>) events.get(2), Account.class, EventType.CHANGED, account1);
        ArgumentCaptor<Collection<Company>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(companyDao).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(newCompany);
        verify(accountDao).saveAll(accounts);
    }

    @SuppressWarnings("unchecked")
    private <T extends UniqueId<Long>> void checkEvent(DomainEvent<Long, T> event, Class<T> domainClass, EventType type, T... entities) {
        assertThat(event.getDomainClass()).isEqualTo(domainClass);
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getDomainObjects()).containsOnly(entities);
    }

    @Test
    public void saveAllAccountsConsolidatesNewCompany() throws Exception {
        Company company1 = new Company("new company");
        Company company2 = new Company("new company");
        Account account1 = new AccountBuilder().id(1L).name("account 1").company(company1).get();
        Account account2 = new AccountBuilder().id(2L).name("account 2").company(company2).get();
        List<Account> accounts = Arrays.asList(account1, account2);
        when(accountDao.saveAll(accounts)).thenAnswer(this::answerSaveAllAccounts);
        when(companyDao.saveAll(anyIterable())).thenAnswer(this::answerSaveAllCompanies);

        List<DomainEvent<?, ?>> events = accountOperations.saveAll(accounts);

        assertThat(events).hasSize(2);
        checkEvent((DomainEvent<Long, Company>) events.get(0), Company.class, EventType.ADDED, company1);
        checkEvent((DomainEvent<Long, Account>) events.get(1), Account.class, EventType.CHANGED, account1, account2);
        ArgumentCaptor<Collection<Company>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(companyDao).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(company1);
        verify(accountDao).saveAll(accounts);
    }

    @Test
    public void deleteAllCallsDao() throws Exception {
        List<Account> accounts = new ArrayList<>();

        accountOperations.deleteAll(accounts);

        verify(accountDao).deleteAll(same(accounts));
    }
}