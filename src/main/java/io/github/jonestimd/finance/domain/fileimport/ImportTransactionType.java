// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import io.github.jonestimd.finance.domain.transaction.TransactionType;
import org.hibernate.annotations.Type;

@MappedSuperclass
public abstract class ImportTransactionType<T extends TransactionType> implements Cloneable {
    @Column(name = "alias", nullable = false)
    private String alias;

    @Column(name = "negate_amount", nullable = false)
    @Type(type = "yes_no")
    private boolean negate;

    public ImportTransactionType() {}

    public ImportTransactionType(String alias, boolean negate) {
        this.alias = alias;
        this.negate = negate;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public abstract T getType();

    public abstract void setType(T type);

    public boolean isNegate() {
        return negate;
    }

    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass())) return false;
        ImportTransactionType that = (ImportTransactionType) obj;
        return Objects.equals(alias, that.alias);
    }

    public int hashCode() {
        return alias == null ? 0 : alias.hashCode();
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
