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

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.fileimport.PageRegion;
import io.github.jonestimd.swing.table.model.ColumnAdapter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;

public class PageRegionTableModel extends ValidatedBeanListTableModel<PageRegion> {
    private static final List<ColumnAdapter<PageRegion, ?>> COLUMN_ADAPTERS = ImmutableList.of(
        PageRegionColumnAdapter.NAME_ADAPTER,
        PageRegionColumnAdapter.TOP_ADAPTER,
        PageRegionColumnAdapter.BOTTOM_ADAPTER,
        PageRegionColumnAdapter.LABEL_LEFT_ADAPTER,
        PageRegionColumnAdapter.LABEL_RIGHT_ADAPTER,
        PageRegionColumnAdapter.VALUE_LEFT_ADAPTER,
        PageRegionColumnAdapter.VALUE_RIGHT_ADAPTER);

    public PageRegionTableModel() {
        super(ImmutableList.<ColumnAdapter<PageRegion, ?>>builder().add(new ColorColumnAdapter()).addAll(COLUMN_ADAPTERS).build());
    }

    public ColorColumnAdapter getColorColumnAdapter() {
        return (ColorColumnAdapter) getColumnAdapter(0);
    }

    public Color getColor(PageRegion region) {
        return getColorColumnAdapter().getValue(region);
    }

    @Override
    public void setBeans(Collection<PageRegion> beans) {
        super.setBeans(beans);
        getColorColumnAdapter().setBeans(beans);
    }

    @Override
    public void queueAdd(PageRegion bean) {
        super.queueAdd(bean);
        getColorColumnAdapter().addBean(bean);
    }

    @Override
    protected void setValue(Object value, int rowIndex, int columnIndex) {
        if (columnIndex > 0) super.setValue(value, rowIndex, columnIndex);
        else { // bypass change tracker for color changes
            PageRegion pageRegion = getRow(rowIndex);
            getColorColumnAdapter().setValue(pageRegion, (Color) value);
        }
    }

    private static class ColorColumnAdapter implements ColumnAdapter<PageRegion, Color> {
        private static final List<Color> DEFAULT_COLORS = ImmutableList.of(
                Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.PINK, Color.CYAN, Color.ORANGE);

        private final Map<PageRegion, Color> rowColors = new HashMap<>();
        private int nextColor = 0;

        public void setBeans(Collection<PageRegion> pageRegions) {
            rowColors.clear();
            for (PageRegion pageRegion : pageRegions) {
                addBean(pageRegion);
            }
        }

        private void addBean(PageRegion pageRegion) {
            rowColors.put(pageRegion, DEFAULT_COLORS.get(nextColor));
            nextColor = (nextColor + 1)%DEFAULT_COLORS.size();
        }

        @Override
        public String getColumnId() {
            return "color";
        }

        @Override
        public String getResource(String name, String defaultValue) {
            return null;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public Class<? super Color> getType() {
            return Color.class;
        }

        @Override
        public boolean isEditable(PageRegion row) {
            return true;
        }

        @Override
        public Color getValue(PageRegion region) {
            return rowColors.getOrDefault(region, Color.GRAY);
        }

        @Override
        public void setValue(PageRegion region, Color color) {
            rowColors.put(region, color);
        }
    }
}
