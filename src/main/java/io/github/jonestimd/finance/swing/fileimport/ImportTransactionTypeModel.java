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
package io.github.jonestimd.finance.swing.fileimport;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.ImportCategory;
import io.github.jonestimd.finance.domain.fileimport.ImportTransactionType;
import io.github.jonestimd.finance.domain.fileimport.ImportTransfer;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionType;

/**
 * Placeholder model for displaying/editing category/transfer mappings in the UI.
 */
public class ImportTransactionTypeModel extends ImportTransactionType<TransactionType> {
    private TransactionType type;

    public ImportTransactionTypeModel() {
    }

    public ImportTransactionTypeModel(ImportTransactionType<?> bean) {
        this.type = bean.getType();
        setAlias(bean.getAlias());
        setNegate(bean.isNegate());
    }

    @Override
    public TransactionType getType() {
        return type;
    }

    @Override
    public void setType(TransactionType type) {
        this.type = type;
    }

    public boolean isCategory() {
        return type instanceof TransactionCategory;
    }

    public boolean isTransfer() {
        return type instanceof Account;
    }

    public ImportCategory asCategory() {
        return new ImportCategory(getAlias(), isNegate(), (TransactionCategory) type);
    }

    public ImportTransfer asTransfer() {
        return new ImportTransfer(getAlias(), isNegate(), (Account) type);
    }
}
