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
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.swing.BorderFactory;
import io.github.jonestimd.swing.ButtonBarFactory;
import io.github.jonestimd.swing.SettingsPersister;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.model.ChangeBufferTableModel;
import io.github.jonestimd.swing.table.model.ValidatedTableModel;

public class ImportTablePanel<T, M extends ChangeBufferTableModel<T> & ValidatedTableModel> extends ValidatedTabPanel {
    protected final DecoratedTable<T, M> table;
    private final Supplier<T> itemFactory;
    private final JComponent buttonBar;
    private final Action deleteAction;
    private final TableModelListener validationListener = (event) -> setTabForeground();

    public ImportTablePanel(FileImportsDialog owner, String actionPrefix, DecoratedTable<T, M> table,
            Supplier<M> modelGetter, Supplier<T> itemFactory) {
        super(new BorderLayout(BorderFactory.GAP, BorderFactory.GAP));
        this.itemFactory = itemFactory;
        this.table = table;
        setBorder(BorderFactory.panelBorder());
        setPreferredSize(new Dimension(800, 300));
        Action addAction = owner.actionFactory.newAction(actionPrefix + ".add", this::addItem);
        deleteAction = owner.actionFactory.newAction(actionPrefix + ".delete", this::deleteItem);
        deleteAction.setEnabled(false);
        add(new JScrollPane(table), BorderLayout.CENTER);
        buttonBar = new ButtonBarFactory().alignRight().add(addAction, deleteAction).get();
        add(buttonBar, BorderLayout.SOUTH);
        owner.getModel().addSelectionListener((oldFile, newFile) -> setTableModel(modelGetter.get()));
        setTableModel(modelGetter.get());
        table.getSelectionModel().addListSelectionListener(event -> deleteAction.setEnabled(table.getSelectedRowCount() > 0));
    }

    protected void addToButtonBar(JComponent component, int index) {
        buttonBar.add(component, index);
    }

    protected void addAction(Action action) {
        buttonBar.add(new JButton(action));
    }

    public void setTableModel(M model) {
        table.getModel().removeTableModelListener(validationListener);
        table.setModel(model);
        model.addTableModelListener(validationListener);
        setTabForeground();
    }

    private void addItem(ActionEvent event) {
        table.getModel().queueAdd(itemFactory.get());
    }

    private void deleteItem(ActionEvent event) {
        if (table.isEditing()) table.editingStopped(null);
        table.getSelectedItems().forEach(row -> table.getModel().queueDelete(row));
    }

    protected boolean isNoErrors() {
        return table.getModel().isNoErrors();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SettingsPersister.saveSettings(table);
    }
}
