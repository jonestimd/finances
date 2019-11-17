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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.swing.BufferedBeanModel;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.swing.table.model.BufferedBeanListTableModel;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportsModel extends BeanListComboBoxModel<ImportFileModel> {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    public static final String CHANGED_PROPERTY = "changed";

    private static final String NAME_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "name.required");
    private static final String NAME_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "name.unique");

    private final Map<ImportFileModel, ImportTransactionTypeTableModel> categoryTableModels = new HashMap<>();
    private final Map<ImportFileModel, ImportPayeeTableModel> payeeTableModels = new HashMap<>();
    private final Map<ImportFileModel, ImportSecurityTableModel> securityTableModels = new HashMap<>();
    private final List<BiConsumer<ImportFileModel, ImportFileModel>> selectionListeners = new ArrayList<>();
    private final List<ImportFile> deletedImports = new ArrayList<>();
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public FileImportsModel(Collection<? extends ImportFile> elements) {
        if (!elements.isEmpty()) {
            List<? extends ImportFile> importFiles = Lists.newArrayList(elements);
            importFiles.sort(Comparator.comparing(ImportFile::getName));
            importFiles.forEach(element -> addElement(newImportFileModel(element)));
            setSelectedItem(getElementAt(0));
        }
    }

    public String validateName(String name) {
        if (name.trim().isEmpty()) return NAME_REQUIRED;
        List<ImportFileModel> matches = Streams.filter(this, file -> file.getName().equalsIgnoreCase(name));
        if (matches.size() > 1 || !matches.isEmpty() && matches.get(0) != getSelectedItem()) return NAME_UNIQUE;
        return null;
    }

    private ImportFileModel newImportFileModel(ImportFile importFile) {
        ImportFileModel model = ImportFileModel.create(importFile);
        model.addPropertyChangeListener(ImportFileModel.CHANGED_PROPERTY, this::forwardChanged);
        model.addPropertyChangeListener("name", this::forwardChange);
        model.addPropertyChangeListener("fileType", this::forwardChange);
        return model;
    }

    private void forwardChanged(PropertyChangeEvent event) {
        changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged());
    }

    private void forwardChange(PropertyChangeEvent event) {
        changeSupport.firePropertyChange(event.getPropertyName(), null, event.getNewValue());
    }

    public PageRegionTableModel getRegionTableModel() {
        if (getSelectedItem() == null) return new PageRegionTableModel();
        return getSelectedItem().getPageRegionTableModel();
    }

    public ImportTransactionTypeTableModel getCategoryTableModel() {
        if (getSelectedItem() == null) return new ImportTransactionTypeTableModel();
        return categoryTableModels.computeIfAbsent(getSelectedItem(), (importFile) -> {
            ImportTransactionTypeTableModel model = new ImportTransactionTypeTableModel();
            Stream.concat(importFile.getImportCategories().stream(), importFile.getImportTransfers().stream())
                    .map(ImportTransactionTypeModel::new).forEach(model::addRow);
            model.addTableModelListener(e -> changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
            return model;
        });
    }

    public ImportPayeeTableModel getPayeeTableModel() {
        if (getSelectedItem() == null) return new ImportPayeeTableModel();
        return payeeTableModels.computeIfAbsent(getSelectedItem(), (importFile) -> {
            ImportPayeeTableModel model = new ImportPayeeTableModel();
            model.setBeans(Streams.map(importFile.getPayeeMap().entrySet(), ImportMapping::new));
            model.addTableModelListener(e -> changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
            return model;
        });
    }

    public ImportSecurityTableModel getSecurityTableModel() {
        if (getSelectedItem() == null) return new ImportSecurityTableModel();
        return securityTableModels.computeIfAbsent(getSelectedItem(), (importFile) -> {
            ImportSecurityTableModel model = new ImportSecurityTableModel();
            model.setBeans(Streams.map(importFile.getSecurityMap().entrySet(), ImportMapping::new));
            model.addTableModelListener(e -> changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
            return model;
        });
    }

    @Override
    public void setSelectedItem(Object importFile) {
        ImportFileModel oldSelection = getSelectedItem();
        super.setSelectedItem(importFile);
        selectionListeners.forEach(listener -> listener.accept(oldSelection, (ImportFileModel) importFile));
    }

    public void addImport() {
        ImportFileModel model = newImportFileModel(ImportFile.newImport());
        addElement(model);
        setSelectedItem(model);
    }

    public void duplicateImport() {
        ImportFileModel model = newImportFileModel(getSelectedItem().clone());
        model.setName("Copy of " + model.getName());
        addElement(model);
        setSelectedItem(model);
    }

    public void deleteImport() {
        ImportFileModel selectedItem = getSelectedItem();
        if (selectedItem.isSaved()) deletedImports.add(selectedItem.getBean());
        categoryTableModels.remove(selectedItem);
        payeeTableModels.remove(selectedItem);
        securityTableModels.remove(selectedItem);
        removeElement(selectedItem);
        if (getSize() > 0) setSelectedItem(getElementAt(0));
        else setSelectedItem(null);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void addSelectionListener(BiConsumer<ImportFileModel, ImportFileModel> listener) {
        selectionListeners.add(listener);
    }

    public boolean isChanged() {
        return Streams.of(this).anyMatch(BufferedBeanModel::isChanged)
                || categoryTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged)
                || payeeTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged)
                || securityTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged)
                || !deletedImports.isEmpty();
    }

    public boolean isValid() {
        return Streams.of(this).allMatch(ImportFileModel::isValid)
                && categoryTableModels.values().stream().allMatch(ValidatedBeanListTableModel::isNoErrors)
                && payeeTableModels.values().stream().allMatch(ValidatedBeanListTableModel::isNoErrors)
                && securityTableModels.values().stream().allMatch(ValidatedBeanListTableModel::isNoErrors);
    }

    public void resetChanges() {
        setElements(Streams.filter(this, UniqueId::isSaved));
        this.forEach(ImportFileModel::resetChanges);
        this.deletedImports.forEach(importFile -> addElement(newImportFileModel(importFile)));
        this.deletedImports.clear();
        categoryTableModels.values().forEach(BufferedBeanListTableModel::revert);
        payeeTableModels.values().forEach(BufferedBeanListTableModel::revert);
        securityTableModels.values().forEach(BufferedBeanListTableModel::revert);
        if (this.getSize() > 0) setSelectedItem(getElementAt(0));
    }
}
