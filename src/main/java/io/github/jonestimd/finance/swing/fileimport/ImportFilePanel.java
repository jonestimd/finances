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
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import io.github.jonestimd.collection.MapBuilder;
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
import io.github.jonestimd.swing.validation.ValidationTracker;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.ComponentUtils.*;
import static io.github.jonestimd.swing.ComponentFactory.*;
import static io.github.jonestimd.swing.component.ComponentBinder.*;

public class ImportFilePanel extends ValidatedTabPanel {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";

    private static Validated requiredTextField(String resourcePrefix) {
        return TextField.validated(LABELS.get(), RESOURCE_PREFIX + resourcePrefix).required("required");
    }

    private ImportFileModel model;
    private final ValidatedTextField nameField;
    private final BeanListComboBox<Account> accountField = new BeanListComboBox<>(new AccountFormat());
    private final JComboBox<ImportType> importTypeField = newComboBox(ImportType.class, LABELS.getString(RESOURCE_PREFIX + "importType.required"));
    private final JComboBox<FileType> fileTypeField = newComboBox(FileType.class, LABELS.getString(RESOURCE_PREFIX + "fileType.required"));
    private final JTextField dateFormatField = requiredTextField("dateFormat.").get();
    private final ValidatedTextField startOffsetField = requiredTextField("startOffset.").configure().inputFilter("[0-9]*").get();
    private final JCheckBox reconcileCheckbox = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "reconcileMode");
    private final JCheckBox singlePayeeCheckbox = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "singlePayee");
    private final BeanListComboBox<Payee> payeeField = new BeanListComboBox<>(new PayeeFormat());
    private final PropertyChangeListener onPropertyChange;
    private boolean isNoErrors = true;

    public ImportFilePanel(FileImportsDialog owner, List<Account> accounts, List<Payee> payees) {
        nameField = new ValidatedTextField(owner.getModel()::validateName);
        accountField.getModel().setElements(accounts, false);
        payeeField.getModel().setElements(payees, false);
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
        builder.append("payee", payeeField);
        builder.overlay(payeeField);
        builder.append(singlePayeeCheckbox);
        builder.append(reconcileCheckbox);
        setBorder(BorderFactory.panelBorder());
        singlePayeeCheckbox.getModel().addItemListener(event -> {
            if (model != null) model.setSinglePayee(singlePayeeCheckbox.isSelected());
            payeeField.setEnabled(singlePayeeCheckbox.isSelected());
        });
        bindToModel(owner.getModel());
        final Map<String, JComponent> propertyLabelMap = new MapBuilder<String, JComponent>()
                .put("changedName", getLabel(nameField))
                .put("changedImportType", getLabel(importTypeField))
                .put("changedFileType", getLabel(fileTypeField))
                .put("changedAccount", getLabel(accountField))
                .put("changedStartOffset", getLabel(startOffsetField))
                .put("changedDateFormat", getLabel(dateFormatField))
                .put("changedPayee", getLabel(payeeField))
                .put("changedReconcile", reconcileCheckbox)
                .get();
        onPropertyChange = (event) -> {
            Color labelColor = getLabelColor(event.getNewValue() == Boolean.TRUE);
            JComponent component = propertyLabelMap.get(event.getPropertyName());
            if (component != null) component.setForeground(labelColor);
        };
        setImportFile(owner.getModel().getSelectedItem());
        ValidationTracker.install(this::setValidationMessages, this);
    }

    private void setValidationMessages(Collection<String> messages) {
        isNoErrors = messages.isEmpty();
        setTabForeground();
    }

    private void bindToModel(FileImportsModel fileImportsModel) {
        bind(nameField, nullSafeConsumer(name -> model.setName(name)));
        importTypeField.addItemListener(event -> {
            if (model != null && event.getStateChange() == ItemEvent.SELECTED) model.setImportType((ImportType) event.getItem());
        });
        fileTypeField.addItemListener(nullSafeListener(event -> model.setFileType((FileType) event.getItem())));
        accountField.addItemListener(nullSafeListener(event -> model.setAccount((Account) event.getItem())));
        bind(startOffsetField, this::parseOffset, nullSafeConsumer(offset -> model.setStartOffset(offset)));
        bind(dateFormatField, nullSafeConsumer(format -> model.setDateFormat(format)));
        reconcileCheckbox.addItemListener(nullSafeListener(event -> model.setReconcile(event.getStateChange() == ItemEvent.SELECTED)));
        payeeField.addItemListener(nullSafeListener(event -> model.setPayee((Payee) event.getItem())));
        singlePayeeCheckbox.addItemListener(event -> {
            if (model != null) {
                if (event.getStateChange() == ItemEvent.DESELECTED) model.setPayee(null);
                else model.setPayee(payeeField.getSelectedItem());
            }
        });
        fileImportsModel.addSelectionListener((oldFile, newFile) -> setImportFile(newFile));
    }

    private <T> Consumer<T> nullSafeConsumer(Consumer<T> listener) {
        return (value) -> {
            if (model != null) listener.accept(value);
        };
    }

    private ItemListener nullSafeListener(ItemListener listener) {
        return (event) -> {
            if (model != null) listener.itemStateChanged(event);
        };
    }

    private Color getLabelColor(boolean changed) {
        return changed ? Color.BLUE : Color.BLACK;
    }

    private Integer parseOffset(String offset) {
        try {
            return Integer.parseInt(offset);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public void setImportFile(ImportFileModel model) {
        if (this.model != null) this.model.removePropertyChangeListener(this.onPropertyChange);
        this.model = model;
        if (model != null) {
            model.addPropertyChangeListener(this.onPropertyChange);
            setFieldsEnabled(true);
            nameField.setText(model.getName());
            importTypeField.setSelectedItem(model.getImportType());
            fileTypeField.setSelectedItem(model.getFileType());
            accountField.setSelectedItem(model.getAccount());
            startOffsetField.setText(model.getStartOffset() == null ? "" : model.getStartOffset().toString());
            dateFormatField.setText(model.getDateFormat());
            reconcileCheckbox.setSelected(model.isReconcile());
            payeeField.setSelectedItem(model.getPayee());
            singlePayeeCheckbox.setSelected(model.getPayee() != null); // set after payeeField so binding won't clear model's payee
            if (model.getId() == null) nameField.requestFocusInWindow();
        }
        else {
            setFieldsEnabled(false);
            nameField.setText("");
            importTypeField.setSelectedItem(null);
            fileTypeField.setSelectedItem(null);
            accountField.setSelectedItem(null);
            startOffsetField.setText("");
            dateFormatField.setText("");
            reconcileCheckbox.setSelected(false);
            payeeField.setSelectedItem(null);
            singlePayeeCheckbox.setSelected(false);
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        nameField.setEditable(enabled);
        importTypeField.setEnabled(enabled);
        fileTypeField.setEnabled(enabled);
        accountField.setEnabled(enabled);
        startOffsetField.setEditable(enabled);
        dateFormatField.setEditable(enabled);
        reconcileCheckbox.setEnabled(enabled);
        payeeField.setEnabled(enabled && singlePayeeCheckbox.isSelected());
        singlePayeeCheckbox.setEnabled(enabled);
    }

    @Override
    protected boolean isNoErrors() {
        return isNoErrors;
    }
}
