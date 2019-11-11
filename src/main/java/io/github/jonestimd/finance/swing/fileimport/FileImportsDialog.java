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
import java.util.List;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;

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
import io.github.jonestimd.swing.dialog.ValidatedDialog;
import io.github.jonestimd.swing.table.TableFactory;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.fileimport.FileImportsModel.*;

public class FileImportsDialog extends ValidatedDialog {
    protected final LabelBuilder.Factory labelFactory = new LabelBuilder.Factory(LABELS.get(), RESOURCE_PREFIX);
    protected final LocalizedAction.Factory actionFactory = new LocalizedAction.Factory(LABELS.get(), RESOURCE_PREFIX + "action.");
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final ImportFilePanel filePanel;
    private final ImportFieldsPanel fieldsPanel;
    private final BeanListComboBox<ImportFileModel> importFileList;
    private final Action applyAction = actionFactory.newAction("apply", this::applyChanges);
    private final Action resetAction = actionFactory.newAction("reset", this::resetChanges);
    private final Action newAction = actionFactory.newAction("new", this::newImport);
    private final Action deleteAction = actionFactory.newAction("delete", this::deleteImport);
    private final Action duplicateAction = actionFactory.newAction("duplicate", this::duplicateImport);
    private final FileImportsModel importsModel;

    public FileImportsDialog(Window owner, List<Account> accounts, List<Payee> payees, List<ImportFile> importFiles, TableFactory tableFactory) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        importsModel = new FileImportsModel(importFiles);
        buttonBar.add(new JButton(applyAction));
        buttonBar.add(new JButton(resetAction));
        importFileList = BeanListComboBox.builder(FormatFactory.format(ImportFileModel::getName), importsModel).get();
        filePanel = new ImportFilePanel(this, accounts, payees);
        fieldsPanel = new ImportFieldsPanel(this);
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.file"), filePanel);
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.fields"), fieldsPanel);
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.pageRegions"), new PageRegionsPanel(this, tableFactory));
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.categories"),
                new ImportTablePanel<>(this, "transactionType", importsModel::getCategoryTableModel, ImportTransactionTypeModel::new, tableFactory));
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.payees"),
                new ImportTablePanel<>(this, "importMapping", importsModel::getPayeeTableModel, ImportMapping::new, tableFactory));
        addTab(LABELS.getString(RESOURCE_PREFIX + "tab.securities"),
                new ImportTablePanel<>(this, "importMapping", importsModel::getSecurityTableModel, ImportMapping::new, tableFactory));
        getFormPanel().setLayout(new BorderLayout(0, 10));
        getFormPanel().add(tabbedPane, BorderLayout.CENTER);
        Box listPanel = Box.createHorizontalBox();
        listPanel.add(labelFactory.newLabel("selectedImport", importFileList));
        listPanel.add(Box.createHorizontalStrut(5));
        listPanel.add(importFileList);
        getFormPanel().add(listPanel, BorderLayout.NORTH);
        importsModel.addSelectionListener((oldFile, newFile) -> {
            tabbedPane.setEnabledAt(2, newFile.getFileType() == FileType.PDF);
        });
        createMenuBar();
        getRootPane().getActionMap().remove(CancelAction.ACTION_MAP_KEY);
        addSaveCondition(importsModel::isChanged);
        saveAction.addPropertyChangeListener(event -> applyAction.setEnabled(saveAction.isEnabled()));
        applyAction.setEnabled(saveAction.isEnabled());
        importsModel.addPropertyChangeListener(FileImportsModel.CHANGED_PROPERTY, event -> updateSaveEnabled());
        importsModel.addPropertyChangeListener("names", event -> importFileList.repaint());
        addSaveCondition(importsModel::isValid);
        // cancelAction.setConfirmClose(); // TODO check for unsaved changes
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = ComponentFactory.newMenuToolBar();
        toolBar.add(ComponentFactory.newToolbarButton(newAction));
        toolBar.add(ComponentFactory.newToolbarButton(deleteAction));
        toolBar.add(ComponentFactory.newToolbarButton(duplicateAction));
        menuBar.add(toolBar);
        setJMenuBar(menuBar);
    }

    private void addTab(String mnemonicAndName, JComponent panel) {
        tabbedPane.add(mnemonicAndName.substring(1), panel);
        tabbedPane.setMnemonicAt(tabbedPane.getComponentCount()-1, mnemonicAndName.charAt(0));
    }

    public FileImportsModel getModel() {
        return importsModel;
    }

    public boolean showDialog() {
        pack();
        setVisible(true);
        return false; // TODO check for valid and changed
    }

    private void newImport(ActionEvent event) {
        tabbedPane.setSelectedIndex(0);
        importsModel.addImport();
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

    private void resetChanges(ActionEvent event) {
        // TODO
    }
}
