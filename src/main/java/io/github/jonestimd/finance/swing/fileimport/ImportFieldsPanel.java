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

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.ComboBoxCellEditor;
import io.github.jonestimd.swing.table.FormatTableCellRenderer;
import io.github.jonestimd.swing.table.MultiSelectTableCellRenderer;
import io.github.jonestimd.swing.table.PopupListTableCellEditor;
import io.github.jonestimd.swing.table.TableFactory;
import io.github.jonestimd.swing.validation.ValidatedComponent;

public class ImportFieldsPanel extends ImportTablePanel<ImportField, ImportFieldTableModel> {
    private static final Format REGION_FORMAT = FormatFactory.format(ImportFieldsPanel::pageRegionName);

    private final ValidationHolder validationHolder = new ValidationHolder();
    private final TableModelComboBoxAdapter<PageRegion> pageRegionModel = new TableModelComboBoxAdapter<>(true);
    private TableModelListener regionModelListener = (event) -> table.getModel().validatePageRegions();
    private TableModelListener fieldModelListener = (event) -> validationHolder.validateValue();
    private PropertyChangeListener singlePayeeListener = (event) -> validationHolder.validateValue();
    private ImportFileModel fileModel;

    public ImportFieldsPanel(FileImportsDialog owner, TableFactory tableFactory) {
        super(owner, "importField", owner.getModel()::getImportFieldTableModel, ImportFieldsPanel::newImportField, tableFactory);
        addToButtonBar(validationHolder, 0);
        table.setDefaultRenderer(List.class, new MultiSelectTableCellRenderer<>(true));
        table.setDefaultRenderer(PageRegion.class, new FormatTableCellRenderer(REGION_FORMAT));
        table.setDefaultEditor(List.class, PopupListTableCellEditor.builder(Function.identity(), Function.identity()).build());
        table.setDefaultEditor(FieldType.class, new ComboBoxCellEditor(new JComboBox<>(FieldType.values())));
        table.setDefaultEditor(AmountFormat.class, new ComboBoxCellEditor(BeanListComboBox.builder(AmountFormat.class).optional().get()));
        table.setDefaultEditor(PageRegion.class, new ComboBoxCellEditor(new BeanListComboBox<>(REGION_FORMAT, pageRegionModel)));
        owner.getModel().addSelectionListener((oldFile, newFile) -> {
            if (oldFile != null) {
                oldFile.getPageRegionTableModel().removeTableModelListener(regionModelListener);
                oldFile.getImportFieldTableModel().removeTableModelListener(fieldModelListener);
                oldFile.removePropertyChangeListener("singlePayee", singlePayeeListener);
            }
            if (newFile != null) setFileImport(newFile);
        });
        if (owner.getModel().getSelectedItem() != null) setFileImport(owner.getModel().getSelectedItem());
    }

    private void setFileImport(ImportFileModel fileModel) {
        this.fileModel = fileModel;
        pageRegionModel.setSource(fileModel.getPageRegionTableModel());
        fileModel.getPageRegionTableModel().addTableModelListener(regionModelListener);
        fileModel.getImportFieldTableModel().addTableModelListener(fieldModelListener);
        fileModel.addPropertyChangeListener("singlePayee", singlePayeeListener);
        validationHolder.validateValue();
    }

    private static ImportField newImportField() {
        return new ImportField(new ArrayList<>(), null);
    }

    private static String pageRegionName(PageRegion region) {
        return region.getName() == null ? "" : region.getName();
    }

    /**
     * Hidden component that provides validation on the table as a whole.  Nesting it inside the panel
     * keeps the errors visible when other tabs are selected (JTabbedPane uses setVisible() which hides
     * messages for that panel but not its children).
     */
    private class ValidationHolder extends JComponent implements ValidatedComponent {
        private String validationMessages;

        public ValidationHolder() {
            setPreferredSize(new Dimension());
        }

        @Override
        public void validateValue() {
            String oldMessages = this.validationMessages;
            this.validationMessages = getValidationMessages();
            firePropertyChange(VALIDATION_MESSAGES, oldMessages, this.validationMessages);
        }

        @Override
        public String getValidationMessages() {
            return new FieldTypeValidator(fileModel).validateImportFields();
        }

        @Override
        public void addValidationListener(PropertyChangeListener listener) {
            addPropertyChangeListener(VALIDATION_MESSAGES, listener);
        }

        @Override
        public void removeValidationListener(PropertyChangeListener listener) {
            removePropertyChangeListener(VALIDATION_MESSAGES, listener);
        }
    }
}
