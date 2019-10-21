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
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.text.Normalizer.Form;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.swing.transaction.AccountFormat;
import io.github.jonestimd.finance.swing.transaction.PayeeFormat;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.LabelBuilder;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.MultiSelectField;
import io.github.jonestimd.swing.component.TextField;
import io.github.jonestimd.swing.component.TextField.Validated;
import io.github.jonestimd.swing.layout.FormElement;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.ValidatedTextField;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ImportFilePanel extends JComponent {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";

    private static Validated requiredTextField(String resourcePrefix) {
        return TextField.validated(LABELS.get(), RESOURCE_PREFIX + resourcePrefix).required("required");
    }

    private MultiSelectField createLabelField() {
        return new MultiSelectField.Builder(false, true).disableTab().get();
    }

    private final ValidatedTextField nameField = requiredTextField("name.").get();
    private final BeanListComboBox<Account> accountField = new BeanListComboBox<>(new AccountFormat());
    private final JComboBox<ImportType> importTypeField = new JComboBox<>(ImportType.values());
    private final JComboBox<FileType> fileTypeField = new JComboBox<>(FileType.values());
    private final JTextField dateFormatField = requiredTextField("dateFormat.").get();
    private final ValidatedTextField startOffsetField = requiredTextField("startOffset.").configure().inputFilter("[0-9]*").get();
    private final JCheckBox reconcileCheckbox = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "reconcileMode");
    private final JCheckBox singlePayeeCheckbox = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "singlePayee");
    private final BeanListComboBox<Payee> payeeField = new BeanListComboBox<>(new PayeeFormat());
    private final MultiSelectField dateLabelField = createLabelField();
    private final MultiSelectField payeeLabelField = createLabelField();
    private final MultiSelectField securityLabelField = createLabelField();
    private final JLabel payeeLabel;

    public ImportFilePanel(List<Account> accounts, List<Payee> payees) {
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
        setBorder(new EmptyBorder(5, 5, 5, 5));
        singlePayeeCheckbox.getModel().addItemListener(event -> {
            if (singlePayeeCheckbox.isSelected()) replacePayeeField(payeeScrollPane, payeeField, "payee");
            else replacePayeeField(payeeField, payeeScrollPane, "payeeLabel");
            if (isVisible()) revalidate(); // TODO dialog.pack() fire event?
        });
    }

    private void replacePayeeField(JComponent oldComponent, JComponent newComponent, String labelKey) {
        oldComponent.setVisible(false);
        newComponent.setVisible(true);
        String label = LABELS.getString(RESOURCE_PREFIX + labelKey);
        payeeLabel.setText(label.substring(1));
    }

    public void setImportFile(ImportFile importFile) {
        if (importFile.getName() != null) {
            nameField.setText(importFile.getName());
            accountField.setSelectedItem(importFile.getAccount());
            importTypeField.setSelectedItem(importFile.getImportType());
            fileTypeField.setSelectedItem(importFile.getFileType());
            startOffsetField.setText(Integer.toString(importFile.getStartOffset()));
            dateFormatField.setText(importFile.getDateFormat());
            reconcileCheckbox.setSelected(importFile.isReconcile());
            setLabels(importFile.getFields(), FieldType.DATE, dateLabelField);
            setLabels(importFile.getFields(), FieldType.PAYEE, payeeLabelField);
            setLabels(importFile.getFields(), FieldType.SECURITY, securityLabelField);
            if (importFile.getPayee() != null) {
                singlePayeeCheckbox.setSelected(true);
                payeeField.setSelectedItem(importFile.getPayee());
            }
        }
    }

    private void setLabels(Collection<ImportField> fields, FieldType type, MultiSelectField input) {
        fields.stream().filter(field -> field.getType() == type).findFirst()
                .ifPresent(field -> input.setItems(field.getLabels()));
    }
}
