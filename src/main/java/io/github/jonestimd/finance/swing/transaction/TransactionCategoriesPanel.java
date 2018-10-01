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
package io.github.jonestimd.finance.swing.transaction;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JToolBar;

import io.github.jonestimd.finance.domain.event.CategoryEvent;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.MergeAction;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.event.ReloadEventHandler;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.table.FormatTableCellRenderer;
import io.github.jonestimd.swing.window.FrameManager;

import static io.github.jonestimd.finance.swing.transaction.TransactionCategoryTableModel.*;
import static org.apache.commons.lang.StringUtils.*;

public class TransactionCategoriesPanel extends AccountAccessPanel<TransactionCategory, TransactionCategorySummary> {
    private final TransactionCategoryOperations transactionCategoryOperations;
    private final Action mergeAction;
    @SuppressWarnings("FieldCanBeLocal")
    private final ReloadEventHandler<Long, TransactionCategorySummary> reloadHandler =
            new ReloadEventHandler<>(this, "category.action.reload.status.initialize", this::getTableData, this::getTableModel);

    public TransactionCategoriesPanel(ServiceLocator serverLocator, DomainEventPublisher domainEventPublisher,
            FinanceTableFactory tableFactory, FrameManager<WindowType> frameManager) {
        super(domainEventPublisher, tableFactory.createValidatedTable(new TransactionCategoryTableModel(domainEventPublisher), CODE_INDEX), "category", frameManager);
        this.transactionCategoryOperations = serverLocator.getTransactionCategoryOperations();
        this.mergeAction = new MergeAction<>(TransactionCategory.class, "mergeCategories", getTable(), new TransactionTypeFormat(), TransactionCategorySummary::getCategory,
                transactionCategoryOperations, domainEventPublisher, this::isMergeDisabled);
        domainEventPublisher.register(TransactionCategorySummary.class, reloadHandler);
        getTable().getColumn(TransactionCategoryColumnAdapter.KEY_ADAPTER).setCellRenderer(new FormatTableCellRenderer(new CategoryKeyFormat(), FinanceTableFactory.HIGHLIGHTER));
        getTable().getColumn(TransactionCategoryColumnAdapter.KEY_ADAPTER).setCellEditor(new CategoryKeyTableCellEditor());
    }

    private boolean isMergeDisabled(List<TransactionCategorySummary> selection) {
        return selection.stream().anyMatch(TransactionCategorySummary::isLocked);
    }

    @Override
    protected boolean isMatch(TransactionCategorySummary tableRow, String criteria) {
        TransactionCategory category = tableRow.getCategory();
        return criteria.isEmpty() || containsIgnoreCase(category.qualifiedName(" "), criteria) || containsIgnoreCase(category.getDescription(), criteria);
    }

    @Override
    protected void addActions(JToolBar toolbar) {
        super.addActions(toolbar);
        toolbar.add(ComponentFactory.newToolbarButton(mergeAction));
    }

    @Override
    protected void addActions(JMenu menu) {
        super.addActions(menu);
        menu.add(mergeAction);
    }

    @Override
    protected boolean isDeleteEnabled(List<TransactionCategorySummary> selectionMinusPendingDeletes) {
        return super.isDeleteEnabled(selectionMinusPendingDeletes) && selectionMinusPendingDeletes.stream().noneMatch(this::isLockedOrHasChildren);
    }

    private boolean isLockedOrHasChildren(TransactionCategorySummary summary) {
        return ! summary.isNew() && (summary.isLocked() || getTableModel().getBeans().stream()
                .anyMatch(other -> {
                    TransactionCategory parent = other.getCategory().getParent();
                    return parent != null && parent.getId().equals(summary.getId());
                }));
    }

    @Override
    protected List<TransactionCategorySummary> getTableData() {
        return transactionCategoryOperations.getTransactionCategorySummaries();
    }

    @Override
    protected TransactionCategorySummary newBean() {
        return new TransactionCategorySummary();
    }

    @Override
    protected List<? extends DomainEvent<?, ?>> saveChanges(List<TransactionCategory> changedCategories, List<TransactionCategory> deletedCategories) {
        List<CategoryEvent> events = new ArrayList<>();
        if (! changedCategories.isEmpty()) {
            transactionCategoryOperations.saveAll(changedCategories);
            events.add(new CategoryEvent(this, EventType.CHANGED, changedCategories));
        }
        if (! deletedCategories.isEmpty()) {
            transactionCategoryOperations.deleteAll(deletedCategories);
            events.add(new CategoryEvent(this, EventType.DELETED, deletedCategories));
        }
        return events;
    }
}