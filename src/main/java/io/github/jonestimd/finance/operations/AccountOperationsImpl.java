// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.github.jonestimd.cache.Cacheable;
import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.CompanyDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.util.Streams;

public class AccountOperationsImpl implements AccountOperations {
    private CompanyDao companyDao;
    private AccountDao accountDao;

    public AccountOperationsImpl(CompanyDao companyDao, AccountDao accountDao) {
        this.companyDao = companyDao;
        this.accountDao = accountDao;
    }

    public List<Account> getAllAccounts() {
        return accountDao.getAll();
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

    public <T extends Iterable<Account>> T saveAll(T accounts) {
        companyDao.saveAll(Streams.of(accounts).map(Account::getCompany).filter(Objects::nonNull).filter(Company::isNew));
        return accountDao.saveAll(accounts);
    }

    @Override
    public <T extends Iterable<Account>> void deleteAll(T accounts) {
        accountDao.deleteAll(accounts);
    }
}