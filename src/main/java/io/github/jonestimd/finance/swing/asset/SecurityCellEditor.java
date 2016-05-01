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
package io.github.jonestimd.finance.swing.asset;

import java.awt.Window;
import java.util.List;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.event.ComboBoxDomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.component.EditableComboBoxCellEditor;
import io.github.jonestimd.swing.validation.Validator;

public class SecurityCellEditor extends EditableComboBoxCellEditor<Security> {
    private final AssetOperations assetOperations;
    private final DomainEventPublisher domainEventPublisher;
    // need a reference to avoid garbage collection
    private final ComboBoxDomainEventListener<Long, Security> domainEventListener = new ComboBoxDomainEventListener<>(getComboBox());

    public SecurityCellEditor(final ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher) {
        super(new SecurityFormat(), Validator.empty(), BundleType.LABELS.getString("table.transaction.security.initialize"));
        this.assetOperations = serviceLocator.getAssetOperations();
        this.domainEventPublisher = domainEventPublisher;
        domainEventPublisher.register(Security.class, domainEventListener);
    }

    protected List<Security> getComboBoxValues() {
        return assetOperations.getAllSecurities();
    }

    protected Security saveItem(Security item) {
        SecurityDialog dialog = new SecurityDialog((Window) getComboBox().getTopLevelAncestor(), getComboBoxModel());
        try {
            return dialog.show(item) ? item : null;
        } finally {
            dialog.dispose();
        }
    }
}