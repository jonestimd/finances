// The MIT License (MIT)
//
// Copyright (c) 2021 Tim Jones
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import io.github.jonestimd.finance.domain.BaseDomain;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;

@Entity @Table(name = "company", uniqueConstraints = @UniqueConstraint(name = "company_ak", columnNames = "name"))
@SequenceGenerator(name = "id_generator", sequenceName = "company_id_seq")
public class Company extends BaseDomain<Long> implements Comparable<Company> {
    public static final String NAME = "name";
    public static final String ACCOUNTS = "accounts";

    @Id @GeneratedValue(strategy=GenerationType.AUTO, generator="id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    private Long id;
    @Column(nullable=false, length=100) @NaturalId(mutable=true)
    private String name;
    @OneToMany(mappedBy="company", fetch=FetchType.LAZY)
    private List<Account> accounts;

    public Company() {}

    public Company(Long id) {
        this.id = id;
    }

    public Company(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    /**
     * @return true if this Company contains {@link Account}s
     */
    public boolean nonEmpty() {
        return accounts != null && ! accounts.isEmpty();
    }

    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (obj instanceof Company) {
            Company that = (Company) obj;
            return name != null && name.equalsIgnoreCase(that.name);
        }
        return false;
    }

    public int hashCode() {
        return name == null ? 0 : name.toUpperCase().hashCode();
    }

    public int compareTo(Company that) {
        return that == null ? 1 : name.toUpperCase().compareTo(that.name.toUpperCase());
    }

    public static List<String> names(Collection<Company> comapnies) {
        List<String> names = new ArrayList<>();
        for (Company company : comapnies) {
            names.add(company.name);
        }
        return names;
    }
}