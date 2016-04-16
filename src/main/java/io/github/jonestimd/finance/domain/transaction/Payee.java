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
package io.github.jonestimd.finance.domain.transaction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.UniqueName;

@Entity
@Table(name = "payee", uniqueConstraints = @UniqueConstraint(name = "payee_ak", columnNames = "name"))
@SequenceGenerator(name = "id_generator", sequenceName = "payee_id_seq")
@NamedQuery(name = Payee.SUMMARY_QUERY, query = "select payee," +
        " (select count(*) from Transaction t where t.payee = payee) as useCount," +
        " (select max(date) from Transaction t where t.payee = payee) as latestTransaction from Payee payee")
public class Payee extends BaseDomain<Long> implements UniqueName, Comparable<Payee> {
    public static final String SUMMARY_QUERY = "payee.getSummaries";
    public static final String NAME = "name";

    @Id @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    private Long id;
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    public Payee() {}

    public Payee(String name) {
        this.name = name;
    }

    public Payee(long id, String name) {
        this(name);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object object) {
        if (object instanceof Payee) {
            Payee other = (Payee) object;
            return name == null ? other.name == null : name.equalsIgnoreCase(other.name);
        }
        return false;
    }

    public int hashCode() {
        return name == null ? 0 : name.toUpperCase().hashCode();
    }

    public int compareTo(Payee other) {
        return other == null ? 1 : name.toUpperCase().compareTo(other.name.toUpperCase());
    }

    public String toString() {
        return "Payee[" + name + "]";
    }
}