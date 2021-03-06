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
package io.github.jonestimd.finance.swing.account;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import io.github.jonestimd.finance.domain.account.Company;

public class CompanyFormat extends Format {

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj instanceof Company) {
            toAppendTo.append(((Company) obj).getName());
        }
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source != null && source.trim().length() > 0) {
            pos.setIndex(source.length() + 1);
            Company company = new Company();
            company.setName(source.trim());
            return company;
        }
        return null;
    }
}