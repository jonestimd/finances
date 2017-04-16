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
package io.github.jonestimd.finance.swing.transaction;

import java.awt.Window;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.ComboBoxDomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.component.EditableComboBoxCellEditor;
import io.github.jonestimd.swing.validation.Validator;

public class TransactionGroupCellEditor extends EditableComboBoxCellEditor<TransactionGroup> {
    private final TransactionGroupOperations transactionGroupOperations;
    // need a strong reference to avoid garbage collection
    @SuppressWarnings("FieldCanBeLocal")
    private final ComboBoxDomainEventListener<Long, TransactionGroup> domainEventListener = new ComboBoxDomainEventListener<>(getComboBox());

    public TransactionGroupCellEditor(final ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher) {
        super(new TransactionGroupFormat(), Validator.empty(), BundleType.LABELS.getString("table.transaction.detail.group.initialize"));
        this.transactionGroupOperations = serviceLocator.getTransactionGroupOperations();
        domainEventPublisher.register(TransactionGroup.class, domainEventListener);
    }

    protected List<TransactionGroup> getComboBoxValues() {
        return transactionGroupOperations.getAllTransactionGroups();
    }

    protected TransactionGroup saveItem(TransactionGroup item) {
        TransactionGroupDialog dialog = new TransactionGroupDialog((Window) getComboBox().getTopLevelAncestor(), getComboBoxModel());
        try {
            return dialog.show(item) ? item : null;
        } finally {
            dialog.dispose();
        }
    }
}