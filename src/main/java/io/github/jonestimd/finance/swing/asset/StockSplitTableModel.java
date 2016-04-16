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
package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;
import io.github.jonestimd.swing.validation.NotNullValidator;
import io.github.jonestimd.swing.validation.Validator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class StockSplitTableModel extends ValidatedBeanListTableModel<StockSplit> {
    public static final int DATE_INDEX = 0;
    private static final StockSplitColumnAdapter<Date> SPLIT_DATE = new StockSplitColumnAdapter<>("date", Date.class,
            StockSplit::getDate,
            StockSplit::setDate,
            new NotNullValidator<>(LABELS.getString("validation.stockSplit.date.required")));
    private static final StockSplitColumnAdapter<BigDecimal> SHARES_IN = new StockSplitColumnAdapter<>("sharesIn", BigDecimal.class,
            StockSplit::getSharesIn,
            StockSplit::setSharesIn,
            new NotNullValidator<>(LABELS.getString("validation.stockSplit.sharesIn.required")));
    private static final StockSplitColumnAdapter<BigDecimal> SHARES_OUT = new StockSplitColumnAdapter<>("sharesOut", BigDecimal.class,
            StockSplit::getSharesOut,
            StockSplit::setSharesOut,
            new NotNullValidator<>(LABELS.getString("validation.stockSplit.sharesOut.required")));

    @SuppressWarnings("unchecked")
    public StockSplitTableModel(List<StockSplit> splits) {
        super(Arrays.asList(SPLIT_DATE, SHARES_IN, SHARES_OUT));
        setBeans(splits);
    }

    protected static class StockSplitColumnAdapter<V> extends FunctionColumnAdapter<StockSplit, V> implements BeanPropertyValidator<StockSplit, V> {
        private final Validator<V> validator;

        protected StockSplitColumnAdapter(String columnId, Class<V> type, Function<StockSplit, V> getter, BiConsumer<StockSplit, V> setter, Validator<V> validator) {
            super(LABELS.get(), "table.stockSplit.", columnId, type, getter, setter);
            this.validator = validator;
        }

        @Override
        public String validate(int selectedIndex, V propertyValue, List<? extends StockSplit> beans) {
            return validator.validate(propertyValue);
        }
    }
}
