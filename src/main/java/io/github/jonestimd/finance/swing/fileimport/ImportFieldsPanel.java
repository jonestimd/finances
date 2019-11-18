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

import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.JComboBox;
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

public class ImportFieldsPanel extends ImportTablePanel<ImportField, ImportFieldTableModel> {
    private static final Format REGION_FORMAT = FormatFactory.format(ImportFieldsPanel::pageRegionName);

    private final TableModelComboBoxAdapter<PageRegion> pageRegionModel = new TableModelComboBoxAdapter<>(true);
    private TableModelListener regionModelListener = (event) -> table.getModel().validatePageRegions();

    public ImportFieldsPanel(FileImportsDialog owner, TableFactory tableFactory) {
        super(owner, "importField", owner.getModel()::getImportFieldTableModel, ImportFieldsPanel::newImportField, tableFactory);
        table.setDefaultRenderer(List.class, new MultiSelectTableCellRenderer<>(true));
        table.setDefaultRenderer(PageRegion.class, new FormatTableCellRenderer(REGION_FORMAT));
        table.setDefaultEditor(List.class, PopupListTableCellEditor.builder(Function.identity(), Function.identity()).build());
        table.setDefaultEditor(FieldType.class, new ComboBoxCellEditor(new JComboBox<>(FieldType.values())));
        table.setDefaultEditor(AmountFormat.class, new ComboBoxCellEditor(BeanListComboBox.builder(AmountFormat.class).optional().get()));
        table.setDefaultEditor(PageRegion.class, new ComboBoxCellEditor(new BeanListComboBox<>(REGION_FORMAT, pageRegionModel)));
        owner.getModel().addSelectionListener((oldFile, newFile) -> {
            if (oldFile != null) oldFile.getPageRegionTableModel().removeTableModelListener(regionModelListener);
            if (newFile != null) setFileImport(newFile);
        });
        if (owner.getModel().getSelectedItem() != null) setFileImport(owner.getModel().getSelectedItem());
    }

    private void setFileImport(ImportFileModel fileModel) {
        pageRegionModel.setSource(fileModel.getPageRegionTableModel());
        fileModel.getPageRegionTableModel().addTableModelListener(regionModelListener);
    }

    private static ImportField newImportField() {
        return new ImportField(new ArrayList<>(), null);
    }

    private static String pageRegionName(PageRegion region) {
        return region.getName() == null ? "" : region.getName();
    }
}
