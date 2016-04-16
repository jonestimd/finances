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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

@Embeddable
public class CategoryKey implements Cloneable, Comparable<CategoryKey> {
    @Column(name = "code", length = 50, nullable = false)
    private String code = "";
    @ManyToOne @JoinColumn(name = "parent_id") @ForeignKey(name = "tx_type_parent_fk")
    private TransactionCategory parent;
    @Transient
    private List<String> codeList;

    public CategoryKey() {}

    public CategoryKey(TransactionCategory parent, String code) {
        this.parent = parent;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.codeList = asList();
    }

    public TransactionCategory getParent() {
        return parent;
    }

    public void setParent(TransactionCategory parent) {
        this.parent = parent;
        this.codeList = asList();
    }

    public List<String> getCodeList() {
        if (codeList == null) {
            codeList = asList();
        }
        return codeList;
    }

    private List<String> asList() {
        List<String> codes = new ArrayList<>();
        TransactionCategory parent = this.parent;
        while (parent != null) {
            codes.add(0, parent.getCode());
            parent = parent.getParent();
        }
        codes.add(getCode());
        return codes;
    }

    public String qualifiedName(String delimiter) {
        String codeOrBlank = Optional.ofNullable(code).orElse("");
        return parent == null ? codeOrBlank : parent.qualifiedName(delimiter) + delimiter + codeOrBlank;
    }

    public CategoryKey clone() {
        try {
            return (CategoryKey) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int compareTo(CategoryKey that) {
        return qualifiedName(":").compareToIgnoreCase(that.qualifiedName(":"));
    }

    private List<Object> keyValues() {
        return Arrays.asList(parent, code == null ? null : code.toUpperCase());
    }

    public int hashCode() {
        return keyValues().hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CategoryKey that = (CategoryKey) obj;
        return keyValues().equals(that.keyValues());
    }
}