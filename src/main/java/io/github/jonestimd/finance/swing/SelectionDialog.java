// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.finance.swing;

import java.awt.Window;
import java.text.Format;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import io.github.jonestimd.swing.component.FormatListCellRenderer;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.list.BeanListModel;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class SelectionDialog<T> extends FormDialog {
    private final BeanListModel<T> listModel = new BeanListModel<>();
    private final JList<T> choiceList = new JList<>(listModel);

    public SelectionDialog(JComponent owner, String resourcePrefix, String labelKey, Format format) {
        this((Window) owner.getTopLevelAncestor(), resourcePrefix, labelKey, format);
    }

    public SelectionDialog(Window owner, String resourcePrefix, String labelKey, Format format) {
        super(owner, LABELS.getString(resourcePrefix + "title"), LABELS.get());
        setSaveEnabled(false);
        choiceList.setCellRenderer(new FormatListCellRenderer(format));
        choiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choiceList.getSelectionModel().addListSelectionListener(event -> setSaveEnabled(choiceList.getSelectedIndex() >= 0));
        GridBagBuilder builder = new GridBagBuilder(getFormPanel(), LABELS.get(), resourcePrefix);
        builder.append(labelKey, choiceList);
    }

    /**
     * @return true if an item was selected
     */
    public boolean show(List<T> choices) {
        listModel.setElements(choices);
        pack();
        setVisible(true);
        return ! isCancelled();
    }

    public T getSelectedItem() {
        return choiceList.getSelectedValue();
    }
}