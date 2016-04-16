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

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.google.common.collect.ImmutableMap;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.swing.component.IconSource;

public class TransactionTypeFormat extends Format implements IconSource {
    private static final Map<Class<?>, Icon> ICONS =  ImmutableMap.<Class<?>, Icon>builder()
        .put(TransactionCategory.class, new ImageIcon(TransactionTypeFormat.class.getResource("/io/github/jonestimd/finance/icons/blank.png")))
        .put(Account.class, new ImageIcon(TransactionTypeFormat.class.getResource("/io/github/jonestimd/finance/icons/transfer.png")))
        .build();
    private final Map<Class<?>, Format> formats = ImmutableMap.<Class<?>, Format>builder()
        .put(TransactionCategory.class, new CategoryFormat())
        .put(Account.class, new AccountFormat())
        .build();

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj != null) {
            formats.get(obj.getClass()).format(obj, toAppendTo, pos);
        }
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        TransactionCategory category = new TransactionCategory();
        category.setCode(source);
        pos.setIndex(source.length());
        return category;
    }

    public Icon getIcon(Object bean) {
        if (bean == null) {
            return null;
        }
        return ICONS.get(bean.getClass());
    }
}