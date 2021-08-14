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
package io.github.jonestimd.finance.domain.asset;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import io.github.jonestimd.finance.domain.BaseDomain;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "asset", uniqueConstraints = { @UniqueConstraint(name = "asset_ak", columnNames = {"name", "type"}) })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) @DiscriminatorColumn(name = "type")
@SequenceGenerator(name = "id_generator", sequenceName = "asset_id_seq")
public abstract class Asset extends BaseDomain<Long> implements Comparable<Asset> {
    public static final String NAME = "name";
    public static final String SYMBOL = "symbol";

    @Id @GeneratedValue(strategy = GenerationType.AUTO, generator = "id_generator")
    @GenericGenerator(name = "id_generator", strategy = "native")
    private Long id;
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    @Column(name = "symbol", length = 10)
    private String symbol;
    @Column(name = "scale", nullable = false)
    private int scale;

    protected Asset() {}

    protected Asset(long id) {
        this.id = id;
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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Asset && getClass().equals(obj.getClass())) {
            Asset that = (Asset) obj;
            return StringUtils.equalsIgnoreCase(name, that.name);
        }
        return false;
    }

    public int hashCode() {
        return name == null ? 0 : name.toUpperCase().hashCode();
    }

    public int compareTo(Asset other) {
        return other == null ? 1 : name.compareToIgnoreCase(other.name);
    }
}