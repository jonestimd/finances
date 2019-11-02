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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.LabelBuilder;
import io.github.jonestimd.swing.action.CancelAction;
import io.github.jonestimd.swing.action.LocalizedAction;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.table.TableFactory;
import io.github.jonestimd.swing.validation.Validator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportDialog extends FormDialog {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    private final Format importFileFormat = FormatFactory.format(ImportFile::getName);
    private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    private final ImportFilePanel filePanel;
    private final ImportFieldsPanel fieldsPanel = new ImportFieldsPanel();
    private final PageRegionsPanel regionsPanel;
    private final BeanListComboBox<ImportFile> importListField;
    private final Action applyAction = LocalizedAction.create(LABELS.get(), RESOURCE_PREFIX + "apply", this::applyChanges);
    private final Action newAction = LocalizedAction.create(LABELS.get(), RESOURCE_PREFIX + "menu.imports.new", this::newImport);
    private final Action deleteAction = LocalizedAction.create(LABELS.get(), RESOURCE_PREFIX + "menu.imports.delete", this::deleteImport);
    private final Action duplicateAction = LocalizedAction.create(LABELS.get(), RESOURCE_PREFIX + "menu.imports.duplicate", this::duplicateImport);

    public FileImportDialog(Window owner, List<Account> accounts, List<Payee> payees, List<ImportFile> importFiles, TableFactory tableFactory) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        buttonBar.add(new JButton(applyAction));
        importListField = new BeanListComboBox<>(importFileFormat, Validator.empty(), importFiles);
        filePanel = new ImportFilePanel(accounts, payees);
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.file"), filePanel);
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.fields"), fieldsPanel);
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.pageRegions"), regionsPanel = new PageRegionsPanel(tableFactory));
        getFormPanel().setLayout(new BorderLayout(0, 10));
        getFormPanel().add(tabbedPane, BorderLayout.CENTER);
        Box listPanel = Box.createHorizontalBox();
        listPanel.add(new LabelBuilder().mnemonicAndName(LABELS.getString(RESOURCE_PREFIX + "name")).forComponent(importListField).get());
        listPanel.add(Box.createHorizontalStrut(5));
        listPanel.add(importListField);
        getFormPanel().add(listPanel, BorderLayout.NORTH);
        importListField.addItemListener(e -> {
            ImportFile importFile = importListField.getSelectedItem();
            filePanel.setImportFile(importFile);
            fieldsPanel.setImportFile(importFile);
            regionsPanel.setImportFile(importFile);
            tabbedPane.setEnabledAt(2, importFile.getFileType() == FileType.PDF);
        });
        if (!importFiles.isEmpty()) importListField.setSelectedIndex(0);
        // TODO disable panels when no import is selected
        // TODO mappings
        // TODO filter regex's, negate amount, memo
        createMenuBar();
        getRootPane().getActionMap().remove(CancelAction.ACTION_MAP_KEY);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu editMenu = ComponentFactory.newMenu(LABELS.get(), RESOURCE_PREFIX + "menu.imports.mnemonicAndName");
        editMenu.add(new JMenuItem(newAction));
        editMenu.add(new JMenuItem(deleteAction));
        editMenu.add(new JMenuItem(duplicateAction));
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
    }

    private void addTab(String mnemonicAndName, JComponent panel) {
        tabbedPane.add(mnemonicAndName.substring(1), panel);
        tabbedPane.setMnemonicAt(tabbedPane.getComponentCount()-1, mnemonicAndName.charAt(0));
    }

    public boolean showDialog() {
        pack();
        setVisible(true);
        return false; // TODO check for valid and changed
    }

    @Override
    protected void addValidationListener(Consumer<Collection<String>> listener) {
        super.addValidationListener(listener);
        // TODO
    }

    private void newImport(ActionEvent event) {
        // TODO
    }

    private void duplicateImport(ActionEvent event) {
        // TODO
    }

    private void deleteImport(ActionEvent event) {
        // TODO
    }

    private void applyChanges(ActionEvent event) {
        // TODO
    }
}
