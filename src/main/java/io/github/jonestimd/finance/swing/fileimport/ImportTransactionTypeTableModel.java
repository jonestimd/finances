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

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.fileimport.ImportCategory;
import io.github.jonestimd.finance.domain.fileimport.ImportTransfer;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;

public class ImportTransactionTypeTableModel extends ValidatedBeanListTableModel<ImportTransactionTypeModel> {
    private static final List<ColumnAdapter<ImportTransactionTypeModel, ?>> COLUMN_ADAPTERS = ImmutableList.of(
        TransactionTypeColumnAdapter.VALUE_ADAPTER,
        TransactionTypeColumnAdapter.TRANSACTION_TYPE_ADAPTER,
        TransactionTypeColumnAdapter.NEGATE_ADAPTER
    );

    public ImportTransactionTypeTableModel() {
        super(COLUMN_ADAPTERS);
    }

    public Set<ImportCategory> getCategories() {
        return getBeans(ImportTransactionTypeModel::isCategory, ImportTransactionTypeModel::asCategory);
    }

    public Set<ImportTransfer> getTransfers() {
        return getBeans(ImportTransactionTypeModel::isTransfer, ImportTransactionTypeModel::asTransfer);
    }

    private <T> Set<T> getBeans(Predicate<ImportTransactionTypeModel> filter, Function<ImportTransactionTypeModel, T> converter) {
        return getBeans().stream().filter(filter).map(converter).collect(Collectors.toSet());
    }
}
