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

import javax.swing.event.TableModelEvent;

import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.swing.validation.BeanPropertyValidator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class StockSplitTableModel extends ValidatedBeanListTableModel<StockSplit> {
    public static final int DATE_INDEX = 0;
    private static final StockSplitColumnAdapter<Date> SPLIT_DATE = new StockSplitColumnAdapter<>(
            "date", Date.class, StockSplit::getDate, StockSplit::setDate, new SplitDateValidator());
    private static final StockSplitColumnAdapter<BigDecimal> SHARES_IN = new StockSplitColumnAdapter<>(
            "sharesIn", BigDecimal.class, StockSplit::getSharesIn, StockSplit::setSharesIn, new SplitRatioValidator("sharesIn"));
    private static final StockSplitColumnAdapter<BigDecimal> SHARES_OUT = new StockSplitColumnAdapter<>(
            "sharesOut", BigDecimal.class, StockSplit::getSharesOut, StockSplit::setSharesOut, new SplitRatioValidator("sharesOut"));

    @SuppressWarnings("unchecked")
    public StockSplitTableModel(List<StockSplit> splits) {
        super(Arrays.asList(SPLIT_DATE, SHARES_IN, SHARES_OUT));
        setBeans(splits);
    }

    @Override
    public void fireTableCellUpdated(int rowIndex, int columnIndex) {
        super.fireTableCellUpdated(rowIndex, columnIndex);
        if (columnIndex == DATE_INDEX) validateDates();
        else {
            validateCell(rowIndex, getColumnCount()-columnIndex);
            super.fireTableCellUpdated(rowIndex, getColumnCount()-columnIndex);
        }
    }

    @Override
    public void addRow(int index, StockSplit row) {
        super.addRow(index, row);
        validateDates();
    }

    @Override
    public void removeRow(StockSplit bean) {
        super.removeRow(bean);
        validateDates();
    }

    private void validateDates() {
        for (int i = 0; i < getRowCount(); i++) {
            validateCell(i, DATE_INDEX);
        }
        super.fireTableChanged(new TableModelEvent(this, 0, Integer.MAX_VALUE, DATE_INDEX));
    }

    protected static class StockSplitColumnAdapter<V> extends FunctionColumnAdapter<StockSplit, V> implements BeanPropertyValidator<StockSplit, V> {
        private final BeanPropertyValidator<StockSplit, V> validator;

        protected StockSplitColumnAdapter(String columnId, Class<V> type, Function<StockSplit, V> getter,
                BiConsumer<StockSplit, V> setter, BeanPropertyValidator<StockSplit, V> validator) {
            super(LABELS.get(), "table.stockSplit.", columnId, type, getter, setter);
            this.validator = validator;
        }

        @Override
        public String validate(int selectedIndex, V propertyValue, List<? extends StockSplit> beans) {
            return validator.validate(selectedIndex, propertyValue, beans);
        }
    }
}
