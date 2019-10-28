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
package io.github.jonestimd.finance.swing.transaction;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.transaction.AmountType;
import io.github.jonestimd.finance.domain.transaction.CategoryKey;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class TransactionCategoryColumnAdapter<V> extends FunctionColumnAdapter<TransactionCategorySummary, V> {
    public static final String RESOURCE_PREFIX = "table.transactionCategory.";
    private static final Logger logger = Logger.getLogger(TransactionCategoryColumnAdapter.class);
    private static final BeanPropertyValidator<TransactionCategorySummary, CategoryKey> KEY_VALIDATOR = (selectedIndex, value, existingCategories) -> {
        logger.trace("validate code: " + value.getCode());
        if (StringUtils.isBlank(value.getCode())) {
            return LABELS.getString("validation.transactionCategory.invalidCode");
        }
        TransactionCategorySummary category = existingCategories.get(selectedIndex);
        if (existingCategories.stream().map(TransactionCategorySummary::getCategory).filter(category.getCategory()::equals).count() > 1) {
            return LABELS.getString("validation.transactionCategory.duplicate");
        }
        return null;
    };

    private TransactionCategoryColumnAdapter(String columnId, Class<? super V> valueType, Function<TransactionCategory,V> getter, BiConsumer<TransactionCategory, V> setter) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, TransactionSummary.compose(getter), TransactionSummary.compose(setter));
    }

    @Override
    public boolean isEditable(TransactionCategorySummary row) {
        return super.isEditable(row) && ! row.getCategory().isLocked();
    }

    private static class ValidatedCategoryColumnAdapter<V> extends TransactionCategoryColumnAdapter<V> implements ValidatedColumnAdapter<TransactionCategorySummary, V> {
        public ValidatedCategoryColumnAdapter(String columnId, Class<? super V> valueType, Function<TransactionCategory,V> getter, BiConsumer<TransactionCategory, V> setter,
                                      BeanPropertyValidator<TransactionCategorySummary, V> validator) {
            super(columnId, valueType, getter, setter);
            this.validator = validator;
        }

        private final BeanPropertyValidator<TransactionCategorySummary, V> validator;

        @Override
        public String validate(int selectedIndex, V propertyValue, List<? extends TransactionCategorySummary> beans) {
            return validator.validate(selectedIndex, propertyValue, beans);
        }
    }

    public static final ColumnAdapter<TransactionCategorySummary, TransactionCategory> PARENT_ADAPTER =
        new TransactionCategoryColumnAdapter<>(TransactionCategory.PARENT, TransactionCategory.class, TransactionCategory::getParent, TransactionCategory::setParent);

    public static final ColumnAdapter<TransactionCategorySummary, Boolean> INCOME_ADAPTER =
        new TransactionCategoryColumnAdapter<>(TransactionCategory.INCOME, Boolean.class, TransactionCategory::isIncome, TransactionCategory::setIncome);

    public static final ColumnAdapter<TransactionCategorySummary, Boolean> SECURITY_ADAPTER =
        new TransactionCategoryColumnAdapter<>(TransactionCategory.SECURITY, Boolean.class, TransactionCategory::isSecurity, TransactionCategory::setSecurity);

    public static final ColumnAdapter<TransactionCategorySummary, AmountType> AMOUNT_TYPE_ADAPTER =
        new TransactionCategoryColumnAdapter<>(TransactionCategory.AMOUNT_TYPE, AmountType.class, TransactionCategory::getAmountType, TransactionCategory::setAmountType);

    public static final ValidatedColumnAdapter<TransactionCategorySummary, CategoryKey> KEY_ADAPTER =
        new ValidatedCategoryColumnAdapter<>(TransactionCategory.CODE, CategoryKey.class, TransactionCategory::getKey, TransactionCategory::setKey, KEY_VALIDATOR);

    public static final ColumnAdapter<TransactionCategorySummary, String> DESCRIPTION_ADAPTER =
        new TransactionCategoryColumnAdapter<>(TransactionCategory.DESCRIPTION, String.class, TransactionCategory::getDescription, TransactionCategory::setDescription);
}