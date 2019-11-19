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
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.FileType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.swing.table.model.FunctionColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedColumnAdapter;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ImportFieldTableColumnAdapter<V> extends FunctionColumnAdapter<ImportField, V> {
    public static final String RESOURCE_PREFIX = "table.importField.column.";
    private static final String LABEL_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "labels.required");
    private static final String DUPLICATE_COLUMNS = LABELS.getString(RESOURCE_PREFIX + "labels.duplicateColumns");
    private static final String BLANK_COLUMNS = LABELS.getString(RESOURCE_PREFIX + "labels.blankColumns");
    private static final String TYPE_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "type.required");
    private static final String TYPE_AMOUNT_COLUMN_INVALID = LABELS.getString(RESOURCE_PREFIX + "type.amountColumn.invalid");
    private static final String AMOUNT_FORMAT_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "amountFormat.required");
    private static final String FORMAT_COLUMN_INVALID = LABELS.getString(RESOURCE_PREFIX + "amountFormat.invalidColumn");
    private static final String REGION_DELETED = LABELS.getString(RESOURCE_PREFIX + "pageRegion.deleted");

    protected ImportFieldTableColumnAdapter(String columnId, Class<? super V> valueType,
            Function<ImportField, V> getter, BiConsumer<ImportField, V> setter) {
        super(LABELS.get(), RESOURCE_PREFIX, columnId, valueType, getter, setter);
    }

    public static final ValidatedFieldColumnAdapter<List<String>> LABELS_ADAPTER =
            new ValidatedFieldColumnAdapter<List<String>>("labels", List.class, ImportField::getLabels, ImportField::setLabels) {
                @Override
                public String validate(int selectedIndex, List<String> columns, List<? extends ImportField> beans) {
                    if (columns.isEmpty()) return LABEL_REQUIRED;
                    if (new HashSet<>(columns).size() < columns.size()) return DUPLICATE_COLUMNS;
                    if (columns.stream().anyMatch(column -> column.trim().isEmpty())) return BLANK_COLUMNS;
                    return null;
                }
            };

    public static final DetailFieldColumnAdapter<Boolean> NEGATE_ADAPTER =
            new DetailFieldColumnAdapter<>("negateAmount", Boolean.class, ImportField::isNegate, ImportField::setNegate);

    public static final DetailFieldColumnAdapter<String> ACCEPT_REGEX_ADAPTER =
            new DetailFieldColumnAdapter<>("acceptRegex", String.class, ImportField::getAcceptRegex, ImportField::setAcceptRegex);

    public static final DetailFieldColumnAdapter<String> REJECT_REGEX_ADAPTER =
            new DetailFieldColumnAdapter<>("rejectRegex", String.class, ImportField::getIgnoredRegex, ImportField::setIgnoredRegex);

    public static final DetailFieldColumnAdapter<String> MEMO_ADAPTER =
            new DetailFieldColumnAdapter<>("memo", String.class, ImportField::getMemo, ImportField::setMemo);

    public static class FieldTypeColumnAdapter extends ValidatedFieldColumnAdapter<FieldType> {
        private final ImportFileModel importFile;
        private final FieldTypeValidator validator;

        public FieldTypeColumnAdapter(ImportFileModel importFile) {
            super("type", FieldType.class, ImportField::getType, ImportField::setType);
            this.importFile = importFile;
            this.validator = new FieldTypeValidator(importFile);
        }

        @Override
        public String validate(int selectedIndex, FieldType type, List<? extends ImportField> beans) {
            if (type == null) return TYPE_REQUIRED;
            if (importFile.getImportType() == ImportType.MULTI_DETAIL_ROWS && type == FieldType.AMOUNT) return TYPE_AMOUNT_COLUMN_INVALID;
            return validator.getValidationMessages(beans.get(selectedIndex));
        }
    }

    public static class AmountFormatColumnAdapter extends ValidatedDetailFieldColumnAdapter<AmountFormat> {
        private final ImportFileModel importFile;

        public AmountFormatColumnAdapter(ImportFileModel importFile) {
            super("amountFormat",AmountFormat .class, ImportField::getAmountFormat,ImportField::setAmountFormat);
            this.importFile = importFile;
        }

        @Override
        public String validate(int selectedIndex, AmountFormat format, List<? extends ImportField> beans) {
            FieldType type = beans.get(selectedIndex).getType();
            if (importFile.getImportType() == ImportType.MULTI_DETAIL_ROWS || type == FieldType.AMOUNT || type == FieldType.ASSET_QUANTITY) {
                if (type != null && !type.isTransaction() && format == null) return AMOUNT_FORMAT_REQUIRED;
            }
            else if (format != null) return FORMAT_COLUMN_INVALID;
            return null;
        }
    }

    public static class RegionTypeColumnAdapter extends ValidatedFieldColumnAdapter<PageRegion> {
        private final ImportFileModel importFile;

        public RegionTypeColumnAdapter(ImportFileModel importFile) {
            super("pageRegion", PageRegion.class, ImportField::getRegion, ImportField::setRegion);
            this.importFile = importFile;
        }

        @Override
        public boolean isEditable(ImportField importField) {
            return super.isEditable(importField) && importFile.getFileType() == FileType.PDF;
        }

        @Override
        public String validate(int selectedIndex, PageRegion pageRegion, List<? extends ImportField> beans) {
            if (importFile.getFileType() == FileType.PDF) {
                if (pageRegion != null && importFile.getPageRegionTableModel().getPendingDeletes().contains(pageRegion)) {
                    return REGION_DELETED;
                }
            }
            return null;
        }
    }

    public static class DetailFieldColumnAdapter<V> extends ImportFieldTableColumnAdapter<V> {
        public DetailFieldColumnAdapter(String columnId, Class<? super V> valueType,
                Function<ImportField, V> getter, BiConsumer<ImportField, V> setter) {
            super(columnId, valueType, getter, setter);
        }

        @Override
        public boolean isEditable(ImportField importField) {
            return super.isEditable(importField) && importField.getType() != null && !importField.getType().isTransaction();
        }

        @Override
        public V getValue(ImportField importField) {
            return isEditable(importField) ? super.getValue(importField) : null;
        }
    }

    public static abstract class ValidatedDetailFieldColumnAdapter<V> extends DetailFieldColumnAdapter<V>
            implements ValidatedColumnAdapter<ImportField, V> {
        protected ValidatedDetailFieldColumnAdapter(String columnId, Class<? super V> valueType,
                Function<ImportField, V> getter, BiConsumer<ImportField, V> setter) {
            super(columnId, valueType, getter, setter);
        }
    }

    public static abstract class ValidatedFieldColumnAdapter<V> extends ImportFieldTableColumnAdapter<V>
            implements ValidatedColumnAdapter<ImportField, V> {
        protected ValidatedFieldColumnAdapter(String columnId, Class<? super V> valueType,
                Function<ImportField, V> getter, BiConsumer<ImportField, V> setter) {
            super(columnId, valueType, getter, setter);
        }
    }
}
