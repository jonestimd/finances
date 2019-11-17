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

import java.util.HashSet;
import java.util.List;

import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.finance.swing.BufferedBeanModel;
import io.github.jonestimd.finance.swing.BufferedBeanModelHandler;
import javassist.util.proxy.ProxyFactory;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.fileimport.ImportFieldPanel.*;

public abstract class ImportFieldModel extends ImportField implements BufferedBeanModel<ImportField> {
    public static final String CHANGED_PROPERTY = BufferedBeanModelHandler.CHANGED_PROPERTY;
    private static final ProxyFactory factory;
    private static final String LABEL_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "label.required");
    private static final String DUPLICATE_COLUMNS = LABELS.getString(RESOURCE_PREFIX + "label.duplicateColumns");
    private static final String BLANK_COLUMNS = LABELS.getString(RESOURCE_PREFIX + "label.blankColumns");
    private static final String TYPE_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "type.required");
    private static final String TYPE_AMOUNT_COLUMN_INVALID = LABELS.getString(RESOURCE_PREFIX + "type.amountColumn.invalid");
    private static final String AMOUNT_FORMAT_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "amountFormat.required");
    private static final String FORMAT_COLUMN_INVALID = LABELS.getString(RESOURCE_PREFIX + "amountFormat.invalidColumn");
    private static final String REGION_DELETED = LABELS.getString(RESOURCE_PREFIX + "pageRegion.deleted");

    static {
        factory = new ProxyFactory();
        factory.setSuperclass(ImportFieldModel.class);
    }

    public static ImportFieldModel create(ImportField importField, ImportFileModel fileModel) {
        try {
            BufferedBeanModelHandler<ImportField> handler = new BufferedBeanModelHandler<>(importField);
            return (ImportFieldModel) factory.create(new Class[]{ImportFileModel.class}, new Object[]{fileModel}, handler);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final ImportFileModel fileModel;

    protected ImportFieldModel(ImportFileModel fileModel) {
        this.fileModel = fileModel;
        fileModel.addPropertyChangeListener("importType", (event) -> firePropertyChange("valid", null, isValid()));
        fileModel.getPageRegionTableModel().addTableModelListener(event -> firePropertyChange("valid", null, isValid()));
    }

    public boolean isValid() {
        return validateLabel(getLabels()) == null && validateType(getType()) == null
                && validateAmountFormat(getAmountFormat()) == null && validateRegion(getRegion()) == null;
    }

    public String validateType(FieldType type) {
        if (type == null) return TYPE_REQUIRED;
        if (fileModel.getImportType() == ImportType.MULTI_DETAIL_ROWS && type == FieldType.AMOUNT) return TYPE_AMOUNT_COLUMN_INVALID;
        return null;
    }

    public String validateAmountFormat(AmountFormat format) {
        if (fileModel.getImportType() == ImportType.MULTI_DETAIL_ROWS || getType() == FieldType.AMOUNT) {
            if (getType() != null && !getType().isTransaction() && format == null) return AMOUNT_FORMAT_REQUIRED;
        }
        else if (format != null) return FORMAT_COLUMN_INVALID;
        return null;
    }

    public static String validateLabel(List<String> columns) {
        if (columns.isEmpty()) return LABEL_REQUIRED;
        if (new HashSet<>(columns).size() < columns.size()) return DUPLICATE_COLUMNS;
        if (!columns.stream().allMatch(ImportFieldModel::isValidColumn)) return BLANK_COLUMNS;
        return null;
    }

    public static boolean isValidColumn(String column) {
        return !column.trim().isEmpty();
    }

    public String validateRegion(PageRegion pageRegion) {
        if (fileModel.getFileType() == FileType.PDF) {
            if (pageRegion != null && fileModel.getPageRegionTableModel().getPendingDeletes().contains(pageRegion)) {
                return REGION_DELETED;
            }
        }
        return null;
    }

    protected abstract void firePropertyChange(String property, Object oldValue, Object newValue);
}
