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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.swing.BufferedBeanModel;
import io.github.jonestimd.finance.swing.BufferedBeanModelHandler;
import javassist.util.proxy.ProxyFactory;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static org.apache.commons.lang.StringUtils.*;

public abstract class ImportFileModel extends ImportFile implements BufferedBeanModel<ImportFile> {
    public static final String CHANGED_PROPERTY = BufferedBeanModelHandler.CHANGED_PROPERTY;
    public static final String FIELDS_PROPERTY = "fields";
    public static final String FIELDS_REQUIRED = LABELS.getString("dialog.fileImport.importField.required");
    private static final ProxyFactory factory;

    static {
        factory = new ProxyFactory();
        factory.setSuperclass(ImportFileModel.class);
    }

    public static ImportFileModel create(ImportFile importFile) {
        try {
            return (ImportFileModel) factory.create(new Class[] {}, new Object[] {}, new Handler(importFile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<FieldType, List<String>> labelChanges = new HashMap<>();
    private List<ImportFieldModel> fieldModels;
    private List<ImportFieldModel> deletedFields = new ArrayList<>();

    protected ImportFileModel() {
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
        return labelChanges.containsKey(type) ? labelChanges.get(type) : getLabels(type);
    }

    private void setCurrentLabels(FieldType type, List<String> labels) {
        boolean oldChanged = isChanged();
        List<String> original = getLabels(type);
        boolean same = original.equals(labels);
        if (same) labelChanges.remove(type);
        else labelChanges.put(type, labels);
        firePropertyChange(type.name().toLowerCase() + "LabelsChanged", null, !same);
        firePropertyChange(CHANGED_PROPERTY, oldChanged, isChanged());
    }

    private List<String> getLabels(FieldType type) {
        return getFields(type).findFirst().map(ImportField::getLabels).orElseGet(ArrayList::new);
    }

    private Stream<ImportField> getFields(FieldType type) {
        return getFields().stream().filter(field -> field.getType() == type);
    }

    public List<ImportFieldModel> getFieldModels() {
        if (fieldModels == null) {
            fieldModels = getFields().stream().filter(field -> !field.getType().isTransaction())
                    .map(this::newFieldModel).collect(Collectors.toList());
        }
        return fieldModels;
    }

    public ImportFieldModel addFieldModel(ImportField field) {
        ImportFieldModel model = newFieldModel(field);
        fieldModels.add(model);
        firePropertyChange(FIELDS_PROPERTY, null, fieldModels);
        return model;
    }

    public void removeFieldModel(ImportFieldModel fieldModel) {
        fieldModels.remove(fieldModel);
        deletedFields.add(fieldModel);
        firePropertyChange(FIELDS_PROPERTY, null, fieldModels);
        firePropertyChange(CHANGED_PROPERTY, null, isChanged());
    }

    private ImportFieldModel newFieldModel(ImportField field) {
        ImportFieldModel model = ImportFieldModel.create(field);
        model.addPropertyChangeListener(ImportFieldModel.CHANGED_PROPERTY, this::forwardChange);
        return model;
    }

    private void forwardChange(PropertyChangeEvent event) {
        firePropertyChange(CHANGED_PROPERTY, null, isChanged());
    }

    protected abstract void firePropertyChange(String property, Object oldValue, Object newValue);

    public String validateFields() {
        return getFieldModels().isEmpty() ? FIELDS_REQUIRED : null;
    }

    public boolean isValid() {
        return isNotBlank(getName()) && getImportType() != null && getFileType() != null && isNotBlank(getDateFormat())
                && getStartOffset() != null && !getDateLabels().isEmpty() && (fieldModels == null || !fieldModels.isEmpty());
    }

    private static class Handler extends BufferedBeanModelHandler<ImportFile> {
        public Handler(ImportFile delegate) {
            super(delegate);
        }

        @Override
        protected boolean isChanged(Object self) {
            ImportFileModel model = (ImportFileModel) self;
            return super.isChanged(self)
                    || !model.deletedFields.isEmpty()
                    || model.fieldModels != null && model.fieldModels.stream().anyMatch(BufferedBeanModel::isChanged)
                    || !model.labelChanges.isEmpty();
        }

        @Override
        protected void resetChanges(Object self) {
            ImportFileModel model = (ImportFileModel) self;
            super.resetChanges(self);
            model.labelChanges.clear();
        }
    }
}
