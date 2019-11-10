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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.swing.BufferedBeanModel;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.swing.table.model.BufferedBeanListTableModel;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportsModel extends BeanListComboBoxModel<ImportFile> {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    public static final String CHANGED_PROPERTY = "changed";

    private static final String NAME_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "name.required");
    private static final String NAME_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "name.unique");

    private final Map<ImportFile, ImportFileModel> fileModels = new HashMap<>();
    private final Map<ImportFile, PageRegionTableModel> regionTableModels = new HashMap<>();
    private final Map<ImportFile, ImportTransactionTypeTableModel> categoryTableModels = new HashMap<>();
    private final Map<ImportFile, ImportPayeeTableModel> payeeTableModels = new HashMap<>();
    private final Map<ImportFile, ImportSecurityTableModel> securityTableModels = new HashMap<>();
    private final List<BiConsumer<ImportFileModel, ImportFileModel>> selectionListeners = new ArrayList<>();
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public FileImportsModel(Collection<? extends ImportFile> elements) {
        super(elements);
        elements.forEach(this::newImportFileModel);
        if (elements.isEmpty()) addImport();
        else setSelectedItem(elements.iterator().next());
    }

    public String validateName(String name) {
        if (name.trim().isEmpty()) return NAME_REQUIRED;
        List<ImportFile> matches = Streams.filter(this, file -> file.getName().equalsIgnoreCase(name));
        if (matches.size() > 1 || !matches.isEmpty() && matches.get(0) != getSelectedItem()) return NAME_UNIQUE;
        return null;
    }

    private void newImportFileModel(ImportFile importFile) {
        ImportFileModel model = ImportFileModel.create(importFile);
        fileModels.put(importFile, model);
        model.addPropertyChangeListener(ImportFileModel.CHANGED_PROPERTY, this::forwardChange);
    }

    private void forwardChange(PropertyChangeEvent event) {
        changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged());
    }

    public PageRegionTableModel getRegionTableModel() {
        if (getSelectedItem() == null) return new PageRegionTableModel();
        return regionTableModels.computeIfAbsent(getSelectedItem(), (importFile) -> {
            PageRegionTableModel model = new PageRegionTableModel();
            model.setBeans(importFile.getPageRegions());
            model.addTableModelListener(e -> changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
            return model;
        });
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
        ImportFile oldSelection = getSelectedItem();
        super.setSelectedItem(importFile);
        notifySelectionListeners(oldSelection, (ImportFile) importFile);
    }

    public void addImport() {
        ImportFile importFile = new ImportFile();
        importFile.setName("");
        importFile.setDateFormat("");
        importFile.setFields(new HashSet<>());
        importFile.setPageRegions(new HashSet<>());
        importFile.setImportCategories(new HashSet<>());
        importFile.setImportTransfers(new HashSet<>());
        importFile.setPayeeMap(new HashMap<>());
        importFile.setSecurityMap(new HashMap<>());
        addElement(importFile);
        newImportFileModel(importFile);
        setSelectedItem(importFile);
    }

    public ImportFileModel getSelectedImport() {
        return fileModels.get(getSelectedItem());
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

    private void notifySelectionListeners(ImportFile oldFile, ImportFile newFile) {
        ImportFileModel oldModel = fileModels.get(oldFile);
        ImportFileModel newModel = fileModels.get(newFile);
        selectionListeners.forEach(listener -> listener.accept(oldModel, newModel));
    }

    public boolean isChanged() {
        return fileModels.values().stream().anyMatch(BufferedBeanModel::isChanged)
                || regionTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged)
                || categoryTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged)
                || payeeTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged)
                || securityTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged);
    }

    public boolean isValid() {
        return fileModels.values().stream().allMatch(ImportFileModel::isValid)
                && regionTableModels.values().stream().allMatch(ValidatedBeanListTableModel::isNoErrors)
                && categoryTableModels.values().stream().allMatch(ValidatedBeanListTableModel::isNoErrors)
                && payeeTableModels.values().stream().allMatch(ValidatedBeanListTableModel::isNoErrors)
                && securityTableModels.values().stream().allMatch(ValidatedBeanListTableModel::isNoErrors);
    }
}
