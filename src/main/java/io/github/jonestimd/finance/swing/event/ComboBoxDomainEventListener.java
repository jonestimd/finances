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
package io.github.jonestimd.finance.swing.event;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;

public class ComboBoxDomainEventListener<ID, T extends UniqueId<ID> & Comparable<? super T>> implements DomainEventListener<ID, T> {
    private final BeanListComboBoxModel<T> comboBoxModel;

    public ComboBoxDomainEventListener(BeanListComboBox<T> comboBox) {
        this((BeanListComboBoxModel<T>) comboBox.getModel());
    }

    public ComboBoxDomainEventListener(BeanListComboBoxModel<T> comboBoxModel) {
        this.comboBoxModel = comboBoxModel;
    }

    public void onDomainEvent(DomainEvent<ID, T> event) {
        if (event.isDelete() || event.isChange() || event.isReplace()) {
            event.getDomainObjects().forEach(this::removeItem);
        }
        if (event.isAdd() || event.isChange()) {
            event.getDomainObjects().forEach(this::addItem);
        }
    }

    protected void addItem(T item) {
        int index = 0;
        while (index < comboBoxModel.getSize() && item.compareTo(comboBoxModel.getElementAt(index)) > 0) {
            index++;
        }
        comboBoxModel.insertElementAt(item, index);
    }

    protected void removeItem(T item) {
        for (int i = 0; i < comboBoxModel.getSize(); i++) {
            T element = comboBoxModel.getElementAt(i);
            if (isSameId(item, element)) {
                comboBoxModel.removeElementAt(i);
                break;
            }
        }
    }

    private boolean isSameId(T item, T element) {
        return UniqueId.isSameId(item, element) && element.getClass().equals(item.getClass());
    }

    public String toString() {
        return "ComboBoxDomainEventListener[" + comboBoxModel + "]";
    }
}