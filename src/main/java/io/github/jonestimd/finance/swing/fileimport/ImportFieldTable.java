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
import java.util.Collections;
import java.util.List;

import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;

import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.swing.table.MixedRowTable;
import io.github.jonestimd.swing.table.sort.HeaderDetailTableRowSorter;

public class ImportFieldTable extends MixedRowTable<ImportField, ImportFieldTableModel> {
    public ImportFieldTable(ImportFieldTableModel model) {
        super(model);
        setRowSorter(new HeaderDetailTableRowSorter<>(this));
        getRowSorter().setSortKeys(Collections.singletonList(new SortKey(0, SortOrder.ASCENDING)));
    }

    @Override
    public void setModel(TableModel dataModel) {
        boolean sorted = getRowSorter() != null;
        List<? extends SortKey> sortKeys = sorted ? new ArrayList<>(getRowSorter().getSortKeys()) : null;
        super.setModel(dataModel);
        if (sorted) getRowSorter().setSortKeys(sortKeys);
    }
}
