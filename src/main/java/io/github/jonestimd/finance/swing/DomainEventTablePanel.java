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
package io.github.jonestimd.finance.swing;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.HighlightText;
import io.github.jonestimd.swing.action.BackgroundAction;
import io.github.jonestimd.swing.action.FocusAction;
import io.github.jonestimd.swing.component.ValidatedTablePanel;
import io.github.jonestimd.swing.table.DecoratedTable;
import io.github.jonestimd.swing.table.filter.PredicateRowFilter;
import io.github.jonestimd.swing.table.model.ValidatedBeanListTableModel;

import static io.github.jonestimd.finance.swing.BundleType.*;

// TODO listen for new items
// TODO use JLayeredPane of frame for validation messages

/**
 * Extends {@link ValidatedTablePanel} to publish domain events when the table data is saved.
 */
public abstract class DomainEventTablePanel<T extends UniqueId<?>> extends ValidatedTablePanel<T> implements HighlightText {
    protected final DomainEventPublisher eventPublisher;
    private final JTextField filterField = new ComponentFactory().newFilterField();

    @SuppressWarnings("unchecked")
    protected DomainEventTablePanel(DomainEventPublisher domainEventPublisher, DecoratedTable<T, ? extends ValidatedBeanListTableModel<T>> table, String resourceGroup) {
        super(BundleType.LABELS.get(), table, resourceGroup);
        this.eventPublisher = domainEventPublisher;
        PredicateRowFilter.install(getRowSorter(), filterField, this::isVisible);
        FocusAction.install(filterField, table, LABELS.get(), "table.filterField.accelerator");
    }

    private boolean isVisible(T tableRow, String criteria) {
        return tableRow.isNew() || isMatch(tableRow, criteria);
    }

    protected abstract boolean isMatch(T tableRow, String criteria);

    @Override
    protected JToolBar createToolBar() {
        JToolBar toolBar = super.createToolBar();
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(filterField);
        return toolBar;
    }

    @Override
    protected Action createSaveAction() {
        return new SaveAction();
    }

    @Override
    public Collection<String> getHighlightText() {
        String filterText = filterField.getText();
        return filterText.isEmpty() ? Collections.emptySet() : Collections.singleton(filterText);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        filterField.setText("");
    }

    /**
     * Save changed rows from the table.
     */
    protected abstract List<? extends DomainEvent<?, ?>> saveChanges(Stream<T> changedRows, List<T> deletedRows);

    public class SaveAction extends BackgroundAction<List<? extends DomainEvent<?, ?>>> {
        public SaveAction() {
            super(DomainEventTablePanel.this, BundleType.LABELS.get(), resourceGroup + ".action.save");
        }

        protected boolean confirmAction(ActionEvent event) {
            return true;
        }

        public List<? extends DomainEvent<?, ?>> performTask() {
            return saveChanges(getTableModel().getPendingUpdates(), getTableModel().getPendingDeletes());
        }

        public void updateUI(List<? extends DomainEvent<?, ?>> events) {
            events.forEach(eventPublisher::publishEvent);
            getTableModel().commit();
            tableSelectionChanged(); // in case the selected row was just saved
        }
    }
}