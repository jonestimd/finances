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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.laf.LookAndFeelConfig;
import io.github.jonestimd.finance.swing.transaction.AccountFormat;
import io.github.jonestimd.finance.swing.transaction.PayeeFormat;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.LabelBuilder;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.PopupListField;
import io.github.jonestimd.swing.component.TextField;
import io.github.jonestimd.swing.component.TextField.Validated;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.validation.ValidatedTextField;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportDialog extends FormDialog {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";

    private static Validated requiredTextField(String resourcePrefix) {
        return TextField.validated(LABELS.get(), RESOURCE_PREFIX + resourcePrefix).required("required");
    }

    private final ValidatedTextField nameField = requiredTextField("name.").get();
    private final BeanListComboBox<Account> accountField = new BeanListComboBox<>(new AccountFormat(), LABELS.getString(RESOURCE_PREFIX + "account.required"));
    private final JComboBox<ImportType> importTypeField = new JComboBox<>(ImportType.values());
    private final JComboBox<FileType> fileTypeField = new JComboBox<>(FileType.values());
    private final JTextField dateFormatField = requiredTextField("dateFormat.").get();
    private final ValidatedTextField startOffsetField = requiredTextField("startOffset.").configure().inputFilter("[0-9]*").get();
    private final JCheckBox reconcileButton = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "reconcileMode");
    private final JCheckBox singlePayeeButton = ComponentFactory.newCheckBox(LABELS.get(), RESOURCE_PREFIX + "singlePayee");
    private final BeanListComboBox<Payee> payeeField = new BeanListComboBox<>(new PayeeFormat(), LABELS.getString(RESOURCE_PREFIX + "payee.required"));
    private final Map<FieldType, PopupListField> labelPanels = new MapBuilder<FieldType, PopupListField>()
            .put(FieldType.DATE, createLabelField())
            .put(FieldType.PAYEE, createLabelField())
            .put(FieldType.SECURITY, createLabelField()).get();
    private final ImportFieldTableModel fieldTableModel = new ImportFieldTableModel();
    private final DecoratedTable<ImportField, ImportFieldTableModel> fieldTable = new DecoratedTable<>(fieldTableModel); // TODO use table factory
    private final JLabel payeeLabel = new LabelBuilder().mnemonicAndName(LABELS.getString(RESOURCE_PREFIX + "payeeLabel"))
            .forComponent(labelPanels.get(FieldType.PAYEE)).get();

    public FileImportDialog(Window owner, List<Account> accounts, List<Payee> payees) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        accountField.getModel().setElements(accounts, false);
        payeeField.getModel().setElements(payees, false);
        nameField.setColumns(15);
        payeeField.setVisible(false);
        JPanel form = FormLayout.builder(BundleType.LABELS.get(), RESOURCE_PREFIX, new JPanel(), 3)
                .add("name", nameField)
                .add("importType", importTypeField)
                .add("fileType", fileTypeField)
                .add("account", accountField)
                .add("dateFormat", dateFormatField)
                .add("startOffset", startOffsetField)
                .add(payeeLabel, labelPanels.get(FieldType.PAYEE)).previousCell().add(payeeField, false)
                .add("dateLabel", labelPanels.get(FieldType.DATE))
                .add("securityLabel", labelPanels.get(FieldType.SECURITY))
                .nextCell().add(singlePayeeButton, true)
                .nextCell().add(reconcileButton, true).container();
        getFormPanel().setLayout(new BorderLayout(0, 10));
        getFormPanel().add(form, BorderLayout.NORTH);
        getFormPanel().add(createScrollPane(fieldTable, 200, 200), BorderLayout.CENTER);
        singlePayeeButton.getModel().addItemListener(this::onSinglePayeeChange);
        // TODO mappings
        // TODO filter regex's, negate amount, memo
    }

    private static JScrollPane createScrollPane(JComponent component, int width, int height) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setPreferredSize(new Dimension(width, height));
        return scrollPane;
    }

    private static PopupListField createLabelField() {
        PopupListField field = PopupListField.builder(true, true).build();
        field.setBorder(new CompoundBorder(new LineBorder(Color.GRAY), new EmptyBorder(1, 1, 1, 1)));
        return field;
    }

    private void onSinglePayeeChange(ItemEvent itemEvent) {
        if (singlePayeeButton.isSelected()) replacePayeeField(labelPanels.get(FieldType.PAYEE), payeeField, "payee");
        else replacePayeeField(payeeField, labelPanels.get(FieldType.PAYEE), "payeeLabel");
        if (isVisible()) {
            revalidate();
            pack();
        }
    }

    private void replacePayeeField(JComponent oldComponent, JComponent newComponent, String labelKey) {
        oldComponent.setVisible(false);
        newComponent.setVisible(true);
        String label = LABELS.getString(RESOURCE_PREFIX + labelKey);
        payeeLabel.setText(label.substring(1));
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
            importFile.getFields().stream()
                    .filter(field -> labelPanels.containsKey(field.getType()))
                    .forEach(field -> labelPanels.get(field.getType()).setItems(field.getLabels()));
            fieldTableModel.setBeans(Streams.filter(importFile.getFields(), field -> ! field.getType().isTransaction()));
            if (importFile.getPayee() != null) {
                singlePayeeButton.setSelected(true);
                payeeField.setSelectedItem(importFile.getPayee());
            }
        }
        pack();
        setVisible(true);
        return false; // TODO check for valid and changed
    }

    public static void main(String... args) {
        LookAndFeelConfig.load();
        new FileImportDialog(JOptionPane.getRootFrame(), new ArrayList<>(), new ArrayList<>()).show(new ImportFile());
    }
}
