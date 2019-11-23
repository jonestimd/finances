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

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.swing.BufferedBeanModel;
import io.github.jonestimd.finance.swing.BufferedBeanModelHandler;
import io.github.jonestimd.swing.table.model.BufferedBeanListTableModel;
import io.github.jonestimd.util.Streams;
import javassist.util.proxy.ProxyFactory;

import static org.apache.commons.lang.StringUtils.*;

public abstract class ImportFileModel extends ImportFile implements BufferedBeanModel<ImportFile> {
    public static final String CHANGED_PROPERTY = BufferedBeanModelHandler.CHANGED_PROPERTY;
    private static final ProxyFactory factory;

    static {
        factory = new ProxyFactory();
        factory.setSuperclass(ImportFileModel.class);
    }

    public static ImportFileModel create(ImportFile importFile) {
        try {
            ImportFileModel model = (ImportFileModel) factory.create(new Class[]{}, new Object[]{}, new Handler(importFile));
            model.init();
            return model;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean singlePayee;
    private ImportFieldTableModel importFieldTableModel;
    private PageRegionTableModel pageRegionTableModel = new PageRegionTableModel();
    private ImportTransactionTypeTableModel categoryTableModel = new ImportTransactionTypeTableModel();
    private ImportPayeeTableModel payeeTableModel = new ImportPayeeTableModel();
    private ImportSecurityTableModel securityTableModel = new ImportSecurityTableModel();

    protected ImportFileModel() {
    }

    protected void init() {
        singlePayee = getPayee() != null;
        importFieldTableModel = new ImportFieldTableModel(this);
        importFieldTableModel.setBeans(getBean().getFields());
        importFieldTableModel.addTableModelListener(event -> firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
        pageRegionTableModel.setBeans(getBean().getPageRegions());
        pageRegionTableModel.addTableModelListener(event -> firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
        Stream.concat(getBean().getImportCategories().stream(), getBean().getImportTransfers().stream())
                .map(ImportTransactionTypeModel::new).forEach(categoryTableModel::addRow);
        categoryTableModel.addTableModelListener(event -> firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
        payeeTableModel.setBeans(Streams.map(getBean().getPayeeMap().entrySet(), ImportMapping::new));
        payeeTableModel.addTableModelListener(event -> firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
        securityTableModel.setBeans(Streams.map(getBean().getSecurityMap().entrySet(), ImportMapping::new));
        securityTableModel.addTableModelListener(event -> firePropertyChange(CHANGED_PROPERTY, null, isChanged()));
    }

    public boolean isSinglePayee() {
        return singlePayee;
    }

    public void setSinglePayee(boolean singlePayee) {
        boolean oldValue = this.singlePayee;
        this.singlePayee = singlePayee;
        firePropertyChange("singlePayee", oldValue, singlePayee);
    }

    private void resetNestedChanges() {
        importFieldTableModel.revert();
        pageRegionTableModel.revert();
        categoryTableModel.revert();
        payeeTableModel.revert();
        securityTableModel.revert();
    }

    public ImportFieldTableModel getImportFieldTableModel() {
        return importFieldTableModel;
    }

    public PageRegionTableModel getPageRegionTableModel() {
        return pageRegionTableModel;
    }

    public ImportTransactionTypeTableModel getCategoryTableModel() {
        return categoryTableModel;
    }

    public ImportPayeeTableModel getPayeeTableModel() {
        return payeeTableModel;
    }

    public ImportSecurityTableModel getSecurityTableModel() {
        return securityTableModel;
    }

    private void commitTables() {
        // TODO commitTable(importFieldTableModel, getBean().getFields());
        commitTable(pageRegionTableModel, getBean().getPageRegions());
        commitTable(payeeTableModel, this::toMap, getBean()::setPayeeMap);
        commitTable(securityTableModel, this::toMap, getBean()::setSecurityMap);
        commitTransactionTypes();
    }

    private <T, M extends BufferedBeanListTableModel<T>> void commitTable(M tableModel, Collection<T> beans) {
        if (tableModel.isChanged()) {
            beans.removeAll(tableModel.getPendingDeletes());
            tableModel.commit();
        }
    }

    private <T> Map<String, T> toMap(BufferedBeanListTableModel<ImportMapping<T>> tableModel) {
        return tableModel.getBeans().stream().collect(Collectors.toMap(ImportMapping::getAlias, ImportMapping::getBean));
    }

    private <T, M extends BufferedBeanListTableModel<T>, V> void commitTable(M tableModel, Function<M, V> getter, Consumer<V> setter) {
        if (tableModel.isChanged()) {
            tableModel.commit();
            setter.accept(getter.apply(tableModel));
        }
    }

    private void commitTransactionTypes() {
        if (categoryTableModel.isChanged()) {
            categoryTableModel.commit();
            getBean().setImportCategories(categoryTableModel.getCategories());
            getBean().setImportTransfers(categoryTableModel.getTransfers());
        }
    }

    protected abstract void firePropertyChange(String property, Object oldValue, Object newValue);

    public boolean isValid() {
        return isNotBlank(getName()) && getImportType() != null && getFileType() != null && isNotBlank(getDateFormat())
                && importFieldTableModel.isNoErrors() && pageRegionTableModel.isNoErrors() && categoryTableModel.isNoErrors()
                && payeeTableModel.isNoErrors() && securityTableModel.isNoErrors() && getStartOffset() != null;
    }

    private static class Handler extends BufferedBeanModelHandler<ImportFile> {
        public Handler(ImportFile delegate) {
            super(delegate);
        }

        @Override
        protected boolean isChanged(Object self) {
            ImportFileModel model = (ImportFileModel) self;
            return super.isChanged(self)
                    || model.isNew()
                    || model.importFieldTableModel.isChanged()
                    || model.pageRegionTableModel.isChanged()
                    || model.categoryTableModel.isChanged()
                    || model.payeeTableModel.isChanged()
                    || model.securityTableModel.isChanged();
        }

        @Override
        protected void commitChanges(Object self) {
            ImportFileModel model = (ImportFileModel) self;
            super.commitChanges(self);
            model.commitTables();
        }

        @Override
        protected void resetChanges(Object self) {
            ImportFileModel model = (ImportFileModel) self;
            super.resetChanges(self);
            model.resetNestedChanges();
        }
    }
}
