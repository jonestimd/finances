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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;

import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.validation.RequiredValidator;
import io.github.jonestimd.swing.validation.ValidatedTextField;
import io.github.jonestimd.util.Streams;

public class LabelsPanel extends Box {
    private final List<Row> rows = new ArrayList<>();
    private final Action addAction = new AbstractAction("+") {
        @Override
        public void actionPerformed(ActionEvent event) {
            addRow();
            resizeWindow();
        }
    };

    private final Action removeAction = new AbstractAction("\u2212") {
        @Override
        public void actionPerformed(ActionEvent event) {
            Row row = (Row) ((JButton) event.getSource()).getParent();
            rows.remove(row);
            remove(row);
            resizeWindow();
        }
    };

    public LabelsPanel() {
        super(BoxLayout.Y_AXIS);
        addRow(new JTextField(), addAction);
    }

    public LabelsPanel(String requiredMessage) {
        super(BoxLayout.Y_AXIS);
        addRow(new ValidatedTextField(new RequiredValidator(requiredMessage)), addAction);
    }

    private void addRow() {
        addRow(new JTextField(), removeAction);
    }

    private void addRow(JTextField labelField, Action removeAction) {
        Row row = new Row(labelField, removeAction);
        rows.add(row);
        add(row);
    }

    private void resizeWindow() {
        revalidate();
        ComponentTreeUtils.findAncestor(LabelsPanel.this, JDialog.class).pack();
    }

    public List<String> getLabels() {
        return Streams.map(rows, (row) -> row.labelField.getText());
    }

    public void setLabels(List<String> labels) {
        int count = Math.max(labels.size(), 1);
        if (count > rows.size()) {
            for (int i = rows.size(); i < count; i++) addRow();
        }
        else if (count < rows.size()) {
            for (int i = rows.size()-1; i >= count; i--) {
                rows.remove(i);
                remove(i);
            }
        }
        if (labels.isEmpty()) rows.get(0).labelField.setText("");
        for (int i = 0; i < labels.size(); i++) {
            rows.get(i).labelField.setText(labels.get(i));
        }
    }

    private class Row extends Box {
        protected final JTextField labelField;

        private Row(JTextField labelField, Action action) {
            super(BoxLayout.X_AXIS);
            this.labelField = labelField;
            add(labelField);
            add(new JButton(action));
        }
    }
}
