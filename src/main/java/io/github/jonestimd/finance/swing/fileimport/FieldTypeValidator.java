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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.fileimport.FileImportsModel.*;

public class FieldTypeValidator {
    private static final String FIELDS_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "importField.required");
    private static final String DATE_LABEL_REQUIRED = LABELS.getString(RESOURCE_PREFIX + "importField.labels.date.required");
    private static final String DATE_LABEL_LIMIT = LABELS.getString(RESOURCE_PREFIX + "importField.labels.date.limit");
    private static final String PAYEE_LABEL_LIMIT = LABELS.getString(RESOURCE_PREFIX + "importField.labels.payee.limit");
    private static final String PAYEE_LABEL_INVALID = LABELS.getString(RESOURCE_PREFIX + "importField.labels.payee.invalid");
    private static final String SECURITY_LABEL_LIMIT = LABELS.getString(RESOURCE_PREFIX + "importField.labels.security.limit");
    private static final Map<FieldType, String> LIMIT_ERRORS = ImmutableMap.of(
            FieldType.DATE, DATE_LABEL_LIMIT,
            FieldType.PAYEE, PAYEE_LABEL_LIMIT,
            FieldType.SECURITY, SECURITY_LABEL_LIMIT);

    private final ImportFileModel fileModel;

    public FieldTypeValidator(ImportFileModel fileModel) {
        this.fileModel = fileModel;
    }

    private ImportFieldTableModel getTableModel() {
        return fileModel.getImportFieldTableModel();
    }

    public String validateImportFields() {
        List<String> messages = new ArrayList<>();
        if (getTableModel().getBeans().size() == getTableModel().getPendingDeletes().size()) messages.add(FIELDS_REQUIRED);
        List<ImportField> dateFields = getFields(FieldType.DATE);
        if (dateFields.isEmpty()) messages.add(DATE_LABEL_REQUIRED);
        if (dateFields.size() > 1) messages.add(DATE_LABEL_LIMIT);
        List<ImportField> payeeFields = getFields(FieldType.PAYEE);
        if (fileModel.isSinglePayee() && !payeeFields.isEmpty()) messages.add(PAYEE_LABEL_INVALID);
        if (payeeFields.size() > 1) messages.add(PAYEE_LABEL_LIMIT);
        if (getFields(FieldType.SECURITY).size() > 1) messages.add(SECURITY_LABEL_LIMIT);
        return messages.isEmpty() ? null : String.join("\n", messages);
    }

    public String getValidationMessages(ImportField field) {
        if (fileModel.isSinglePayee() && field.getType() == FieldType.PAYEE) return PAYEE_LABEL_INVALID;
        if (field.getType().isTransaction()) {
            if (getFields(field.getType()).size() > 1) return LIMIT_ERRORS.get(field.getType());
        }
        return null;
    }

    private List<ImportField> getFields(FieldType type) {
        List<ImportField> importFields = Streams.filter(getTableModel().getBeans(), (field) -> field.getType() == type);
        importFields.removeAll(getTableModel().getPendingDeletes());
        return importFields;
    }

}
