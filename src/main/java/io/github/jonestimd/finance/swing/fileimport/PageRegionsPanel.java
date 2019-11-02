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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.finance.swing.BorderFactory;
import io.github.jonestimd.swing.ButtonBarFactory;
import io.github.jonestimd.swing.table.ColorTableCellEditor;
import io.github.jonestimd.swing.table.ColorTableCellRenderer;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.TableFactory;
import io.github.jonestimd.swing.table.TableInitializer;

public class PageRegionsPanel extends JPanel {
    private final DecoratedTable<PageRegion, PageRegionTableModel> table;

    private JDialog previewDialog;

    public PageRegionsPanel(FileImportsDialog owner, TableFactory tableFactory) {
        super(new BorderLayout(BorderFactory.GAP, BorderFactory.GAP));
        setBorder(BorderFactory.panelBorder());
        setPreferredSize(new Dimension(550, 100));
        table = tableFactory.validatedTableBuilder(new PageRegionTableModel()).get();
        table.setDefaultRenderer(Color.class, new ColorTableCellRenderer());
        table.setDefaultEditor(Color.class, new ColorTableCellEditor());
        table.setDefaultRenderer(Float.class, InfinityRenderer.POSITIVE_RENDERER);
        table.setDefaultEditor(Float.class, new RegionCoordinateEditor(table));
        initializeColumns();
        Action addRegionAction = owner.actionFactory.newAction("pdfRegion.add", this::addRegion);
        Action deleteRegionAction = owner.actionFactory.newAction("pdfRegion.delete", this::deleteRegion);
        Action pdfPreviewAction = owner.actionFactory.newAction("pdfPreview", this::showPreview);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new ButtonBarFactory().alignRight().add(addRegionAction, deleteRegionAction, pdfPreviewAction).get(), BorderLayout.SOUTH);
        owner.getModel().addSelectionListener((model, importFile) -> {
            table.setModel(model.getRegionTableModel());
            initializeColumns();
        });
    }

    private void initializeColumns() {
        TableInitializer.setFixedWidth(table.getColumnModel().getColumn(0), table.getRowHeight());
        table.getColumn(PageRegionColumnAdapter.BOTTOM_ADAPTER).setCellRenderer(InfinityRenderer.NEGATIVE_RENDERER);
        table.getColumn(PageRegionColumnAdapter.LABEL_LEFT_ADAPTER).setCellRenderer(InfinityRenderer.NEGATIVE_RENDERER);
        table.getColumn(PageRegionColumnAdapter.VALUE_LEFT_ADAPTER).setCellRenderer(InfinityRenderer.NEGATIVE_RENDERER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        previewDialog = new PdfPreviewDialog((Window) getTopLevelAncestor(), table);
    }

    private void showPreview(ActionEvent e) {
        if (!previewDialog.isVisible()) {
            previewDialog.pack();
            Rectangle bounds = getTopLevelAncestor().getBounds();
            Rectangle screen = getGraphicsConfiguration().getBounds();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
            Dimension size = previewDialog.getSize();
            int x = bounds.x + bounds.width;
            int y = Math.min(bounds.y, screen.y + screen.height - size.height - screenInsets.bottom);
            if (x + size.width > screen.x + screen.width - screenInsets.right) x = bounds.x - size.width;
            previewDialog.setLocation(x, y);
            previewDialog.setVisible(true);
        }
    }

    private void addRegion(ActionEvent event) {
        // TODO
    }

    private void deleteRegion(ActionEvent event) {
        // TODO
    }
}
