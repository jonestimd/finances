// The MIT License (MIT)
//
// Copyright (c) 2022 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.github.jonestimd.cache.Cacheable;
import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.CompanyDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.event.AccountEvent;
import io.github.jonestimd.finance.domain.event.CompanyEvent;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.util.Streams;

public class AccountOperationsImpl implements AccountOperations {
    public static final String EVENT_SOURCE = "AccountOperations";
    private static final Collector<Company, ?, Map<String, Company>> COMPANY_BY_NAME =
            Collectors.toMap(Company::getName, Function.identity(), (c1, c2) -> c1);
    private CompanyDao companyDao;
    private AccountDao accountDao;

    public AccountOperationsImpl(CompanyDao companyDao, AccountDao accountDao) {
        this.companyDao = companyDao;
        this.accountDao = accountDao;
    }

    public List<Account> getAllAccounts() {
        List<Account> accounts = accountDao.getAll();
        Collections.sort(accounts);
        return accounts;
    }

    public List<AccountSummary> getAccountSummaries() {
        return accountDao.getAccountSummaries();
    }

    public List<Company> getAllCompanies() {
        return companyDao.getAll(Company.ACCOUNTS);
    }

    public Company save(Company company) {
        return companyDao.save(company);
    }

    public <T extends Iterable<Company>> T saveCompanies(T companies) {
        return companyDao.saveAll(companies);
    }

    public void deleteCompanies(Collection<Company> companies) {
        accountDao.removeAccountsFromCompanies(companies);
        companyDao.deleteAll(companies);
    }

    public Account getAccount(long id) {
        return accountDao.get(id);
    }

    @Cacheable
    public Account getAccount(Company company, String name) {
        return accountDao.getAccount(company, name);
    }

    public Account save(Account account) {
        if (account.getCompany() != null && account.getCompany().getId() == null) {
            companyDao.save(account.getCompany());
        }
        return accountDao.save(account);
    }

    public <T extends Iterable<Account>> List<DomainEvent<?, ?>> saveAll(T accounts) {
        List<DomainEvent<?, ?>> events = saveNewCompanies(accounts);
        List<Account> newAccounts = Streams.filter(accounts, Account::isNew);
        accountDao.saveAll(accounts);
        List<Account> changedAccounts = Streams.toList(accounts);
        if (! newAccounts.isEmpty()) {
            events.add(new AccountEvent(EVENT_SOURCE, EventType.ADDED, newAccounts));
            changedAccounts.removeAll(newAccounts);
        }
        events.add(new AccountEvent(EVENT_SOURCE, EventType.CHANGED, changedAccounts));
        return events;
    }

    private <T extends Iterable<Account>> List<DomainEvent<?, ?>> saveNewCompanies(T accounts) {
        List<DomainEvent<?, ?>> events = new ArrayList<>();
        Collection<Company> companies = consolidateNewCompanies(accounts);
        if (! companies.isEmpty()) {
            companyDao.saveAll(companies);
            events.add(new CompanyEvent(EVENT_SOURCE, EventType.ADDED, companies));
        }
        return events;
    }

    private <T extends Iterable<Account>> Collection<Company> consolidateNewCompanies(T accounts) {
        Map<String, Company> companyMap = Account.getNewCompanies(accounts).collect(COMPANY_BY_NAME);
        accounts.forEach(account -> {
            Company company = account.getCompany();
            if (company != null && company.isNew()) account.setCompany(companyMap.get(company.getName()));
        });
        return companyMap.values();
    }

    @Override
    public <T extends Iterable<Account>> void deleteAll(T accounts) {
        accountDao.deleteAll(accounts);
    }
}