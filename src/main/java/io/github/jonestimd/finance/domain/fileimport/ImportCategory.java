// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import io.github.jonestimd.finance.domain.transaction.TransactionCategory;

@Embeddable
@AttributeOverride(name = "alias", column = @Column(name = "type_alias", nullable = false))
public class ImportCategory extends ImportTransactionType<TransactionCategory> {
    public static final ImportCategory EMPTY_IMPORT_CATEGORY = new ImportCategory(null, false, null);

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "tx_category_id", nullable = false, foreignKey = @ForeignKey(name = "import_category_category_fk"))
    private TransactionCategory category;

    public ImportCategory() {}

    public ImportCategory(String alias, boolean negate, TransactionCategory category) {
        super(alias, negate);
        this.category = category;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    @Override
    public TransactionCategory getType() {
        return category;
    }

    @Override
    public void setType(TransactionCategory type) {
        this.category = category;
    }

    @Override
    public ImportCategory clone() {
        return (ImportCategory) super.clone();
    }
}
