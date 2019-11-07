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

import io.github.jonestimd.finance.domain.fileimport.ImportCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ImportCategoryColumnAdapter<V> extends FunctionColumnAdapter<ImportMapping<ImportCategory>, V> {
    public static final String RESOURCE_PREFIX = "table.importCategory.column.";
    private static final String VALUE_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "value.required");
    private static final String VALUE_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "value.unique");
    private static final String CATEGORY_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "category.required");

    protected ImportCategoryColumnAdapter(String columnId, Class<? super V> valueType,
            Function<ImportMapping<ImportCategory>, V> getter, BiConsumer<ImportMapping<ImportCategory>, V> setter) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, getter, setter);
    }

    public static final ValidatedCategoryColumnAdapter<String> VALUE_ADAPTER =
        new ValidatedCategoryColumnAdapter<String>("value", String.class, ImportMapping::getValue, ImportMapping::setValue) {
            @Override
            public String validate(int selectedIndex, String value, List<? extends ImportMapping<ImportCategory>> beans) {
                if (value == null || value.trim().isEmpty()) return VALUE_REQUIRED;
                ImportMapping<ImportCategory> row = beans.get(selectedIndex);
                if (beans.stream().filter(mapping -> mapping != row).anyMatch(mapping -> mapping.getValue().equals(value))) {
                    return VALUE_UNIQUE;
                }
                return null;
            }
        };

    public static final ValidatedCategoryColumnAdapter<TransactionType> CATEGORY_ADAPTER =
        new ValidatedCategoryColumnAdapter<TransactionType>("category", TransactionType.class,
                (mapping) -> mapping.getMapping().getCategory(),
                (mapping, category) -> mapping.getMapping().setCategory((TransactionCategory) category)) {
            @Override
            public String validate(int selectedIndex, TransactionType category, List<? extends ImportMapping<ImportCategory>> beans) {
                return category == null ? CATEGORY_REQUIRED : null;
            }
        };

    public static final ImportCategoryColumnAdapter<Boolean> NEGATE_ADAPTER =
        new ImportCategoryColumnAdapter<>("negate", Boolean.class,
                (mapping) -> mapping.getMapping().isNegate(),
                (mapping, negate) -> mapping.getMapping().setNegate(negate));

    private static abstract class ValidatedCategoryColumnAdapter<V> extends ImportCategoryColumnAdapter<V>
            implements ValidatedColumnAdapter<ImportMapping<ImportCategory>, V> {
        public ValidatedCategoryColumnAdapter(String columnId, Class<? super V> valueType,
                Function<ImportMapping<ImportCategory>, V> getter, BiConsumer<ImportMapping<ImportCategory>, V> setter) {
            super(columnId, valueType, getter, setter);
        }
    }
}
