package io.github.jonestimd.finance.domain.account;

import io.github.jonestimd.finance.domain.TestDomainUtils;

public class AccountBuilder {
    private final Account account = new Account();

    public AccountBuilder() {}

    public AccountBuilder(Account account) {
        TestDomainUtils.setId(this.account, account.getId());
        this.account.setCompany(account.getCompany());
        this.account.setName(account.getName());
    }

    public AccountBuilder nextId() {
        TestDomainUtils.setId(account);
        return this;
    }

    public AccountBuilder name(String name) {
        account.setName(name);
        return this;
    }

    public AccountBuilder company(Company company) {
        account.setCompany(company);
        return this;
    }

    public AccountBuilder type(AccountType type) {
        account.setType(type);
        return this;
    }

    public Account get() {
        if (account.getName() == null && account.getId() != null) {
            account.setName("account #" + account.getId());
        }
        return  account;
    }
}
