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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.swing.BufferedBeanModel;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportsModel extends BeanListComboBoxModel<ImportFileModel> {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    public static final String CHANGED_PROPERTY = "changed";

    private static final String NAME_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "name.required");
    private static final String NAME_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "name.unique");

    private final List<BiConsumer<ImportFileModel, ImportFileModel>> selectionListeners = new ArrayList<>();
    private final List<ImportFileModel> deletedImports = new ArrayList<>();
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

    public ImportFieldTableModel getImportFieldTableModel() {
        return getSelectedItem() == null ? new ImportFieldTableModel(null) : getSelectedItem().getImportFieldTableModel();
    }

    public PageRegionTableModel getRegionTableModel() {
        return getSelectedItem() == null ? new PageRegionTableModel() : getSelectedItem().getPageRegionTableModel();
    }

    public ImportTransactionTypeTableModel getCategoryTableModel() {
        return getSelectedItem() == null ? new ImportTransactionTypeTableModel() : getSelectedItem().getCategoryTableModel();
    }

    public ImportPayeeTableModel getPayeeTableModel() {
        return getSelectedItem() == null ? new ImportPayeeTableModel() : getSelectedItem().getPayeeTableModel();
    }

    public ImportSecurityTableModel getSecurityTableModel() {
        return getSelectedItem() == null ? new ImportSecurityTableModel() : getSelectedItem().getSecurityTableModel();
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
        if (selectedItem.isSaved()) deletedImports.add(selectedItem);
        removeElement(selectedItem);
        if (getSize() > 0) setSelectedItem(getElementAt(0));
        else setSelectedItem(null);
    }

    /**
     * Get the list of deleted imports.  Also clears the list.
     */
    public List<ImportFile> getDeletes() {
        List<ImportFile> deletes = Streams.map(deletedImports, ImportFileModel::getBean);
        deletedImports.clear();
        changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged());
        return deletes;
    }

    /**
     * Get the set of modified imports.  Also commits the changes.
     */
    public Set<ImportFile> getChanges() {
        Set<ImportFile> changes = new HashSet<>();
        for (ImportFileModel fileModel : this) {
            if (fileModel.commitTables() || fileModel.isChanged()) changes.add(fileModel.commitChanges());
        }
        return changes;
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
        return Streams.of(this).anyMatch(BufferedBeanModel::isChanged) || !deletedImports.isEmpty();
    }

    public boolean isValid() {
        return Streams.of(this).allMatch(ImportFileModel::isValid);
    }

    public void resetChanges() {
        setElements(Streams.filter(this, UniqueId::isSaved));
        this.deletedImports.forEach(this::addElement);
        this.deletedImports.clear();
        this.forEach(ImportFileModel::resetChanges);
        if (this.getSize() > 0) setSelectedItem(getElementAt(0));
    }
}
