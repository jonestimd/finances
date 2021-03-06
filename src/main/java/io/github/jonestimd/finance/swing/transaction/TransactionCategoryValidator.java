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

import java.util.regex.Pattern;

import io.github.jonestimd.swing.validation.Validator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class TransactionCategoryValidator implements Validator<String> {
    private static final String CODE_PATTERN = " *[^: ][^:]*";
    private Pattern pattern;
    private String message;

    public TransactionCategoryValidator(boolean nestedCode) {
        pattern = nestedCode ? Pattern.compile("(" + CODE_PATTERN + "(:" + CODE_PATTERN + ")*)?") : Pattern.compile(CODE_PATTERN);
        message = LABELS.getString("validation.transactionCategory." + (nestedCode ? "invalidNestedCodes" : "invalidCode"));
    }

    public String validate(String value) {
        if (! pattern.matcher(value).matches()) {
            return message;
        }
        return null;
    }
}