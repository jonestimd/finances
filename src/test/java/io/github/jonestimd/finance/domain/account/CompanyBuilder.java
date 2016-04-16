package io.github.jonestimd.finance.domain.account;

import io.github.jonestimd.finance.domain.TestDomainUtils;

public class CompanyBuilder {
    private final Company company = new Company();

    public CompanyBuilder() {}

    public CompanyBuilder(Company company) {
        TestDomainUtils.setId(this.company, company.getId());
        this.company.setName(company.getName());
    }

    public CompanyBuilder nextId() {
        TestDomainUtils.setId(company);
        return this;
    }

    public CompanyBuilder name(String name) {
        company.setName(name);
        return this;
    }

    public Company get() {
        return company;
    }
}
