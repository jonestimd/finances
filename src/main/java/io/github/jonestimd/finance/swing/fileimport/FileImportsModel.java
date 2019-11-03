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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.util.Streams;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.apache.log4j.Logger;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportsModel extends BeanListComboBoxModel<ImportFile> {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    public static final String CHANGED_PROPERTY = "changed";

    private static final String NAME_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "name.required");
    private static final String NAME_UNIQUE = LABELS.getString(RESOURCE_PREFIX + "name.unique");
    private static final ProxyFactory proxyFactory;

    static {
        proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(ImportFile.class);
    }

    private final Map<ImportFile, ImportFile> changeProxies = new HashMap<>();
    private final List<ImportFileHandler> changeHandlers = new ArrayList<>();
    private final Map<Long, PageRegionTableModel> regionTableModels = new HashMap<>();
    private final List<BiConsumer<FileImportsModel, ImportFile>> selectionListeners = new ArrayList<>();
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public FileImportsModel(Collection<? extends ImportFile> elements) {
        super(elements);
        for (ImportFile importFile : elements) createProxy(importFile);
        if (elements.isEmpty()) addImport();
        else setSelectedItem(elements.iterator().next());
    }

    private ImportFile createProxy(ImportFile importFile) {
        try {
            ImportFileHandler handler = new ImportFileHandler(importFile);
            changeHandlers.add(handler);
            ImportFile proxy = (ImportFile) proxyFactory.create(new Class[0], new Object[0], handler);
            changeProxies.put(importFile, proxy);
            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            return model;
        });
    }

    @Override
    public void setSelectedItem(Object anItem) {
        super.setSelectedItem(anItem);
        notifySelectionListeners((ImportFile) anItem);
    }

    public void addImport() {
        ImportFile importFile = new ImportFile();
        importFile.setName("");
        importFile.setFields(new HashSet<>());
        importFile.setPageRegions(new HashSet<>());
        addElement(importFile);
        createProxy(importFile);
        setSelectedItem(importFile);
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
        return changeHandlers.stream().anyMatch(ImportFileHandler::isChanged);
    }

    public String getImportName() {
        return changeProxies.get(getSelectedItem()).getName();
    }

    public void setImportName(String name) {
        setProperty(ImportFile::setName, name);
    }

    public ImportType getImportType() {
        return changeProxies.get(getSelectedItem()).getImportType();
    }

    public void setImportType(ImportType importType) {
        setProperty(ImportFile::setImportType, importType);
    }

    public FileType getFileType() {
        return changeProxies.get(getSelectedItem()).getFileType();
    }

    public void setFileType(FileType fileType) {
        setProperty(ImportFile::setFileType, fileType);
    }

    public Account getAccount() {
        return changeProxies.get(getSelectedItem()).getAccount();
    }

    public void setAccount(Account account) {
        setProperty(ImportFile::setAccount, account);
    }

    public Integer getStartOffset() {
        return changeProxies.get(getSelectedItem()).getStartOffset();
    }

    public void setStartOffset(Integer offset) {
        setProperty(ImportFile::setStartOffset, offset);
    }

    public String getDateFormat() {
        return changeProxies.get(getSelectedItem()).getDateFormat();
    }

    public void setDateFormat(String dateFormat) {
        setProperty(ImportFile::setDateFormat, dateFormat);
    }

    public boolean isReconcile() {
        return changeProxies.get(getSelectedItem()).isReconcile();
    }

    public void setReconcile(boolean reconcile) {
        setProperty(ImportFile::setReconcile, reconcile);
    }

    private <T> void setProperty(BiConsumer<ImportFile, T> setter, T value) {
        boolean oldChanged = isChanged();
        setter.accept(changeProxies.get(getSelectedItem()), value);
        changeSupport.firePropertyChange(CHANGED_PROPERTY, oldChanged, isChanged());
    }

    private static class ImportFileHandler implements MethodHandler {
        private static final Map<String, Function<ImportFile, ?>> CHANGE_PROPERTIES = new MapBuilder<String, Function<ImportFile, ?>>()
                .put("Name", ImportFile::getName)
                .put("ImportType", ImportFile::getImportType)
                .put("FileType", ImportFile::getFileType)
                .put("Account", ImportFile::getAccount)
                .put("StartOffset", ImportFile::getStartOffset)
                .put("DateFormat", ImportFile::getDateFormat)
                .put("Reconcile", ImportFile::isReconcile)
                .get();

        private final Map<String, Object> changes = new HashMap<>();
        private final ImportFile delegate;
        private final Logger logger = Logger.getLogger(getClass());

        public ImportFileHandler(ImportFile delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            if (thisMethod.getName().startsWith("get")) {
                String property = thisMethod.getName().substring(3);
                return changes.containsKey(property) ? changes.get(property) : thisMethod.invoke(delegate, args);
            }
            if (thisMethod.getName().startsWith("is")) {
                String property = thisMethod.getName().substring(2);
                return changes.containsKey(property) ? changes.get(property) : thisMethod.invoke(delegate, args);
            }
            if (thisMethod.getName().startsWith("set") && args.length == 1) {
                String property = thisMethod.getName().substring(3);
                if (CHANGE_PROPERTIES.containsKey(property)) {
                    Object originalValue = CHANGE_PROPERTIES.get(property).apply(delegate);
                    if (Objects.equals(originalValue, args[0])) changes.remove(property);
                    else changes.put(property, args[0]);
                }
                else logger.warn("untracked property changed: " + property);
                return null;
            }
            return thisMethod.invoke(delegate, args);
        }

        public boolean isChanged() {
            return !changes.isEmpty();
        }
    }
}
