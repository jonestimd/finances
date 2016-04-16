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

import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionGroupSummary;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;

public class TransactionGroupColumnAdapter<V> extends FunctionColumnAdapter<TransactionGroupSummary, V> {
    private TransactionGroupColumnAdapter(String resourcePrefix, Class<V> valueType, Function<TransactionGroup, V> getter, BiConsumer<TransactionGroup, V> setter) {
        super(BundleType.LABELS.get(), "table.transactionGroup.", resourcePrefix, valueType, TransactionSummary.compose(getter), TransactionSummary.compose(setter));
    }

    private static abstract class ValidatedColumnAdapter<V> extends TransactionGroupColumnAdapter<V> implements BeanPropertyValidator<TransactionGroupSummary, V> {
        private ValidatedColumnAdapter(String resourcePrefix, Class<V> valueType, Function<TransactionGroup, V> getter, BiConsumer<TransactionGroup, V> setter) {
            super(resourcePrefix, valueType, getter, setter);
        }
    }

    public static final TransactionGroupColumnAdapter<String> NAME_ADAPTER =
        new ValidatedColumnAdapter<String>(TransactionGroup.NAME, String.class, TransactionGroup::getName, TransactionGroup::setName) {
            private final TransactionGroupNameValidator<TransactionGroupSummary> validator =
                    new TransactionGroupNameValidator<>(TransactionGroupSummary::getName);

            public String validate(int selectedIndex, String name, List<? extends TransactionGroupSummary> existingValues) {
                return validator.validate(selectedIndex, name, existingValues);
            }
        };

    public static final TransactionGroupColumnAdapter<String> DESCRIPTION_ADAPTER =
        new TransactionGroupColumnAdapter<>(TransactionGroup.DESCRIPTION, String.class, TransactionGroup::getDescription, TransactionGroup::setDescription);
}