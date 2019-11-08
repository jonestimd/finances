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
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class TransactionTypeColumnAdapter<V> extends FunctionColumnAdapter<ImportTransactionTypeModel, V> {
    public static final String RESOURCE_PREFIX = "table.importTransactionType.column.";
    private static final String VALUE_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "alias.required");
    private static final String VALUE_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "alias.unique");
    private static final String TYPE_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "transactionType.required");

    protected TransactionTypeColumnAdapter(String columnId, Class<? super V> valueType,
            Function<ImportTransactionTypeModel, V> getter, BiConsumer<ImportTransactionTypeModel, V> setter) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, getter, setter);
    }

    public static final ValidatedTypeColumnAdapter<String> VALUE_ADAPTER =
        new ValidatedTypeColumnAdapter<String>("alias", String.class,
                ImportTransactionTypeModel::getAlias, ImportTransactionTypeModel::setAlias) {
            @Override
            public String validate(int selectedIndex, String value, List<? extends ImportTransactionTypeModel> beans) {
                if (value == null || value.trim().isEmpty()) return VALUE_REQUIRED;
                ImportTransactionTypeModel row = beans.get(selectedIndex);
                if (beans.stream().filter(mapping -> mapping != row).anyMatch(mapping -> mapping.getAlias().equals(value))) {
                    return VALUE_UNIQUE;
                }
                return null;
            }
        };

    public static final ValidatedTypeColumnAdapter<TransactionType> TRANSACTION_TYPE_ADAPTER =
        new ValidatedTypeColumnAdapter<TransactionType>("transactionType", TransactionType.class,
                ImportTransactionTypeModel::getType, ImportTransactionTypeModel::setType) {
            @Override
            public String validate(int selectedIndex, TransactionType type, List<? extends ImportTransactionTypeModel> beans) {
                return type == null ? TYPE_REQUIRED : null;
            }
        };

    public static final TransactionTypeColumnAdapter<Boolean> NEGATE_ADAPTER =
        new TransactionTypeColumnAdapter<>("negate", Boolean.class,
                ImportTransactionTypeModel::isNegate, ImportTransactionTypeModel::setNegate);

    private static abstract class ValidatedTypeColumnAdapter<V> extends TransactionTypeColumnAdapter<V>
            implements ValidatedColumnAdapter<ImportTransactionTypeModel, V> {
        public ValidatedTypeColumnAdapter(String columnId, Class<? super V> valueType,
                Function<ImportTransactionTypeModel, V> getter, BiConsumer<ImportTransactionTypeModel, V> setter) {
            super(columnId, valueType, getter, setter);
        }
    }
}
