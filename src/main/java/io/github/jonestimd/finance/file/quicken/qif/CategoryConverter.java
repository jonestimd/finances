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
package io.github.jonestimd.finance.file.quicken.qif;

import java.util.Collections;
import java.util.Set;

import io.github.jonestimd.finance.operations.TransactionCategoryOperations;

public class CategoryConverter implements RecordConverter {

    private static final Set<String> TYPES = Collections.singleton("Type:Cat");

    private TransactionCategoryOperations TransactionCategoryOperations;

    public CategoryConverter(TransactionCategoryOperations TransactionCategoryOperations) {
        this.TransactionCategoryOperations = TransactionCategoryOperations;
    }

    @Override
    public String getStatusKey() {
        return "import.qif.category.converter.status";
    }

    public Set<String> getTypes() {
        return TYPES;
    }

    public void importRecord(AccountHolder accountHolder, QifRecord record) {
        CategoryParser parser = new CategoryParser(record.getValue(QifField.NAME));
        TransactionCategoryOperations.getOrCreateTransactionCategory(record.getValue(QifField.DESCRIPTION),
                record.hasValue(QifField.INCOME), parser.getCategoryNames());
    }
}
