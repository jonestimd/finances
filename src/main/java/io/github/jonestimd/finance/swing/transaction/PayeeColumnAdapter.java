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

import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import io.github.jonestimd.finance.domain.transaction.TransactionSummary;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;

public class PayeeColumnAdapter<V> extends FunctionColumnAdapter<PayeeSummary, V> implements BeanPropertyValidator<PayeeSummary, V> {
    private final BeanPropertyValidator<PayeeSummary, V> validator;

    private PayeeColumnAdapter(String columnId, Class<V> valueType, Function<Payee, V> getter, BiConsumer<Payee, V> setter,
                               BeanPropertyValidator<PayeeSummary, V> validator) {
        super(BundleType.LABELS.get(), "table.payee.", columnId, valueType, TransactionSummary.compose(getter), TransactionSummary.compose(setter));
        this.validator = validator;
    }

    public String validate(int selectedIndex, V value, List<? extends PayeeSummary> existingValues) {
        return validator.validate(selectedIndex, value, existingValues);
    }

    public static final PayeeColumnAdapter<String> NAME_ADAPTER =
            new PayeeColumnAdapter<>(Payee.NAME, String.class, Payee::getName, Payee::setName, new PayeeNameValidator());
}