// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.lang.Comparables;
import io.github.jonestimd.util.Streams;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import static io.github.jonestimd.finance.domain.transaction.SecurityAction.*;

@Entity
@Table(name = "tx_category",
       uniqueConstraints = @UniqueConstraint(name = "tx_category_key", columnNames = {"parent_id", "code"}))
@SequenceGenerator(name = "id_generator", sequenceName = "tx_category_id_seq")
@NamedQueries({
    @NamedQuery(name = TransactionCategory.SUMMARY_QUERY, query =
        "select category, (select count(distinct td.transaction) from TransactionDetail td where td.category = category) as useCount " +
        "from TransactionCategory category"),
    @NamedQuery(name = TransactionCategory.PARENTS_QUERY, query = "select distinct key.parent from TransactionCategory"),
    @NamedQuery(name = TransactionCategory.SEARCH_QUERY, query = "from TransactionCategory where lower(code) like :search")
})
public class TransactionCategory extends BaseDomain<Long> implements TransactionType {
    public static final String SUMMARY_QUERY = "transactionCategory.getSummaries";
    public static final String PARENTS_QUERY = "transactionCategory.getParents";
    public static final String SEARCH_QUERY = "transactionCategory.findByPartialCode";
    public static final String INCOME = "income";
    public static final String SECURITY = "security";
    public static final String AMOUNT_TYPE = "amountType";
    public static final String CODE = "key.code";
    public static final String PARENT = "key.parent";
    public static final String DESCRIPTION = "description";
    public static final List<SecurityAction> LOCKED_ACTIONS = ImmutableList.of(
            BUY, SELL, SHARES_IN, SHARES_OUT, DIVIDEND, REINVEST, COMMISSION_AND_FEES);
    public static final List<String> LOCKED_ACTION_CODES = Collections.unmodifiableList(Streams.map(LOCKED_ACTIONS, SecurityAction::code));

    private CategoryKey key = new CategoryKey();
    @Id @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    private Long id;
    @Column(name = "income", nullable = false, length=1) @Type(type = "yes_no")
    private boolean income;
    @Column(name = "security", nullable = false, length=1) @Type(type = "yes_no")
    private boolean security;
    @Column(name = "amount_type", nullable = false) @Type(type = "io.github.jonestimd.hibernate.EnumUserType",
            parameters = {@Parameter(name = "enumClass", value = "io.github.jonestimd.finance.domain.transaction.AmountType")})
    private AmountType amountType = AmountType.DEBIT_DEPOSIT;
    @Lob @Column(name = "description")
    private String description;
    @OneToMany(mappedBy = "key.parent")
    private List<TransactionCategory> children;

    public TransactionCategory() {}

    public TransactionCategory(long id) {
        this.id = id;
    }

    public TransactionCategory(String code) {
        this(null, code);
    }

    public TransactionCategory(TransactionCategory parent, String code) {
        this.key = new CategoryKey(parent, code);
    }

    public Long getId() {
        return id;
    }

    public CategoryKey getKey() {
        return key;
    }

    public void setKey(CategoryKey key) {
        this.key = key;
    }

    public String getCode() {
        return key.getCode();
    }

    public void setCode(String code) {
        key.setCode(code);
    }

    public TransactionCategory getParent() {
        return key.getParent();
    }

    public void setParent(TransactionCategory parent) {
        key.setParent(parent);
    }

    public List<TransactionCategory> getParents() {
        List<TransactionCategory> parents = new LinkedList<>();
        TransactionCategory parent = key.getParent();
        while (parent != null) {
            parents.add(parent);
            parent = parent.getParent();
        }
        return parents;
    }

    public List<Long> getParentIds() {
        return getParents().stream().map(UniqueId::getId).collect(Collectors.toList());
    }

    /**
     * @return a stream containing this category's ID and the IDs of all of its subcategories.
     */
    public Stream<Long> getSubcategoryIds() {
        return Stream.concat(Stream.of(id), children.stream().flatMap(TransactionCategory::getSubcategoryIds));
    }

    public boolean isTransfer() {
        return false;
    }

    public boolean isAffectsBalance() {
        return amountType.isAffectsBalance();
    }

    public boolean isIncome() {
        return income;
    }

    public void setIncome(boolean income) {
        this.income = income;
    }

    public boolean isSecurity() {
        return security;
    }

    public void setSecurity(boolean security) {
        this.security = security;
    }

    public AmountType getAmountType() {
        return amountType;
    }

    public void setAmountType(AmountType amountType) {
        this.amountType = amountType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isLocked() {
        return security && getParent() == null && LOCKED_ACTION_CODES.contains(getCode());
    }

    public int compareTo(TransactionType that) {
        if (that == null) {
            return 1;
        }
        else if (that instanceof TransactionCategory) {
            return Comparables.compareIgnoreCase(key.getCodeList(), ((TransactionCategory) that).key.getCodeList());
        }
        return qualifiedName().compareToIgnoreCase(that.qualifiedName());
    }

    public String qualifiedName(String delimiter) {
        return key.qualifiedName(delimiter);
    }

    public int hashCode() {
        return key.hashCode();
    }

    public boolean equals(Object obj) {
        return this == obj || obj != null && getClass() == obj.getClass() && key.equals(((TransactionCategory) obj).key);
    }

    public String toString() {
        return "Category{" + qualifiedName(":") + "}";
    }
}