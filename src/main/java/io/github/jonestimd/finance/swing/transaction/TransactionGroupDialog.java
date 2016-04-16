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
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JTextField;

import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.ValidatedTextField;
import io.github.jonestimd.swing.validation.Validator;

import static io.github.jonestimd.util.JavaFunctions.*;

public class TransactionGroupDialog extends FormDialog {
    private static final String RESOURCE_PREFIX = "transactionGroup.dialog.";
    private static final TransactionGroupNameValidator<TransactionGroup> NAME_VALIDATOR =
            new TransactionGroupNameValidator<>(nullGuard(TransactionGroup::getName));
    private final ResourceBundle bundle = BundleType.LABELS.get();
    private TransactionGroup group;
    private JTextField nameField;
    private JTextField descriptionField;

    public TransactionGroupDialog(Window owner, final Iterable<TransactionGroup> groups) {
        super(owner, null, BundleType.LABELS.get());
        nameField = new ValidatedTextField(new Validator<String>() {
            public String validate(String value) {
                return NAME_VALIDATOR.validate(group, value, groups);
            }
        });
        nameField.setColumns(30);
        descriptionField = new JTextField();
        buildForm(getFormPanel());
    }

    private void initializeComponents() {
        setTitle(bundle.getString(RESOURCE_PREFIX + (group.getId() == null ? "title.new" : "title.edit")));
        nameField.setText(group.getName());
        descriptionField.setText(group.getDescription());
    }

    private void buildForm(JPanel formPanel) {
        GridBagBuilder builder = new GridBagBuilder(formPanel, bundle, RESOURCE_PREFIX);
        builder.append("name.mnemonicAndName", nameField);
        builder.append("description.mnemonicAndName", descriptionField);
    }

    public boolean show(TransactionGroup group) {
        this.group = group;
        initializeComponents();
        pack();
        setVisible(true);
        return ! isCancelled();
    }

    protected void onSave() {
        super.onSave();
        group.setName(nameField.getText());
        group.setDescription(descriptionField.getText());
    }

    public TransactionGroup getGroup() {
        return group;
    }
}
