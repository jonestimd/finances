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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.swing.table.DecoratedTable;

public class PageRegionsPanel extends JPanel {
    private final PageRegionTableModel tableModel = new PageRegionTableModel();
    private final DecoratedTable<PageRegion, PageRegionTableModel> table = new DecoratedTable<>(tableModel);

    public PageRegionsPanel() {
        super(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5)); // TODO get from resource bundle
        table.setPreferredScrollableViewportSize(new Dimension(535, 100));
        add(new JScrollPane(table), BorderLayout.CENTER);
        // TODO buttons: add, delete, preview
    }

    public void setImportFile(ImportFile importFile) {
        tableModel.setBeans(importFile.getPageRegions());
    }
}
