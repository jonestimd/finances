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
package io.github.jonestimd.finance.domain.fileimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "import_field")
@SequenceGenerator(name = "id_generator", sequenceName = "import_field_id_seq")
public class ImportField implements UniqueId<Long>, Cloneable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    @Column(name = "id", nullable = false)
    private Long id;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "import_field_label",
            joinColumns = @JoinColumn(name = "import_field_id", nullable = false),
            foreignKey = @javax.persistence.ForeignKey(name = "import_field_label_fk"))
    @ForeignKey(name = "import_field_label_fk")
    @Column(name = "label", nullable = false)
    @OrderColumn(name = "list_index", nullable = false)
    private List<String> labels;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private FieldType type;
    @ManyToOne
    @JoinColumn(name = "import_page_region_id") @ForeignKey(name = "import_field_page_region_fk")
    private PageRegion region;
    @Column(name = "number_format", length = 15)
    @Enumerated(EnumType.STRING)
    private AmountFormat amountFormat;
    @Column(name = "negate", nullable = false)
    @Type(type = "yes_no")
    private boolean negate;
    @Column(name = "memo", length = 2000)
    private String memo;
    @Column(name = "ignore_regex", length = 2000)
    private String ignoreRegex;
    @Column(name = "accept_regex", length = 2000)
    private String acceptRegex;
    @ManyToOne
    @JoinColumn(name = "tx_category_id") @ForeignKey(name = "import_field_tx_category_fk")
    private TransactionCategory category;
    @ManyToOne
    @JoinColumn(name = "transfer_account_id") @ForeignKey(name = "import_field_transfer_account_fk")
    private Account transferAccount;

    public ImportField() {
        this(null, null);
    }

    public ImportField(List<String> labels, PageRegion region) {
        this.labels = labels;
        this.region = region;
    }

    public Long getId() {
        return id;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    /**
     * Get the alias used to map to a category, security, or payee.
     */
    public String getAlias() {
        return Joiner.on("\n").join(labels);
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public PageRegion getRegion() {
        return region;
    }

    public void setRegion(PageRegion region) {
        this.region = region;
    }

    public boolean isInRegion(float x, float y) {
        return  region == null || region.top() >= y && y >= region.bottom()
                && Math.min(region.labelLeft(), region.valueLeft()) <= x
                && x <= Math.max(region.labelRight(), region.valueRight());
    }

    public boolean isLabelRegion(float x) {
        return region == null || region.labelLeft() <= x && x <= region.labelRight();
    }

    public boolean isValueRegion(float x) {
        return region == null || region.valueLeft() <= x && x <= region.valueRight();
    }

    public boolean isPastRightEdge(float x) {
        return  region != null && x > region.valueRight() && x > region.labelRight();
    }

    public AmountFormat getAmountFormat() {
        return amountFormat;
    }

    public void setAmountFormat(AmountFormat amountFormat) {
        this.amountFormat = amountFormat;
    }

    public boolean isNegate() {
        return negate;
    }

    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo != null && !memo.trim().isEmpty() ? memo : null;
    }

    public String getIgnoredRegex() {
        return ignoreRegex;
    }

    public void setIgnoredRegex(String ignoreRegex) {
        this.ignoreRegex = ignoreRegex != null && !ignoreRegex.isEmpty() ? ignoreRegex : null;
    }

    public String getAcceptRegex() {
        return acceptRegex;
    }

    public void setAcceptRegex(String acceptRegex) {
        this.acceptRegex = acceptRegex != null && !acceptRegex.isEmpty() ? acceptRegex : null;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public Account getTransferAccount() {
        return transferAccount;
    }

    public void setTransferAccount(Account transferAccount) {
        this.transferAccount = transferAccount;
    }

    public boolean hasLabel(String value) {
        return labels.stream().anyMatch(value::equalsIgnoreCase);
    }

    public boolean isMatch(String upperPrefix) {
        return labels.stream().anyMatch(label -> label.toUpperCase().startsWith(upperPrefix));
    }

    public String getValue(Map<String, String> record) {
        String value = labels.stream().map(label -> record.getOrDefault(label, "")).collect(Collectors.joining("\n"));
        return filterValue(value) ? null : value;
    }

    private boolean filterValue(String value) {
        return ignoreRegex != null && value.matches(ignoreRegex) || acceptRegex != null && ! value.matches(acceptRegex);
    }

    public BigDecimal parseAmount(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = amountFormat.toBigDecimal(value.replaceAll("[^0-9.\\-]", ""));
        return negate ? amount.negate() : amount;
    }

    public ImportField clone() {
        try {
            ImportField clone = (ImportField) super.clone();
            clone.id = null;
            clone.labels = new ArrayList<>(labels);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the amount and memo on a transaction detail for a multi-detail import record.
     */
    public void updateDetail(TransactionDetail detail, String amount) {
        detail.setMemo(memo);
        detail.setAmount(parseAmount(amount));
        if (category != null) detail.setCategory(category);
        if (transferAccount != null) {
            detail.setRelatedDetail(new TransactionDetail(transferAccount, detail));
        }
    }

    public String toString() {
        return "{ImportField: id=" + id + ", labels=" + labels + "}";
    }
}