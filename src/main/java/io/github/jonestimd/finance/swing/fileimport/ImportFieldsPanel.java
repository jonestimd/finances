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

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.swing.BorderFactory;
import io.github.jonestimd.swing.component.MultiSelectListCellRenderer;
import io.github.jonestimd.swing.list.BeanListModel;

public class ImportFieldsPanel extends JComponent {
    private final JList<ImportField> fieldList = new JList<>();
    private final ImportFieldPanel fieldPanel = new ImportFieldPanel();

    public ImportFieldsPanel() {
        // TODO Add/Delete buttons
        // TODO hide/disable detail panel when no field selected
        fieldList.setCellRenderer(new MultiSelectListCellRenderer<>(true, ImportField::getLabels));
        fieldList.addListSelectionListener(this::onFieldSelected);
        setLayout(new BorderLayout(5, 10));
        setBorder(BorderFactory.panelBorder());
        add(new JScrollPane(fieldList), BorderLayout.WEST);
        add(fieldPanel, BorderLayout.CENTER);
    }

    public void setImportFile(ImportFile importFile) {
        fieldList.setModel(new BeanListModel<>(importFile.getFields()));
        fieldPanel.setImportFile(importFile);
    }

    private void onFieldSelected(ListSelectionEvent event) {
        fieldPanel.setImportField(fieldList.getSelectedValue());
    }
}
