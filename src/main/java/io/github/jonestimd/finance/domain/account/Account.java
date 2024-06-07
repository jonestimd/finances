// The MIT License (MIT)
//
// Copyright (c) 2024 Tim Jones
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
package io.github.jonestimd.finance.domain.account;

import java.util.Objects;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.util.Streams;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "account", uniqueConstraints = {
    @UniqueConstraint(name = "account_ak", columnNames = {"name", "company_id"})
})
@SequenceGenerator(name = "id_generator", sequenceName = "account_id_seq")
@NamedQuery(name="account.removeFromCompany",query="update Account set company = null where company in (:companies)")
public class Account extends BaseDomain<Long> implements TransactionType {
    public static final String SUMMARY_QUERY = "account.getSummaries";
    public static final String DESCRIPTION = "description";
    public static final String COMPANY = "company";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String NUMBER = "number";
    public static final String CLOSED = "closed";

    @Id @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    private Long id;
    @ManyToOne(fetch=FetchType.EAGER, optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "account_currency_fk"))
    private Currency currency;
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(foreignKey = @ForeignKey(name = "account_company_fk"))
    private Company company;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 25, nullable = false) @Type(type = "io.github.jonestimd.hibernate.EnumUserType",
            parameters = {@Parameter(name = "enumClass", value = "io.github.jonestimd.finance.domain.account.AccountType")})
    private AccountType type;
    @Column(name = "account_no", length = 25)
    private String number;
    @Column(name = "closed", nullable = false, length=1) @Type(type = "yes_no")
    private boolean closed;
    @Lob @Column(name = "description")
    private String description;

    public Account() {}

    public Account(Currency currency) {
        this.currency = currency;
    }

    public Account(Long id) {
        this.id = id;
    }

    public Account(Long id, String name) {
        this(id);
        this.name = name;
    }

    public Account(Company company, String name) {
        this.company = company;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    @Transient
    public boolean isTransfer() {
        return true;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Long getCompanyId() {
        return company == null ? null : company.getId();
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public String qualifiedName(String delimiter) {
        return company == null ? name : company.getName() + delimiter + name;
    }

    public int compareTo(TransactionType that) {
        if (that == null) {
            return 1;
        }
        return qualifiedName().compareToIgnoreCase(that.qualifiedName());
    }

    public int hashCode() {
        return Objects.hash(company, StringUtils.upperCase(name));
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj != null && getClass().equals(obj.getClass())) {
            Account that = (Account) obj;
            return Objects.equals(company, that.company) && StringUtils.equalsIgnoreCase(name, that.name);
        }
        return false;
    }

    public String toString() {
        return String.format("Account: %s[%s]", (company == null ? "" : company.getName()), name);
    }

    public static Stream<Company> getNewCompanies(Iterable<Account> accounts) {
        return Streams.of(accounts).map(Account::getCompany).filter(Objects::nonNull).filter(Company::isNew);
    }
}