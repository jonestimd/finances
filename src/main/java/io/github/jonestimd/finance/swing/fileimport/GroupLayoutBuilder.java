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

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import io.github.jonestimd.swing.LabelBuilder;

public class GroupLayoutBuilder {
    private final GroupLayout layout;
    private final SequentialGroup horizontalGroup;
    private final List<ParallelGroup> columnGroups = new ArrayList<>();
    private final SequentialGroup verticalGroup;
    private final List<ParallelGroup> rowGroups = new ArrayList<>();
    private final ResourceBundle bundle;
    private final String resourcePrefix;
    private int row = 0;
    private int column = 0;
    private final int columnCount;

    public GroupLayoutBuilder(Container container, ResourceBundle bundle, String resourcePrefix, int columnCount) {
        layout = new GroupLayout(container);
        container.setLayout(layout);
        layout.setHorizontalGroup(horizontalGroup = layout.createSequentialGroup());
        layout.setVerticalGroup(verticalGroup = layout.createSequentialGroup());
        layout.setAutoCreateGaps(true);
        this.bundle = bundle;
        this.resourcePrefix = resourcePrefix;
        this.columnCount = columnCount;
    }

    private void addColumnGroup(ParallelGroup group) {
        columnGroups.add(group);
        horizontalGroup.addGroup(group);
    }

    private void addRowGroup(ParallelGroup group) {
        rowGroups.add(group);
        verticalGroup.addGroup(group);
    }

    public GroupLayoutBuilder addCheckBox(JCheckBox component) {
        return this.addCheckBox(component, 0);
    }

    public GroupLayoutBuilder addCheckBox(JCheckBox component, int rowOffset) {
        ensureRow();
        row += rowOffset;
        ensureGroups();
        columnGroups.get(column++).addComponent(component);
        rowGroups.get(row).addComponent(component);
        row -= rowOffset;
        return this;
    }

    public GroupLayoutBuilder addField(String labelKey, JComponent component) {
        JLabel label = new LabelBuilder().mnemonicAndName(bundle.getString(resourcePrefix+labelKey)).forComponent(component).get();
        return addField(label, component);
    }

    public GroupLayoutBuilder addField(JLabel label, JComponent component) {
        addLabel(label, component);
        ensureRowAndGroups();
        columnGroups.get(column++).addComponent(component);
        rowGroups.get(row--).addComponent(component);
        return this;
    }

    public GroupLayoutBuilder overlayField(JComponent component) {
        columnGroups.get(column-1).addComponent(component);
        rowGroups.get(row+1).addComponent(component);
        return  this;
    }

    private void addLabel(JLabel label, JComponent component) {
        ensureRowAndGroups();
        columnGroups.get(column).addComponent(label);
        rowGroups.get(row++).addComponent(label);
    }

    private void ensureRowAndGroups() {
        ensureRow();
        ensureGroups();
    }

    private void ensureGroups() {
        while (columnGroups.size() <= column) {
            addColumnGroup(layout.createParallelGroup(Alignment.LEADING));
        }
        while (rowGroups.size() <= row) {
            addRowGroup(layout.createParallelGroup(Alignment.BASELINE));
        }
    }

    private void ensureRow() {
        if (column >= columnCount) nextRow();
    }

    /**
     * Skip to the beginning of the next empty row.
     */
    public GroupLayoutBuilder nextRow() {
        column = 0;
        row = rowGroups.size();
        return this;
    }

    public GroupLayoutBuilder nextColumn() {
        column++;
        return this;
    }

    public GroupLayout getLayout() {
        return layout;
    }
}
