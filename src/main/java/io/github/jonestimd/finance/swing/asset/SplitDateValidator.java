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
package io.github.jonestimd.finance.swing.asset;

import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;
import io.github.jonestimd.util.Streams;

public class SplitDateValidator implements BeanPropertyValidator<StockSplit, Date> {
    protected static final String REQUIRED_MESSAGE = "validation.stockSplit.date.required";
    protected static final String UNIQUE_MESSAGE = "validation.stockSplit.date.unique";
    protected final String requireMessage = BundleType.LABELS.getString(REQUIRED_MESSAGE);
    protected final String uniqueMessage = BundleType.LABELS.getString(UNIQUE_MESSAGE);

    @Override
    public String validate(int selectedIndex, Date splitDate, List<? extends StockSplit> beans) {
        if (splitDate == null) return requireMessage;
        List<Date> dates = Streams.map(beans, StockSplit::getDate);
        dates.remove(selectedIndex);
        if (dates.contains(splitDate)) return uniqueMessage;
        return null;
    }
}