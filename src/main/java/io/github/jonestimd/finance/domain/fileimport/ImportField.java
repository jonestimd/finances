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
package io.github.jonestimd.finance.domain.fileimport;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "import_field")
@SequenceGenerator(name = "id_generator", sequenceName = "import_field_id_seq")
public class ImportField {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
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
    @Column(name = "date_format", length = 50)
    private String dateFormat;
    @Column(name = "negate", nullable = false)
    @Type(type = "yes_no")
    private boolean negate;
    @Column(name = "memo", length = 2000)
    private String memo;
    @Column(name = "ignore_regex", length = 2000)
    private String ignoreRegex;
    @Column(name = "accept_regex", length = 2000)
    private String acceptRegex;

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
        return  region == null || region.getTop() >= y && y >= region.getBottom()
                && Math.min(region.getLabelLeft(), region.getValueLeft()) <= x
                && x <= Math.max(region.getLabelRight(), region.getValueRight());
    }

    public boolean isLabelRegion(float x) {
        return region == null || region.getLabelLeft() <= x && x <= region.getLabelRight();
    }

    public boolean isValueRegion(float x) {
        return region == null || region.getValueLeft() <= x && x <= region.getValueRight();
    }

    public boolean isPastRightEdge(float x) {
        return  region != null && x > region.getValueRight() && x > region.getLabelRight();
    }

    public AmountFormat getAmountFormat() {
        return amountFormat;
    }

    public void setAmountFormat(AmountFormat amountFormat) {
        this.amountFormat = amountFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
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
        this.memo = memo;
    }

    public String getIgnoredRegex() {
        return ignoreRegex;
    }

    public void setIgnoredRegex(String ignoreRegex) {
        this.ignoreRegex = ignoreRegex;
    }

    public String getAcceptRegex() {
        return acceptRegex;
    }

    public void setAcceptRegex(String acceptRegex) {
        this.acceptRegex = acceptRegex;
    }

    public boolean hasLabel(String value) {
        return labels.stream().anyMatch(value::equalsIgnoreCase);
    }

    public boolean isMatch(String upperPrefix) {
        return labels.stream().anyMatch(label -> label.toUpperCase().startsWith(upperPrefix));
    }

    public String getValue(Map<String, String> record) {
        String value = Joiner.on('\n').join(labels.stream().map(label -> record.getOrDefault(label, "")).collect(Collectors.toList()));
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

    public Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat(dateFormat).parse(dateString);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Set the amount and memo on a transaction detail for a multidetail import record.
     */
    public void updateDetail(TransactionDetail detail, String amount) {
        detail.setMemo(memo);
        detail.setAmount(parseAmount(amount));
    }

    public String toString() {
        return "{ImportField: id=" + id + ", labels=" + labels + "}";
    }
}