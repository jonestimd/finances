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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.LabelBuilder;
import io.github.jonestimd.swing.SettingsPersister;
import io.github.jonestimd.swing.action.BackgroundAction;
import io.github.jonestimd.swing.action.CancelAction;
import io.github.jonestimd.swing.action.LocalizedAction;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.dialog.Dialogs;
import io.github.jonestimd.swing.dialog.ValidatedDialog;
import io.github.jonestimd.swing.table.TableFactory;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.fileimport.FileImportsModel.*;

public class FileImportsDialog extends ValidatedDialog {
    protected static final Color ERROR_COLOR = LABELS.getColor("error.foreground.color");
    protected static final String EMPTY_PLACEHOLDER = LABELS.getString("error.empty.placeholder");
    protected final LabelBuilder.Factory labelFactory = new LabelBuilder.Factory(LABELS.get(), RESOURCE_PREFIX);
    protected final LocalizedAction.Factory actionFactory = new LocalizedAction.Factory(LABELS.get(), RESOURCE_PREFIX + "action.");
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final ImportFilePanel filePanel;
    private final ImportFieldsPanel fieldsPanel;
    private final PageRegionsPanel regionsPanel;
    private final ImportTablePanel<ImportTransactionTypeModel, ImportTransactionTypeTableModel> categoriesPanel;
    private final ImportTablePanel<ImportMapping<Payee>, ImportPayeeTableModel> payeesPanel;
    private final ImportTablePanel<ImportMapping<Security>, ImportSecurityTableModel> securitiesPanel;
    private final BeanListComboBox<ImportFileModel> importFileList;
    private final ApplyAction applyAction = new ApplyAction();
    private final Action resetAction = actionFactory.newAction("reset", this::resetChanges);
    private final Action newAction = actionFactory.newAction("new", this::newImport);
    private final Action deleteAction = actionFactory.newAction("delete", this::deleteImport);
    private final Action duplicateAction = actionFactory.newAction("duplicate", this::duplicateImport);
    private final FileImportsModel importsModel;
    private final Runnable saveImports;

    public FileImportsDialog(Window owner, List<Account> accounts, List<TransactionCategory> categories, List<Payee> payees,
            FileImportsModel importsModel, TableFactory tableFactory, Runnable saveImports) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        this.importsModel = importsModel;
        this.saveImports = saveImports;
        buttonBar.add(new JButton(applyAction));
        buttonBar.add(new JButton(resetAction));
        importFileList = BeanListComboBox.builder(FormatFactory.format(ImportFileModel::getName), importsModel).get();
        importFileList.setRenderer(new ImportListRenderer(importFileList.getRenderer()));
        filePanel = addTab(LABELS.getString(RESOURCE_PREFIX + "tab.file"), new ImportFilePanel(this, accounts, payees));
        fieldsPanel = addTab(LABELS.getString(RESOURCE_PREFIX + "tab.columns"), new ImportFieldsPanel(this, tableFactory, accounts, categories));
        regionsPanel = addTab(LABELS.getString(RESOURCE_PREFIX + "tab.pageRegions"), new PageRegionsPanel(this, tableFactory));
        categoriesPanel = addTab(LABELS.getString(RESOURCE_PREFIX + "tab.categories"),
                new ImportTablePanel<>(this, "transactionType",
                        tableFactory.validatedTableBuilder(importsModel.getCategoryTableModel()).sortedBy(0).get(),
                        importsModel::getCategoryTableModel, ImportTransactionTypeModel::new));
        payeesPanel = addTab(LABELS.getString(RESOURCE_PREFIX + "tab.payees"),
                new ImportTablePanel<>(this, "importMapping",
                        tableFactory.validatedTableBuilder(importsModel.getPayeeTableModel()).sortedBy(0).get(),
                        importsModel::getPayeeTableModel, ImportMapping::new));
        securitiesPanel = addTab(LABELS.getString(RESOURCE_PREFIX + "tab.securities"),
                new ImportTablePanel<>(this, "importMapping",
                        tableFactory.validatedTableBuilder(importsModel.getSecurityTableModel()).sortedBy(0).get(),
                        importsModel::getSecurityTableModel, ImportMapping::new));
        getFormPanel().setLayout(new BorderLayout(0, 10));
        getFormPanel().add(tabbedPane, BorderLayout.CENTER);
        Box listPanel = Box.createHorizontalBox();
        listPanel.add(labelFactory.newLabel("selectedImport", importFileList));
        listPanel.add(Box.createHorizontalStrut(5));
        listPanel.add(importFileList);
        getFormPanel().add(listPanel, BorderLayout.NORTH);
        importsModel.addPropertyChangeListener("fileType", event -> tabbedPane.setEnabledAt(2, event.getNewValue() == FileType.PDF));
        importsModel.addSelectionListener((oldFile, newFile) -> setImportFile(newFile));
        if (importsModel.getSelectedItem() != null) setImportFile(importsModel.getSelectedItem());
        createMenuBar();
        getRootPane().getActionMap().remove(CancelAction.ACTION_MAP_KEY);
        saveAction.addPropertyChangeListener(event -> applyAction.setEnabled(saveAction.isEnabled()));
        applyAction.setEnabled(saveAction.isEnabled());
        importsModel.addPropertyChangeListener(FileImportsModel.CHANGED_PROPERTY, event -> updateSaveEnabled());
        importsModel.addPropertyChangeListener("name", event -> importFileList.repaint());
        addSaveCondition(importsModel::isChanged);
        addSaveCondition(importsModel::isValid);
        cancelAction.setConfirmClose(this::confirmClose);
    }

    private boolean confirmClose() {
        return !importsModel.isChanged() || Dialogs.confirmDiscardChanges(this);
    }

    private void setImportFile(ImportFileModel newFile) {
        boolean enabled = newFile != null;
        deleteAction.setEnabled(enabled);
        duplicateAction.setEnabled(enabled);
        for (int i = 1; i < tabbedPane.getTabCount(); i++) tabbedPane.setEnabledAt(i, enabled);
        tabbedPane.setEnabledAt(2, enabled && newFile.getFileType() == FileType.PDF);
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

    private <C extends JComponent> C addTab(String mnemonicAndName, C panel) {
        tabbedPane.add(mnemonicAndName.substring(1), panel);
        tabbedPane.setMnemonicAt(tabbedPane.getComponentCount() - 1, mnemonicAndName.charAt(0));
        return panel;
    }

    public FileImportsModel getModel() {
        return importsModel;
    }

    public boolean showDialog() {
        pack();
        setVisible(true);
        return !isCancelled() || applyAction.applied;
    }

    private void newImport(ActionEvent event) {
        tabbedPane.setSelectedIndex(0);
        importsModel.addImport();
    }

    private void duplicateImport(ActionEvent event) {
        tabbedPane.setSelectedIndex(0);
        importsModel.duplicateImport();
    }

    private void deleteImport(ActionEvent event) {
        importsModel.deleteImport();
    }

    private void resetChanges(ActionEvent event) {
        importsModel.resetChanges();
        filePanel.setImportFile(importsModel.getSelectedItem());
        fieldsPanel.setTableModel(importsModel.getImportFieldTableModel());
        regionsPanel.setTableModel(importsModel.getRegionTableModel());
        categoriesPanel.setTableModel(importsModel.getCategoryTableModel());
        payeesPanel.setTableModel(importsModel.getPayeeTableModel());
        securitiesPanel.setTableModel(importsModel.getSecurityTableModel());
    }

    @Override
    public void dispose() {
        super.dispose();
        ComponentTreeUtils.visitComponentTree(getContentPane(), JComponent.class, SettingsPersister::saveSettings);
    }

    protected static class ImportListRenderer implements ListCellRenderer<ImportFileModel> {
        private final ListCellRenderer<? super ImportFileModel> delegate;

        protected ImportListRenderer(ListCellRenderer<? super ImportFileModel> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ImportFileModel> list, ImportFileModel value, int index,
                boolean isSelected, boolean cellHasFocus) {
            Component renderer = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null && !value.isValid()) {
                renderer.setForeground(ERROR_COLOR);
                if (value.getName().trim().isEmpty()) ((JLabel) renderer).setText(EMPTY_PLACEHOLDER);
            }
            return renderer;
        }
    }

    protected class ApplyAction extends BackgroundAction<Set<ImportFile>> {
        protected boolean applied = false;

        protected ApplyAction() {
            super(FileImportsDialog.this, LABELS.get(), RESOURCE_PREFIX + "action.apply");
        }

        @Override
        protected boolean confirmAction(ActionEvent event) {
            return true;
        }

        @Override
        protected Set<ImportFile> performTask() {
            saveImports.run();
            applied = true;
            return null;
        }

        @Override
        protected void updateUI(Set<ImportFile> result) {
            updateSaveEnabled(); // new imports now have IDs
            // nothing else to do because models contain saved entities
        }
    }
}
