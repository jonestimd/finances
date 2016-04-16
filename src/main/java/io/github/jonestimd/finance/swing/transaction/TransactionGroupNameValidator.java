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

import java.util.function.Function;

import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.validation.UniqueValueValidator;

public class TransactionGroupNameValidator<T> extends UniqueValueValidator<T> {
    private static final String REQUIRED_MESSAGE = "validation.transactionGroup.nameRequired";
    private static final String UNIQUE_MESSAGE = "validation.transactionGroup.nameUnique";

    public TransactionGroupNameValidator(Function<T, String> adapter) {
        super(adapter, BundleType.LABELS.getString(REQUIRED_MESSAGE), BundleType.LABELS.getString(UNIQUE_MESSAGE));
    }
}