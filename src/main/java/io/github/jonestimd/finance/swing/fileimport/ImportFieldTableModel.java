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

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;

public class ImportFieldTableModel extends ValidatedBeanListTableModel<ImportField> {
    public static final int FIELD_TYPE_INDEX = 1;
    public static final int AMOUNT_FORMAT_INDEX = 2;
    public static final int PAGE_REGION_INDEX = 6;

    public ImportFieldTableModel(ImportFileModel importFile) {
        super(ImmutableList.of(
            ImportFieldTableColumnAdapter.LABELS_ADAPTER,
            new ImportFieldTableColumnAdapter.FieldTypeColumnAdapter(importFile),
            new ImportFieldTableColumnAdapter.AmountFormatColumnAdapter(importFile),
            ImportFieldTableColumnAdapter.NEGATE_ADAPTER,
            ImportFieldTableColumnAdapter.ACCEPT_REGEX_ADAPTER,
            ImportFieldTableColumnAdapter.REJECT_REGEX_ADAPTER,
            new ImportFieldTableColumnAdapter.RegionTypeColumnAdapter(importFile),
            ImportFieldTableColumnAdapter.MEMO_ADAPTER
        ));
        importFile.addPropertyChangeListener("importType", event -> {
            for (int i = 0; i < getRowCount(); i++) {
                fireTableCellUpdated(i, FIELD_TYPE_INDEX);
                fireTableCellUpdated(i, AMOUNT_FORMAT_INDEX);
            }
        });
    }

    @Override
    public void fireTableCellUpdated(int rowIndex, int columnIndex) {
        if (columnIndex == FIELD_TYPE_INDEX) fireTableRowsUpdated(rowIndex, rowIndex);
        else super.fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void validatePageRegions() {
        for (int i = 0; i < getRowCount(); i++) {
            fireTableCellUpdated(i, PAGE_REGION_INDEX);
        }
    }
}
