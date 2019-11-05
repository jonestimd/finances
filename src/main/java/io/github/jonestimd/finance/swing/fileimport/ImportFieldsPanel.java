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
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import io.github.jonestimd.finance.swing.BorderFactory;
import io.github.jonestimd.swing.ButtonBarFactory;
import io.github.jonestimd.swing.component.MultiSelectListCellRenderer;
import io.github.jonestimd.swing.list.BeanListModel;

public class ImportFieldsPanel extends JComponent {
    private final JList<ImportFieldModel> fieldList = new JList<>();
    private final ImportFieldPanel fieldPanel = new ImportFieldPanel();
    private final Action addAction;
    private final Action deleteAction;

    public ImportFieldsPanel(FileImportsDialog owner) {
        addAction = owner.actionFactory.newAction("importField.add", this::addField);
        deleteAction = owner.actionFactory.newAction("importField.delete", this::deleteField);
        // TODO hide/disable detail panel when no field selected
        fieldList.setCellRenderer(new MultiSelectListCellRenderer<>(true, ImportFieldModel::getLabels));
        fieldList.addListSelectionListener(this::onFieldSelected);
        JPanel listPanel = new JPanel(new BorderLayout(0, BorderFactory.GAP));
        listPanel.add(new JScrollPane(fieldList), BorderLayout.CENTER);
        listPanel.add(new ButtonBarFactory().alignRight().add(addAction, deleteAction).get(), BorderLayout.SOUTH);
        setLayout(new BorderLayout(5, 10));
        setBorder(BorderFactory.panelBorder());
        add(listPanel, BorderLayout.WEST);
        add(fieldPanel, BorderLayout.CENTER);
        owner.getModel().addSelectionListener((model, importFile) -> setImportFields(model));
        setImportFields(owner.getModel());
    }

    public void setImportFields(FileImportsModel model) {
        fieldList.setModel(new BeanListModel<>(model.getFieldModels()));
        fieldPanel.setImportFile(model.getSelectedItem());
        if (model.getFieldModels().size() > 0) fieldList.setSelectedIndex(0);
    }

    private void onFieldSelected(ListSelectionEvent event) {
        fieldPanel.setImportField(fieldList.getSelectedValue());
    }

    private void addField(ActionEvent event) {
        // TODO
    }

    private void deleteField(ActionEvent event) {
        // TODO
    }
}
