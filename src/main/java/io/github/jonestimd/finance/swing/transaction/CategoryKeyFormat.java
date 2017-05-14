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
package io.github.jonestimd.finance.swing.transaction;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Map;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.transaction.CategoryKey;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.util.Streams;

public class CategoryKeyFormat extends Format {
    public static final String SEPARATOR = BundleType.LABELS.getString("io.github.jonestimd.finance.category.code.separator");
    private final Map<CategoryKey, TransactionCategory> categoryMap;

    public CategoryKeyFormat() {
        this(Stream.empty());
    }

    public CategoryKeyFormat(Stream<TransactionCategory> categories) {
        this.categoryMap = Streams.uniqueIndex(categories, TransactionCategory::getKey);
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj instanceof CategoryKey) {
            toAppendTo.append(((CategoryKey) obj).qualifiedName(SEPARATOR));
        }
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        TransactionCategory category = null;
        int offset = 0;
        for (String code : source.split(SEPARATOR, -1)) {
            if (code.trim().isEmpty()) {
                pos.setErrorIndex(offset);
                return null;
            }
            offset += code.length() + 1;
            category = categoryMap.getOrDefault(new CategoryKey(category, code.trim()), new TransactionCategory(category, code.trim()));
        }
        pos.setIndex(offset);
        return category.getKey();
    }

    @Override
    public CategoryKey parseObject(String source) throws ParseException {
        return (CategoryKey) super.parseObject(source);
    }
}