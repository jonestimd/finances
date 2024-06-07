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
package io.github.jonestimd.finance.domain.asset;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.util.Streams;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Table;

@Entity
@Table(appliesTo = "security")
@SecondaryTable(name = "security", foreignKey = @ForeignKey(name = "security_asset_fk"),
        pkJoinColumns = {@PrimaryKeyJoinColumn(name = "asset_id", referencedColumnName = "id")})
public class Security extends Asset {
    public static final String SECURITY_SUMMARIES = "security.getSummaries";
    public static final String SECURITY_SUMMARY = "security.getSummary";
    public static final String ACCOUNT_SECURITY_SUMMARY = "security.getAccountSummary";
    public static final String SECURITY_SUMMARY_BY_ACCOUNT = "security.getSummariesByAccount";
    public static final String TYPE = "type";

    @Column(table = "security", name = "type", length = 25, nullable = false)
    private String type;
    @OneToMany(cascade=CascadeType.ALL, mappedBy="security", orphanRemoval=true) @LazyCollection(LazyCollectionOption.FALSE)
    private List<StockSplit> splits;

    public Security() {
        setScale(AssetType.SECURITY.getDefaultScale());
    }

    public Security(String name, SecurityType type) {
        this();
        setName(name);
        this.type = type.getValue();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toString() {
        return "Security[" + getName() + "]";
    }

    public List<StockSplit> getSplits() {
        return splits;
    }

    public void setSplits(List<StockSplit> splits) {
        this.splits = splits;
    }

    public void updateSplits(Collection<StockSplit> splits) {
        Map<Long, StockSplit> byId = Streams.uniqueIndex(this.splits, UniqueId::getId);
        for (StockSplit split : splits) {
            if (split.isNew() || ! byId.containsKey(split.getId())) this.splits.add(split);
            else {
                StockSplit persisted = byId.remove(split.getId());
                persisted.setDate(split.getDate());
                persisted.setSharesIn(split.getSharesIn());
                persisted.setSharesOut(split.getSharesOut());
            }
        }
        this.splits.removeAll(byId.values());
    }

    public BigDecimal applySplits(BigDecimal shares, Date fromDate, Date toDate) {
        return getSplitRatio(fromDate, toDate).apply(shares, getScale());
    }

    public BigDecimal revertSplits(BigDecimal shares, Date fromDate, Date toDate) {
        return getSplitRatio(fromDate, toDate).revert(shares, getScale());
    }

    public SplitRatio getSplitRatio(Date fromDate, Date toDate) {
        if (fromDate != null && splits != null) {
            return splits.stream().filter(isBetween(fromDate, toDate))
                    .map(StockSplit::getSplitRatio)
                    .reduce(SplitRatio::multiply).orElseGet(SplitRatio::new);
        }
        return new SplitRatio();
    }

    private static Predicate<StockSplit> isBetween(Date fromDate, Date toDate) {
        return split -> ! split.getDate().before(fromDate) && (toDate == null || ! split.getDate().after(toDate));
    }
}