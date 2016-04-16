// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;

import com.google.common.collect.Ordering;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.operations.Merge;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.swing.action.DialogAction;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.model.BufferedBeanListTableModel;
import io.github.jonestimd.util.JavaPredicates;
import io.github.jonestimd.util.Streams;

public class MergeAction<TableBean, DialogBean extends UniqueId<Long>> extends DialogAction {
    private final Class<DialogBean> beanClass;
    private final DecoratedTable<TableBean, ? extends BufferedBeanListTableModel<TableBean>> table;
    private final SelectionDialog<DialogBean> dialog;
    private final Function<TableBean, DialogBean> toDialogBean;
    private final Merge<DialogBean> merge;
    private final DomainEventPublisher domainEventPublisher;
    private final List<DialogBean> toMerge = new ArrayList<>();
    private final Ordering<DialogBean> listOrdering;
    private final Predicate<List<TableBean>> isDisabled;
    private List<DialogBean> deleted;

    public MergeAction(Class<DialogBean> beanClass, String resourceGroup, DecoratedTable<TableBean, ? extends BufferedBeanListTableModel<TableBean>> table, Format listFormat,
                       Function<TableBean, DialogBean> toDialogBean, Merge<DialogBean> merge, DomainEventPublisher domainEventPublisher) {
        this(beanClass, resourceGroup, table, listFormat, toDialogBean, merge, domainEventPublisher, JavaPredicates.alwaysFalse());
    }

    public MergeAction(Class<DialogBean> beanClass, String resourceGroup, DecoratedTable<TableBean, ? extends BufferedBeanListTableModel<TableBean>> table, Format listFormat,
                       Function<TableBean, DialogBean> toDialogBean, Merge<DialogBean> merge, DomainEventPublisher domainEventPublisher, Predicate<List<TableBean>> isDisabled) {
        super(BundleType.LABELS.get(), "action." + resourceGroup);
        this.beanClass = beanClass;
        this.table = table;
        this.dialog = new SelectionDialog<>(this.table, "dialog." + resourceGroup + ".", "listLabel", listFormat);
        this.toDialogBean = toDialogBean;
        this.merge = merge;
        this.domainEventPublisher = domainEventPublisher;
        this.listOrdering = Ordering.natural().onResultOf(listFormat::format);
        this.isDisabled = isDisabled;
        table.getSelectionModel().addListSelectionListener(this::selectionChanged);
    }

    private void selectionChanged(ListSelectionEvent event) {
        List<TableBean> selectedItems = table.getSelectedItems();
        selectedItems.removeAll(table.getModel().getPendingDeletes());
        setEnabled(selectedItems.size() > 1 && !isDisabled.test(selectedItems));
    }

    @Override
    protected void loadDialogData() {
        toMerge.clear();
        List<TableBean> selectedItems = table.getSelectedItems();
        selectedItems.removeAll(table.getModel().getPendingDeletes());
        toMerge.addAll(Streams.map(selectedItems, toDialogBean));
        Collections.sort(toMerge, listOrdering);
    }

    @Override
    protected boolean displayDialog(JComponent owner) {
        dialog.show(toMerge);
        return !dialog.isCancelled();
    }

    @Override
    protected void saveDialogData() {
        toMerge.remove(dialog.getSelectedItem());
        deleted = merge.merge(toMerge, dialog.getSelectedItem());
    }

    @Override
    protected void setSaveResultOnUI() {
        domainEventPublisher.publishEvent(new DomainEvent<>(this, toMerge, dialog.getSelectedItem(), beanClass));
        domainEventPublisher.publishEvent(new DomainEvent<>(this, EventType.DELETED, deleted, beanClass));
    }
}
