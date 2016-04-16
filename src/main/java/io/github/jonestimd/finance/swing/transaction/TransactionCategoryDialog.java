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

import java.awt.Window;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.ValidatedTextField;

public class TransactionCategoryDialog extends FormDialog {
    private static final String RESOURCE_PREFIX = "dialog.transactionCategory.";
    private final ResourceBundle bundle = BundleType.LABELS.get();
    private TransactionCategory category;
    private JComboBox<TransactionCategory> parentComboBox;
    private JTextField codeField;
    private JRadioButton[] incomeExpenseGroup;
    private JTextArea descriptionField = new JTextArea(5, 50);

    public TransactionCategoryDialog(Window owner, TransactionCategory category, List<TransactionCategory> categories) {
        super(owner, null, BundleType.LABELS.get());
        this.category = category;
        parentComboBox = new BeanListComboBox<>(new CategoryKeyFormat(), categories);
        codeField = new ValidatedTextField(new TransactionCategoryValidator(false));
        codeField.setColumns(30);
        incomeExpenseGroup = ComponentFactory.newRadioButtonGroup(BundleType.LABELS.get(),
                RESOURCE_PREFIX + "income.mnemonicAndName", RESOURCE_PREFIX + "expense.mnemonicAndName");
        initializeComponents();
        buildForm(getFormPanel());
    }

    private void initializeComponents() {
        setTitle(bundle.getString(RESOURCE_PREFIX + (category.getId() == null ? "title.new" : "title.edit")));
        parentComboBox.setSelectedItem(category.getParent());
        codeField.setText(category.getCode());
        incomeExpenseGroup[category.isIncome() ? 0 : 1].setSelected(true);
        descriptionField.setText(category.getDescription());
    }

    private void buildForm(JPanel formPanel) {
        GridBagBuilder builder = new GridBagBuilder(formPanel, bundle, RESOURCE_PREFIX);
        builder.append("parent.mnemonicAndName", parentComboBox);
        builder.append("code.mnemonicAndName", codeField);
        builder.append("description.mnemonicAndName", descriptionField);
        builder.append(incomeExpenseGroup);
    }

    protected void onSave() {
        super.onSave();
        category.setParent((TransactionCategory) parentComboBox.getSelectedItem());
        category.setCode(codeField.getText());
        category.setIncome(incomeExpenseGroup[0].isSelected());
        category.setDescription(descriptionField.getText());
    }

    public TransactionCategory getCategory() {
        return category;
    }
}