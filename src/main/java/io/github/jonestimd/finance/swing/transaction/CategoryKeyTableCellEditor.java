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

import java.util.Collections;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTextField;

import io.github.jonestimd.finance.domain.transaction.CategoryKey;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.table.CompositeCellEditor;
import io.github.jonestimd.swing.table.model.BeanListTableModel;
import io.github.jonestimd.util.Streams;

public class CategoryKeyTableCellEditor extends CompositeCellEditor<CategoryKey> {
    private BeanListComboBox<TransactionCategory> parentList = new BeanListComboBox<>(new CategoryFormat());
    private JTextField codeField = new JTextField();

    public CategoryKeyTableCellEditor() {
        super(CategoryKey::clone);
        addFields(parentList, codeField);
    }

    @Override
    protected int getInitialFocus() {
        return 0;
    }

    @SuppressWarnings("unchecked")
    protected void prepareEditor(JTable table, CategoryKey value, boolean isSelected, int row, int column) {
        BeanListTableModel<TransactionCategorySummary> tableModel = (BeanListTableModel<TransactionCategorySummary>) table.getModel();
        List<TransactionCategory> categories = Streams.map(tableModel.getBeans(), TransactionCategorySummary::getCategory);
        categories.remove(table.convertRowIndexToModel(row));
        Collections.sort(categories);
        categories.add(0, null);
        parentList.getModel().setElements(categories);

        int selectedIndex = categories.indexOf(value.getParent());
        parentList.setSelectedIndex(selectedIndex);
        codeField.setText(value.getCode());
    }

    @Override
    protected void updateCellEditorValue(CategoryKey bean) {
        bean.setParent(parentList.getSelectedItem());
        bean.setCode(codeField.getText());
    }
}