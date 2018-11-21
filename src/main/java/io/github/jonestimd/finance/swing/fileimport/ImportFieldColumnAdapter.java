// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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

import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ImportFieldColumnAdapter<V> extends FunctionColumnAdapter<ImportField, V> {
    public static final String RESOURCE_PREFIX = "table.importField.column.";

    protected ImportFieldColumnAdapter(String columnId, Class<? super V> valueType, Function<ImportField, V> getter, BiConsumer<ImportField, V> setter) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, getter, setter);
    }

    public static final ImportFieldColumnAdapter<FieldType> TYPE_ADAPTER =
        new ImportFieldColumnAdapter<>("type", FieldType.class, ImportField::getType, ImportField::setType);

    public static final ImportFieldColumnAdapter<List<String>> LABELS_ADAPTER =
        new ImportFieldColumnAdapter<>("labels", List.class, ImportField::getLabels, ImportField::setLabels);

    public static final ImportFieldColumnAdapter<AmountFormat> AMOUNT_FORMAT_ADAPTER =
        new ImportFieldColumnAdapter<>("amountFormat", AmountFormat.class, ImportField::getAmountFormat, ImportField::setAmountFormat);

    public static final ImportFieldColumnAdapter<Boolean> NEGATE_ADAPTER =
        new ImportFieldColumnAdapter<>("negateAmount", Boolean.class, ImportField::isNegate, ImportField::setNegate);

    public static final ImportFieldColumnAdapter<String> ACCEPT_REGEX_ADAPTER =
        new ImportFieldColumnAdapter<>("acceptRegex", String.class, ImportField::getAcceptRegex, ImportField::setAcceptRegex);

    public static final ImportFieldColumnAdapter<String> IGNORE_REGEX_ADAPTER =
        new ImportFieldColumnAdapter<>("ignoreRegex", String.class, ImportField::getIgnoredRegex, ImportField::setIgnoredRegex);

    public static final ImportFieldColumnAdapter<String> MEMO_ADAPTER =
        new ImportFieldColumnAdapter<>("memo", String.class, ImportField::getMemo, ImportField::setMemo);

}
