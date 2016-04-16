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
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.google.common.base.Function;
import io.github.jonestimd.finance.domain.BaseDomain;

@Entity @Table(name="transaction_group", uniqueConstraints={@UniqueConstraint(name = "transaction_group_ak", columnNames={"name"})})
@SequenceGenerator(name="id_generator", sequenceName="transaction_group_id_seq")
@NamedQuery(name = TransactionGroup.SUMMARY_QUERY,
    query = "select g, (select count(distinct td.transaction) from TransactionDetail td where td.group = g) as useCount from TransactionGroup g")
public class TransactionGroup extends BaseDomain<Long> implements Comparable<TransactionGroup> {
    public static final String SUMMARY_QUERY = "transactionGroup.getSummaries";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final Function<TransactionGroup, String> GET_NAME = new Function<TransactionGroup, String>() {
        public String apply(TransactionGroup input) {
            return input.getName();
        }
    };

    @Id @GeneratedValue(strategy=GenerationType.AUTO, generator="id_generator")
    private Long id;
    @Column(name="name", nullable=false, length=50)
    private String name;
    @Lob @Column(name="description")
    private String description;

    public TransactionGroup() {}

    public TransactionGroup(long id) {
        this.id = id;
    }

    public TransactionGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int hashCode() {
        return 31 + ((name == null) ? 0 : name.toUpperCase().hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TransactionGroup that = (TransactionGroup) obj;
        if (name == null ? that.name != null : ! name.equalsIgnoreCase(that.name)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "Transaction Group (" + name + ")";
    }

    public int compareTo(TransactionGroup that) {
        return that == null ? 1 : name.compareTo(that.name);
    }
}