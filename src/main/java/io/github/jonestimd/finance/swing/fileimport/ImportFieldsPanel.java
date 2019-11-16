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
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;

import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.swing.BorderFactory;
import io.github.jonestimd.swing.ButtonBarFactory;
import io.github.jonestimd.swing.component.MultiSelectListCellRenderer;
import io.github.jonestimd.swing.list.BeanListModel;
import io.github.jonestimd.swing.validation.ValidatedComponent;
import io.github.jonestimd.swing.validation.ValidationSupport;

public class ImportFieldsPanel extends JComponent {
    private final FieldList fieldList = new FieldList();
    private final ImportFieldPanel fieldPanel = new ImportFieldPanel();
    private final Action addAction;
    private final Action deleteAction; // TODO disable when no field selected
    private final PropertyChangeListener repaintList = (event) -> fieldList.repaint();
    private ImportFileModel fileModel;
    private BeanListModel<ImportFieldModel> listModel;

    public ImportFieldsPanel(FileImportsDialog owner) {
        addAction = owner.actionFactory.newAction("importField.add", this::addField);
        deleteAction = owner.actionFactory.newAction("importField.delete", this::deleteField);
        fieldList.setCellRenderer(new ListCellRenderer());
        fieldList.addListSelectionListener(this::onFieldSelected);
        JPanel listPanel = new JPanel(new BorderLayout(0, BorderFactory.GAP));
        listPanel.add(new JScrollPane(fieldList), BorderLayout.CENTER);
        listPanel.add(new ButtonBarFactory().alignRight().add(addAction, deleteAction).get(), BorderLayout.SOUTH);
        setLayout(new BorderLayout(5, 10));
        setBorder(BorderFactory.panelBorder());
        add(listPanel, BorderLayout.WEST);
        add(fieldPanel, BorderLayout.CENTER);
        owner.getModel().addSelectionListener((oldFile, newFile) -> {
            if (oldFile != null) oldFile.getFieldModels().forEach(fieldModel -> fieldModel.removePropertyChangeListener(repaintList));
            setImportFile(newFile);
        });
        setImportFile(owner.getModel().getSelectedItem());
    }

    public void setImportFile(ImportFileModel model) {
        if (this.fileModel != null) {
            this.fileModel.getFieldModels().forEach(fieldModel -> fieldModel.removePropertyChangeListener(repaintList));
        }
        this.fileModel = model;
        this.listModel = new BeanListModel<>(model == null ? Collections.emptyList() : model.getFieldModels());
        fieldList.setModel(listModel);
        fieldPanel.setImportFile(model);
        if (model != null) {
            model.getFieldModels().forEach(fieldModel -> fieldModel.addPropertyChangeListener(repaintList));
            if (model.getFieldModels().size() > 0) fieldList.setSelectedIndex(0);
            fieldList.validateValue();
        }
    }

    private void onFieldSelected(ListSelectionEvent event) {
        if (fieldList.getSelectedValue() != null) {
            fieldPanel.setImportField(fieldList.getSelectedValue());
            fieldPanel.setVisible(true);
        }
        else fieldPanel.setVisible(false);
    }

    private void addField(ActionEvent event) {
        ImportFieldModel fieldModel = fileModel.addFieldModel(new ImportField(new ArrayList<>(), null));
        fieldModel.addPropertyChangeListener(repaintList);
        listModel.addElement(fieldModel);
        fieldList.setSelectedIndex(listModel.getSize()-1);
        fieldList.validateValue();
    }

    private void deleteField(ActionEvent event) {
        fileModel.removeFieldModel(fieldList.getSelectedValue());
        listModel.removeElementAt(fieldList.getSelectedIndex());
        fieldList.validateValue();
    }

    private static class ListCellRenderer extends MultiSelectListCellRenderer<ImportFieldModel> {
        private static final Color SELECTED_ERROR_BACKGROUND = new Color(255, 215, 215);

        public ListCellRenderer() {
            super(true, ImportFieldModel::getLabels);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ImportFieldModel> list, ImportFieldModel value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!value.isValid()) {
                if (isSelected) setBackground(Color.PINK);
                else setBackground(SELECTED_ERROR_BACKGROUND);
            }
            return this;
        }
    }

    private class FieldList extends JList<ImportFieldModel> implements ValidatedComponent {
        private final ValidationSupport<ImportFileModel> validationSupport = new ValidationSupport<>(this, ImportFileModel::validateFields);

        @Override
        public void validateValue() {
            validationSupport.validateValue(fileModel);
        }

        @Override
        public String getValidationMessages() {
            return validationSupport.getMessages();
        }

        @Override
        public void addValidationListener(PropertyChangeListener listener) {
            validationSupport.addValidationListener(listener);
        }

        @Override
        public void removeValidationListener(PropertyChangeListener listener) {
            validationSupport.removeValidationListener(listener);
        }
    }
}
