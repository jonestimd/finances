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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.swing.table.model.BufferedBeanListTableModel;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportsModel extends BeanListComboBoxModel<ImportFile> {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    public static final String CHANGED_PROPERTY = "changed";

    private static final String NAME_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "name.required");
    private static final String NAME_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "name.unique");
    private static final Map<String, Function<ImportFile, Object>> PROPERTY_GETTERS = new MapBuilder<String, Function<ImportFile, Object>>()
        .put("name", ImportFile::getName)
        .put("importType", ImportFile::getImportType)
        .put("fileType", ImportFile::getFileType)
        .put("account", ImportFile::getAccount)
        .put("startOffset", ImportFile::getStartOffset)
        .put("dateFormat", ImportFile::getDateFormat)
        .put("reconcile", ImportFile::isReconcile)
        .put("payee", ImportFile::getPayee)
        .get();

    private final Map<ImportFile, Map<String, Object>> changes = new HashMap<>();
    private final Map<ImportFile, Map<FieldType, List<String>>> transactionLabelChanges = new HashMap<>();
    private final Map<Long, PageRegionTableModel> regionTableModels = new HashMap<>();
    private final List<BiConsumer<FileImportsModel, ImportFile>> selectionListeners = new ArrayList<>();
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public FileImportsModel(Collection<? extends ImportFile> elements) {
        super(elements);
        if (elements.isEmpty()) addImport();
        else setSelectedItem(elements.iterator().next());
    }

    public String validateName(String name) {
        if (name.trim().isEmpty()) return NAME_REQUIRED;
        List<ImportFile> matches = Streams.filter(this, file -> file.getName().equalsIgnoreCase(name));
        if (matches.size() > 1 || !matches.isEmpty() && matches.get(0) != getSelectedItem()) return NAME_UNIQUE;
        return null;
    }

    public PageRegionTableModel getRegionTableModel() {
        if (getSelectedItem() == null) return new PageRegionTableModel();
        return regionTableModels.computeIfAbsent(getSelectedItem().getId(), (id) -> {
            PageRegionTableModel model = new PageRegionTableModel();
            model.setBeans(getSelectedItem().getPageRegions());
            model.addTableModelListener(e -> changeSupport.firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
            return model;
        });
    }

    @Override
    public void setSelectedItem(Object importFile) {
        super.setSelectedItem(importFile);
        notifySelectionListeners((ImportFile) importFile);
        Map<String, Object> importChanges = changes.getOrDefault(importFile, Collections.emptyMap());
        for (Entry<String, Function<ImportFile, Object>> entry : PROPERTY_GETTERS.entrySet()) {
            changeSupport.firePropertyChange(entry.getKey() + "Changed", null, importChanges.containsKey(entry.getKey()));
        }
    }

    public void addImport() {
        ImportFile importFile = new ImportFile();
        importFile.setName("");
        importFile.setFields(new HashSet<>());
        importFile.setPageRegions(new HashSet<>());
        addElement(importFile);
        setSelectedItem(importFile);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void addSelectionListener(BiConsumer<FileImportsModel, ImportFile> listener) {
        selectionListeners.add(listener);
    }

    private void notifySelectionListeners(ImportFile importFile) {
        selectionListeners.forEach(listener -> listener.accept(this, importFile));
    }

    public boolean isChanged() {
        return !changes.isEmpty()
                || !transactionLabelChanges.isEmpty()
                || regionTableModels.values().stream().anyMatch(BufferedBeanListTableModel::isChanged);
    }

    public String getImportName() {
        return getProperty("name");
    }

    public void setImportName(String name) {
        setProperty("name", name);
    }

    public ImportType getImportType() {
        return getProperty("importType");
    }

    public void setImportType(ImportType importType) {
        setProperty("importType", importType);
    }

    public FileType getFileType() {
        return getProperty("fileType");
    }

    public void setFileType(FileType fileType) {
        setProperty("fileType", fileType);
    }

    public Account getAccount() {
        return getProperty("account");
    }

    public void setAccount(Account account) {
        setProperty("account", account);
    }

    public Integer getStartOffset() {
        return getProperty("startOffset");
    }

    public void setStartOffset(Integer offset) {
        setProperty("startOffset", offset);
    }

    public String getDateFormat() {
        return getProperty("dateFormat");
    }

    public void setDateFormat(String dateFormat) {
        setProperty("dateFormat", dateFormat);
    }

    public boolean isReconcile() {
        return getProperty("reconcile");
    }

    public void setReconcile(boolean reconcile) {
        setProperty("reconcile", reconcile);
    }

    public Payee getPayee() {
        return getProperty("payee");
    }

    public void setPayee(Payee payee) {
        setProperty("payee", payee);
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(String name) {
        ImportFile selectedImport = getSelectedItem();
        Map<String, ?> importChanges = changes.getOrDefault(selectedImport, Collections.emptyMap());
        return importChanges.containsKey(name) ? (T) importChanges.get(name) : (T) PROPERTY_GETTERS.get(name).apply(selectedImport);
    }

    private <T> void setProperty(final String name, T value) {
        boolean oldChanged = isChanged();
        ImportFile selectedImport = getSelectedItem();
        boolean same = Objects.equals(PROPERTY_GETTERS.get(name).apply(selectedImport), value);
        if (same) {
            Map<String, Object> importChanges = changes.get(selectedImport);
            if (importChanges != null) {
                importChanges.remove(name);
                if (importChanges.isEmpty()) changes.remove(selectedImport);
            }
        }
        else changes.computeIfAbsent(selectedImport, (i) -> new HashMap<>()).put(name, value);
        changeSupport.firePropertyChange(name + "Changed", null, !same);
        changeSupport.firePropertyChange(CHANGED_PROPERTY, oldChanged, isChanged());
    }

    public List<String> getDateLabels() {
        return getCurrentLabels(FieldType.DATE);
    }

    public void setDateLabels(List<String> dateLabels) {
        setCurrentLabels(FieldType.DATE, dateLabels);
    }

    public List<String> getPayeeLabels() {
        return getCurrentLabels(FieldType.PAYEE);
    }

    public void setPayeeLabels(List<String> payeeLabels) {
        setCurrentLabels(FieldType.PAYEE, payeeLabels);
    }

    public List<String> getSecurityLabels() {
        return getCurrentLabels(FieldType.SECURITY);
    }

    public void setSecurityLabels(List<String> securityLabels) {
        setCurrentLabels(FieldType.SECURITY, securityLabels);
    }

    private List<String> getCurrentLabels(FieldType type) {
        ImportFile selectedImport = getSelectedItem();
        Map<FieldType, List<String>> importChanges = transactionLabelChanges.getOrDefault(selectedImport, Collections.emptyMap());
        return importChanges.containsKey(type) ? importChanges.get(type) : getLabels(type);
    }

    private void setCurrentLabels(FieldType type, List<String> labels) {
        boolean oldChanged = isChanged();
        ImportFile selectedImport = getSelectedItem();
        List<String> original = getLabels(type);
        boolean same = original.equals(labels);
        if (same) {
            Map<FieldType, List<String>> importChanges = transactionLabelChanges.get(selectedImport);
            if (importChanges != null) {
                importChanges.remove(type);
                if (importChanges.isEmpty()) transactionLabelChanges.remove(selectedImport);
            }
        }
        else transactionLabelChanges.computeIfAbsent(selectedImport, (i) -> new HashMap<>()).put(type, labels);
        changeSupport.firePropertyChange(type.name().toLowerCase() + "LabelsChanged", null, !same);
        changeSupport.firePropertyChange(CHANGED_PROPERTY, oldChanged, isChanged());
    }

    private List<String> getLabels(FieldType type) {
        return getFields(type).findFirst().map(ImportField::getLabels).orElseGet(ArrayList::new);
    }

    private Stream<ImportField> getFields(FieldType type) {
        return getSelectedItem().getFields().stream().filter(field -> field.getType() == type);
    }
}
