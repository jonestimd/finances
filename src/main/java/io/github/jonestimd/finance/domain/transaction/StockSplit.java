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
package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import io.github.jonestimd.finance.domain.BaseDomain;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SplitRatio;
import org.hibernate.annotations.GenericGenerator;

@NamedQueries({
    @NamedQuery(name = "stockSplit.findBySecurityAndDate",
            query = "from StockSplit where date = :date and security = :security")})

@Entity
@Table(name = "stock_split", uniqueConstraints = @UniqueConstraint(name = "stock_split_ak", columnNames = {"date", "security_id"}))
@SequenceGenerator(name = "id_generator", sequenceName = "stock_split_id_seq")
public class StockSplit extends BaseDomain<Long> {
    @Id @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(foreignKey = @ForeignKey(name = "stock_split_security_fk"))
    private Security security;
    @Column(name = "date", nullable = false) @Temporal(TemporalType.DATE)
    private Date date;
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "sharesIn", column = @Column(name = "shares_in", nullable = false, precision = 19, scale = 6)),
        @AttributeOverride(name = "sharesOut", column = @Column(name = "shares_out", nullable = false, precision = 19, scale = 6))})
    private SplitRatio splitRatio = new SplitRatio();

    public StockSplit() {}

    public StockSplit(Security security, Date date, SplitRatio splitRatio) {
        this.security = security;
        this.date = date;
        this.splitRatio = splitRatio;
    }

    public StockSplit(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public SplitRatio getSplitRatio() {
        return splitRatio;
    }

    public void setSplitRatio(SplitRatio splitRatio) {
        this.splitRatio = splitRatio;
    }

    public BigDecimal getSharesIn() {
        return splitRatio.getSharesIn();
    }

    public void setSharesIn(BigDecimal sharesIn) {
        splitRatio.setSharesIn(sharesIn);
    }

    public BigDecimal getSharesOut() {
        return splitRatio.getSharesOut();
    }

    public void setSharesOut(BigDecimal sharesOut) {
        splitRatio.setSharesOut(sharesOut);
    }

    public String toString() {
        return String.format("%1$f for %2$f on %3$tm/%3$td/%3$tY", splitRatio.getSharesOut(), splitRatio.getSharesIn(), date);
    }
}