// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.swing.laf.LookAndFeelConfig;
import io.github.jonestimd.finance.swing.transaction.AccountFormat;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.TextField;
import io.github.jonestimd.swing.component.TextField.Validated;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.FormElement;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.layout.GridBagFormula;
import io.github.jonestimd.swing.validation.ValidatedTextField;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportDialog extends FormDialog {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    private static final GridBagFormula LABELS_LABEL_CONSTRAINTS = GridBagFormula.get(null, GridBagConstraints.NORTHEAST, 1, 1, 0, 0, new Insets(4, 0, 0, 0));
    private static final GridBagFormula LABELS_CONSTRAINTS = GridBagFormula.get(LABELS_LABEL_CONSTRAINTS, GridBagConstraints.NORTH, 1, 1, 1, 0, 0);
    private static Validated requiredTextField(String resourcePrefix) {
        return TextField.validated(LABELS.get(), RESOURCE_PREFIX + resourcePrefix).required("required");
    }

    private final ValidatedTextField nameField = requiredTextField("name.").get();
    private final BeanListComboBox<Account> accountField;
    private final JComboBox<ImportType> importTypeField = new JComboBox<>(ImportType.values());
    private final JComboBox<FileType> fileTypeField = new JComboBox<>(FileType.values());
    private final JTextField dateFormatField = requiredTextField("dateFormat.").get();
    private final ValidatedTextField startOffsetField = requiredTextField("startOffset.").configure().inputFilter("[0-9]*").get();
    private final JCheckBox reconcileButton = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "reconcileMode");
    private final JComboBox<AmountFormat> amountFormatField = new JComboBox<>(AmountFormat.values());
    private final JComboBox<AmountFormat> sharesFormatField = new JComboBox<>(AmountFormat.values());
    private final Map<FieldType, LabelsPanel> labelPanels = new MapBuilder<FieldType, LabelsPanel>()
            .put(FieldType.DATE, new LabelsPanel(LABELS.getString(RESOURCE_PREFIX + "dateLabel.required")))
            .put(FieldType.AMOUNT, new LabelsPanel(LABELS.getString(RESOURCE_PREFIX + "amountLabel.required")))
            .put(FieldType.CATEGORY, new LabelsPanel())
            .put(FieldType.TRANSFER_ACCOUNT, new LabelsPanel())
            .put(FieldType.PAYEE, new LabelsPanel())
            .put(FieldType.SECURITY, new LabelsPanel())
            .put(FieldType.ASSET_QUANTITY, new LabelsPanel()).get();

    public FileImportDialog(Window owner, List<Account> accounts) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        GridBagBuilder builder = new GridBagBuilder(getFormPanel(), LABELS.get(), RESOURCE_PREFIX).setConstraints(LabelsPanel.class, LABELS_CONSTRAINTS);
        builder.append("name", nameField);
        accountField = builder.append("account", new BeanListComboBox<>(new AccountFormat(), LABELS.getString(RESOURCE_PREFIX + "account.required")));
        accountField.getModel().setElements(accounts, false);
        builder.append("importType", importTypeField);
        builder.append("fileType", fileTypeField);
        builder.append("dateFormat", dateFormatField);
        builder.append("dateLabel", labelPanels.get(FieldType.DATE));
        builder.append("payeeLabel", labelPanels.get(FieldType.PAYEE));
        builder.append("securityLabel", labelPanels.get(FieldType.SECURITY));
        builder.append("startOffset", startOffsetField);
        builder.append(reconcileButton);
        // TODO mappings
        // TODO filter regex's, negate amount, memo
        builder.append(new JSeparator(JSeparator.HORIZONTAL), FormElement.PANEL);
        builder.append("amountFormat", amountFormatField);
        builder.append("amountLabel", labelPanels.get(FieldType.AMOUNT));
        builder.append("categoryLabels", labelPanels.get(FieldType.CATEGORY));
        builder.append("transferLabel", labelPanels.get(FieldType.TRANSFER_ACCOUNT));
        builder.append("sharesFormat", sharesFormatField);
        builder.append("sharesLabel", labelPanels.get(FieldType.ASSET_QUANTITY));
    }

    public boolean show(ImportFile importFile) {
        // TODO update title
        if (importFile.getName() != null) {
            nameField.setText(importFile.getName());
            accountField.setSelectedItem(importFile.getAccount());
            importTypeField.setSelectedItem(importFile.getImportType());
            fileTypeField.setSelectedItem(importFile.getFileType());
            startOffsetField.setText(Integer.toString(importFile.getStartOffset()));
            dateFormatField.setText(importFile.getDateFormat());
            reconcileButton.setSelected(importFile.isReconcile());
            importFile.getFields().forEach(field -> {
                labelPanels.get(field.getType()).setLabels(field.getLabels());
                if (field.getType() == FieldType.AMOUNT) {
                    amountFormatField.setSelectedItem(field.getAmountFormat());
                }
                else if (field.getType() == FieldType.ASSET_QUANTITY) {
                    sharesFormatField.setSelectedItem(field.getAmountFormat());
                }
            });
        }
        pack();
        setVisible(true);
        return false; // TODO check for valid and changed
    }

    public static void main(String... args) {
        LookAndFeelConfig.load();
        new FileImportDialog(JOptionPane.getRootFrame(), new ArrayList<>()).show(new ImportFile());
    }}
