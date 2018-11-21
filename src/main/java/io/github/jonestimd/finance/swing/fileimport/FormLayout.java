// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.swing.JLabel;

import io.github.jonestimd.swing.LabelBuilder;

// TODO handle right to left container
public class FormLayout<T extends Container> implements LayoutManager2 {
    public static final double MIN_FIELD_LABEL_RATIO = 1.5;
    private final T container;
    private final int columns;
    private final int labelGap;
    private final int fieldGap;
    private final int rowGap;
    private final Set<FormConstraint> componentConstraints = new TreeSet<>();
    private final int columnGroups;

    public FormLayout(T container, int columnGroups, int labelGap, int fieldGap, int rowGap) {
        this.container = container;
        this.columnGroups = columnGroups;
        this.columns = columnGroups*2;
        this.labelGap = labelGap;
        this.fieldGap = fieldGap;
        this.rowGap = rowGap;
        container.setLayout(this);
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof FormConstraint) {
            FormConstraint formConstraint = (FormConstraint) constraints;
            if (formConstraint.column >= columns) throw new IllegalArgumentException("column out of range");
            componentConstraints.add(formConstraint);
        }
        else if (constraints != null) throw new IllegalArgumentException("only FormConstraints are supported");
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container target) {
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        componentConstraints.removeIf(entry -> entry.component == comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            return new LayoutInfo(Component::getPreferredSize).getSize(target.getInsets());
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            return new LayoutInfo(Component::getMinimumSize).getSize(target.getInsets());
        }
    }

    @Override
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int top = insets.top;
            int left = insets.left;
            int right = target.getWidth() - insets.right;
            LayoutInfo info = new LayoutInfo(Component::getMinimumSize, right - left);
            int row = 0, rowHeight = info.getRowHeight(0);
            for (FormConstraint entry : componentConstraints) {
                if (entry.component.isVisible()) {
                    if (entry.row != row) {
                        top += rowHeight + rowGap;
                        rowHeight = info.getRowHeight(entry.row);
                        row = entry.row;
                    }
                    entry.setBounds(info, top, left);
                }
            }
        }
    }

    public T getContainer() {
        return container;
    }

    public FormConstraint getConstraints(Component component) {
        return componentConstraints.stream().filter(constraint -> constraint.component == component)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("component not found"));
    }

    public static <T extends Container> Builder<T> builder(ResourceBundle bundle, String resourcePrefix, T target, int columnGroups) {
        return new Builder<>(bundle, resourcePrefix, target, columnGroups, 5, 10, 10);
    }

    public static <T extends Container> Builder<T> builder(ResourceBundle bundle, String resourcePrefix, T target, int columnGroups, int labelGap, int fieldGap, int rowGap) {
        return new Builder<>(bundle, resourcePrefix, target, columnGroups, labelGap, fieldGap, rowGap);
    }

    public static class FormConstraint implements Comparable<FormConstraint> {
        public final int row;
        public final int column;
        public final boolean fillHeight;
        protected final Component component;

        protected FormConstraint(Component component, int row, int column, boolean fillHeight) {
            this.component = component;
            this.row = row;
            this.column = column;
            this.fillHeight = fillHeight;
        }

        @Override
        public int compareTo(FormConstraint other) {
            int diff = row - other.row;
            if (diff == 0) diff = column - other.column;
            if (diff == 0) diff = component.hashCode() - other.component.hashCode();
            return diff == 0 && component != other.component ? -1 : diff;
        }

        protected void setBounds(FormLayout<?>.LayoutInfo info, int top, int left) {
            Dimension size = component.getPreferredSize();
            int indent = 0, rowHeight = info.getRowHeight(row);
            if ((column & 1) != 1) indent = info.labelWidths[column >> 1] - size.width;
            else size.width = info.fieldWidth;
            if (fillHeight) {
                component.setSize(size.width, rowHeight);
                component.setLocation(left + indent + info.getX(column), top);
            }
            else {
                component.setSize(size.width, size.height);
                component.setLocation(left + indent + info.getX(column), top + (rowHeight - size.height)/2);
            }
        }

        @Override
        public String toString() {
            return "FormConstraint[" + row + "," + column + "]";
        }

    }

    protected class LayoutInfo {
        private final int[] labelWidths = new int[columnGroups];
        private final int maxLabel;
        private int fieldWidth = 0;
        private int[] rowHeights;

        protected LayoutInfo(Function<Component, Dimension> getter) {
            int rows = componentConstraints.stream().mapToInt(cc -> cc.row).max().orElse(0) + 1;
            rowHeights = new int[rows];
            for (FormConstraint entry : componentConstraints) {
                if (entry.component.isVisible()) {
                    Dimension size = getter.apply(entry.component);
                    if ((entry.column & 1) == 0) labelWidths[entry.column >> 1] = Math.max(labelWidths[entry.column >> 1], size.width);
                    else fieldWidth = Math.max(fieldWidth, size.width);
                    rowHeights[entry.row] = Math.max(rowHeights[entry.row], size.height);
                }
            }
            maxLabel = IntStream.of(labelWidths).max().getAsInt();
            fieldWidth = Math.max(fieldWidth, (int) (maxLabel*MIN_FIELD_LABEL_RATIO));
        }

        protected LayoutInfo(Function<Component, Dimension> getter, int width) {
            this(getter);
            width -= (columnGroups - 1)*fieldGap + IntStream.of(labelWidths).sum() + labelGap*columnGroups;
            fieldWidth = (width/columnGroups);
        }

        public Dimension getSize(Insets insets) {
            int labelSum = IntStream.of(labelWidths).sum();
            int columnWidth = labelGap + fieldGap + fieldWidth;
            int height = IntStream.of(rowHeights).reduce(0, (v1, v2) -> v1 + v2) + (rowHeights.length - 1)*rowGap;
            return new Dimension(labelSum + columnWidth*columnGroups + insets.left + insets.right - fieldGap, height + insets.top + insets.bottom);
        }

        public int getRowHeight(int row) {
            return rowHeights[row];
        }

        public int getX(int column) {
            int labelSum = IntStream.of(labelWidths).limit(column + 1 >> 1).sum() + (column & 1)*(labelGap);
            return labelSum + (labelGap + fieldWidth + fieldGap)*(column >> 1);
        }
    }

    public static class Builder<T extends Container> {
        private final ResourceBundle bundle;
        private final String resourcePrefix;
        private final FormLayout<T> layout;
        private int row = 0;
        private int column = 0;

        protected Builder(ResourceBundle bundle, String resourcePrefix, T target, int columnGroups, int labelGap, int fieldGap, int rowGap) {
            this.bundle = bundle;
            this.resourcePrefix = resourcePrefix;
            this.layout = new FormLayout<>(target, columnGroups, labelGap, fieldGap, rowGap);
        }

        public Builder<T> add(JLabel label, Component component) {
            add(label, false);
            return add(component, true);
        }

        public Builder<T> add(String labelKey, Component component) {
            JLabel label = new LabelBuilder().mnemonicAndName(bundle.getString(resourcePrefix + labelKey)).forComponent(component).get();
            return add(label, component);
        }

        public Builder<T> add(Component component, boolean fillHeight) {
            layout.container.add(component, new FormConstraint(component, row, column, fillHeight));
            return nextCell();
        }

        public Builder<T> nextCell() {
            column++;
            return column >= layout.columns ? nextRow() : this;
        }

        public Builder<T> previousCell() {
            column--;
            if (column < 0) {
                column = layout.columns-1;
                row--;
            }
            return this;
        }

        public Builder<T> nextRow() {
            row++;
            column = 0;
            return this;
        }

        public FormLayout<T> layout() {
            return layout;
        }

        public T container() {
            return layout.container;
        }
    }
}
