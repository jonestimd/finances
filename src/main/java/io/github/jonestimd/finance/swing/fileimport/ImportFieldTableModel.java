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

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.swing.fileimport.ImportFieldTableColumnAdapter.AmountFormatColumnAdapter;
import io.github.jonestimd.finance.swing.fileimport.ImportFieldTableColumnAdapter.CategoryColumnAdapter;
import io.github.jonestimd.finance.swing.fileimport.ImportFieldTableColumnAdapter.FieldTypeColumnAdapter;
import io.github.jonestimd.finance.swing.fileimport.ImportFieldTableColumnAdapter.RegionTypeColumnAdapter;
import io.github.jonestimd.swing.table.model.BufferedHeaderDetailTableModel;
import io.github.jonestimd.swing.table.model.DetailAdapter;
import io.github.jonestimd.swing.table.model.EmptyColumnAdapter;

public class ImportFieldTableModel extends BufferedHeaderDetailTableModel<ImportField> {
    public static final int FIELD_TYPE_AMOUNT_FORMAT_INDEX = 1;
    public static final int PAGE_REGION_INDEX = 4;

    public ImportFieldTableModel(ImportFileModel importFile) {
        super(new FieldDetailAdapter(), ImmutableList.of(
                ImportFieldTableColumnAdapter.LABELS_ADAPTER,
                new FieldTypeColumnAdapter(importFile),
                ImportFieldTableColumnAdapter.NEGATE_ADAPTER,
                ImportFieldTableColumnAdapter.ACCEPT_REGEX_ADAPTER,
                new RegionTypeColumnAdapter(importFile)),
                Collections.singletonList(ImmutableList.of(
                        new CategoryColumnAdapter(importFile),
                        new AmountFormatColumnAdapter(importFile),
                        new EmptyColumnAdapter<>("dummyColumn0", Number.class),
                        ImportFieldTableColumnAdapter.REJECT_REGEX_ADAPTER,
                        ImportFieldTableColumnAdapter.MEMO_ADAPTER)));
        if (importFile != null) importFile.addPropertyChangeListener("importType", event -> {
            for (int i = 0; i < getRowCount(); i++) {
                fireTableCellUpdated(i, FIELD_TYPE_AMOUNT_FORMAT_INDEX);
            }
        });
    }

    @Override
    public void fireTableCellUpdated(int rowIndex, int columnIndex) {
        if (!isSubRow(rowIndex) && columnIndex == FIELD_TYPE_AMOUNT_FORMAT_INDEX) {
            validateBean(rowIndex);
            for (int i = 0; i < getRowCount(); i += 2) {
                if (i != rowIndex && !isPendingDelete(i)) super.fireTableCellUpdated(i, columnIndex);
            }
        }
        else super.fireTableCellUpdated(rowIndex, columnIndex);
    }

    private void validateBean(int rowIndex) {
        for (int i = 0; i < 2; i++) {
            for (int c = 0; c < getColumnCount(); c++) super.fireTableCellUpdated(rowIndex + i, c);
        }
    }

    public void validatePageRegions() {
        for (int i = 0; i < getBeanCount(); i++) {
            fireTableCellUpdated(i*2, PAGE_REGION_INDEX);
        }
    }

    private static class FieldDetailAdapter implements DetailAdapter<ImportField> {
        @Override
        public int getDetailCount(ImportField group) {
            return 1;
        }

        @Override
        public List<?> getDetails(ImportField group, int subRowTypeIndex) {
            return Collections.singletonList(group);
        }

        @Override
        public Object getDetail(ImportField group, int detailIndex) {
            return group;
        }

        @Override
        public int detailIndex(ImportField group, Object detail) {
            return 0;
        }

        @Override
        public int getDetailTypeIndex(ImportField group, int detailIndex) {
            return 0;
        }

        @Override
        public int appendDetail(ImportField group) {
            return 1;
        }

        @Override
        public Object removeDetail(ImportField group, int index) {
            return group;
        }

        @Override
        public void removeDetail(ImportField group, Object detail) {
        }
    }
}
