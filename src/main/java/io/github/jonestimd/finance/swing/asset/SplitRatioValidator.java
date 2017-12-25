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

import java.math.BigDecimal;
import java.util.List;

import io.github.jonestimd.collection.BigDecimals;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class SplitRatioValidator implements BeanPropertyValidator<StockSplit, BigDecimal> {
    protected final String requiredMessage;
    protected final String invalidSplitMessage = LABELS.getString("validation.stockSplit.ratio.invalid");

    public SplitRatioValidator(String columnId) {
        this.requiredMessage = LABELS.getString("validation.stockSplit." + columnId + ".required");
    }

    @Override
    public String validate(int selectedIndex, BigDecimal propertyValue, List<? extends StockSplit> beans) {
        if (propertyValue == null) return requiredMessage;
        StockSplit split = beans.get(selectedIndex);
        if (BigDecimals.isEqual(split.getSharesIn(), split.getSharesOut())) return invalidSplitMessage;
        return null;
    }
}
