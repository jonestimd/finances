// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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
package io.github.jonestimd.finance.swing.fileimport;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.swing.BorderFactory;
import io.github.jonestimd.finance.swing.transaction.AccountFormat;
import io.github.jonestimd.finance.swing.transaction.PayeeFormat;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.MultiSelectField;
import io.github.jonestimd.swing.component.TextField;
import io.github.jonestimd.swing.component.TextField.Validated;
import io.github.jonestimd.swing.layout.FormElement;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.ValidatedTextField;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.swing.ComponentFactory.*;
import static io.github.jonestimd.swing.component.ComponentBinder.*;

public class ImportFilePanel extends JComponent {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";

    private static Validated requiredTextField(String resourcePrefix) {
        return TextField.validated(LABELS.get(), RESOURCE_PREFIX + resourcePrefix).required("required");
    }

    private MultiSelectField createLabelField() {
        return new MultiSelectField.Builder(false, true).disableTab().get();
    }

    private final ValidatedTextField nameField;
    private final BeanListComboBox<Account> accountField = new BeanListComboBox<>(new AccountFormat());
    private final JComboBox<ImportType> importTypeField = newComboBox(ImportType.class, LABELS.getString(RESOURCE_PREFIX + "importType.required"));
    private final JComboBox<FileType> fileTypeField = newComboBox(FileType.class, LABELS.getString(RESOURCE_PREFIX + "fileType.required"));
    private final JTextField dateFormatField = requiredTextField("dateFormat.").get();
    private final ValidatedTextField startOffsetField = requiredTextField("startOffset.").configure().inputFilter("[0-9]*").get();
    private final JCheckBox reconcileCheckbox = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "reconcileMode");
    private final JCheckBox singlePayeeCheckbox = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "singlePayee");
    private final BeanListComboBox<Payee> payeeField = new BeanListComboBox<>(new PayeeFormat());
    private final MultiSelectField dateLabelField = createLabelField();
    private final MultiSelectField payeeLabelField = createLabelField();
    private final MultiSelectField securityLabelField = createLabelField();
    private final JLabel payeeLabel;

    public ImportFilePanel(FileImportsDialog owner, List<Account> accounts, List<Payee> payees) {
        nameField = new ValidatedTextField(owner.getModel()::validateName);
        accountField.getModel().setElements(accounts, false);
        payeeField.getModel().setElements(payees, false);
        payeeField.setVisible(false);
        GridBagBuilder builder = new GridBagBuilder(this, LABELS.get(), RESOURCE_PREFIX)
                .useScrollPane(MultiSelectField.class)
                .setConstraints(MultiSelectField.class, FormElement.TEXT_FIELD);
        builder.append("name", nameField);
        builder.append("importType", importTypeField);
        builder.append("fileType", fileTypeField);
        builder.append("account", accountField);
        builder.append("startOffset", startOffsetField);
        builder.append("dateFormat", dateFormatField);
        builder.append(new JSeparator(), FormElement.TOP_LABEL);
        builder.append("dateLabel", dateLabelField);
        builder.append("securityLabel", securityLabelField);
        JScrollPane payeeScrollPane = builder.append("payeeLabel", new JScrollPane(payeeLabelField), FormElement.TEXT_FIELD);
        payeeField.setPreferredSize(payeeScrollPane.getPreferredSize());
        this.payeeLabel = builder.getLastLabel();
        builder.overlay(payeeField);
        builder.append(singlePayeeCheckbox);
        builder.append(reconcileCheckbox);
        setBorder(BorderFactory.panelBorder());
        singlePayeeCheckbox.getModel().addItemListener(event -> {
            if (singlePayeeCheckbox.isSelected()) replacePayeeField(payeeScrollPane, payeeField, "payee");
            else replacePayeeField(payeeField, payeeScrollPane, "payeeLabel");
            if (isVisible()) revalidate();
        });
        bindToModel(owner.getModel());
        setImportFile(owner.getModel());
    }

    private void bindToModel(FileImportsModel model) {
        bind(nameField, model::setImportName);
        importTypeField.addItemListener(event -> model.setImportType((ImportType) event.getItem()));
        fileTypeField.addItemListener(event -> model.setFileType((FileType) event.getItem()));
        accountField.addItemListener(event -> model.setAccount((Account) event.getItem()));
        bind(startOffsetField, this::parseOffset, model::setStartOffset);
        bind(dateFormatField, model::setDateFormat);
        reconcileCheckbox.addItemListener(event -> model.setReconcile(event.getStateChange() == ItemEvent.SELECTED));
        payeeField.addItemListener(event -> model.setPayee((Payee) event.getItem()));
        singlePayeeCheckbox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.DESELECTED) model.setPayee(null);
            else model.setPayee(payeeField.getSelectedItem());
        });
        bindLabelField(dateLabelField, model::setDateLabels);
        bindLabelField(payeeLabelField, model::setPayeeLabels);
        bindLabelField(securityLabelField, model::setSecurityLabels);
        model.addSelectionListener((m, importFile) -> setImportFile(model));
        model.addPropertyChangeListener((event) -> {
            Color labelColor = getLabelColor(event.getNewValue() == Boolean.TRUE);
            switch (event.getPropertyName()) {
                case "nameChanged":
                    setLabelColor(nameField, labelColor); break;
                case "importTypeChanged":
                    setLabelColor(importTypeField, labelColor); break;
                case "fileTypeChanged":
                    setLabelColor(fileTypeField, labelColor); break;
                case "accountChanged":
                    setLabelColor(accountField, labelColor); break;
                case "startOffsetChanged":
                    setLabelColor(startOffsetField, labelColor); break;
                case "dateFormatChanged":
                    setLabelColor(dateFormatField, labelColor); break;
                case "payeeChanged":
                case "payeeLabelsChanged":
                    payeeLabel.setForeground(labelColor); break;
                case "reconcileChanged":
                    reconcileCheckbox.setForeground(labelColor); break;
                case "dateLabelsChanged":
                    setLabelColor(dateLabelField, labelColor); break;
                case "securityLabelsChanged":
                    setLabelColor(securityLabelField, labelColor); break;
            }
        });
    }

    private Color getLabelColor(boolean changed) {
        return changed ? Color.BLUE : Color.BLACK;
    }

    private void setLabelColor(JComponent component, Color labelColor) {
        JLabel label = (JLabel) component.getClientProperty("labeledBy");
        label.setForeground(labelColor);
    }

    private void bindLabelField(MultiSelectField field, Consumer<List<String>> setter) {
        field.addPropertyChangeListener(MultiSelectField.ITEMS_PROPERTY, (event) -> setter.accept(field.getItems()));
    }

    private Integer parseOffset(String offset) {
        try {
            return Integer.parseInt(offset);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void replacePayeeField(JComponent oldComponent, JComponent newComponent, String labelKey) {
        oldComponent.setVisible(false);
        newComponent.setVisible(true);
        String label = LABELS.getString(RESOURCE_PREFIX + labelKey);
        payeeLabel.setText(label.substring(1));
    }

    public void setImportFile(FileImportsModel model) {
        nameField.setText(model.getImportName());
        importTypeField.setSelectedItem(model.getImportType());
        fileTypeField.setSelectedItem(model.getFileType());
        accountField.setSelectedItem(model.getAccount());
        startOffsetField.setText(Integer.toString(model.getStartOffset()));
        dateFormatField.setText(model.getDateFormat());
        reconcileCheckbox.setSelected(model.isReconcile());
        payeeField.setSelectedItem(model.getPayee());
        singlePayeeCheckbox.setSelected(model.getPayee() != null); // set after payeeField so binding won't clear model's payee
        dateLabelField.setItems(model.getDateLabels());
        payeeLabelField.setItems(model.getPayeeLabels());
        securityLabelField.setItems(model.getSecurityLabels());
        if (model.getSelectedItem().getId() == null) nameField.requestFocusInWindow();
    }
}
