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

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.SecurityEvent;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.event.ReloadEventHandler;
import io.github.jonestimd.finance.swing.transaction.TransactionSummaryTablePanel;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.action.DialogAction;
import io.github.jonestimd.swing.action.MnemonicAction;
import io.github.jonestimd.swing.table.TableSummary;

import static io.github.jonestimd.finance.swing.asset.SecurityTableModel.*;
import static org.apache.commons.lang.StringUtils.*;

public class SecuritiesPanel extends TransactionSummaryTablePanel<Security, SecuritySummary> {
    private final AssetOperations assetOperations;
    private final FinanceTableFactory tableFactory;
    private final SplitsDialogAction splitsAction = new SplitsDialogAction();
    private final MnemonicAction hideZeroSharesAction = new MnemonicAction(BundleType.LABELS.get(), "action.hideZeroShares") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getRowSorter().allRowsChanged();
        }
    };
    @SuppressWarnings("FieldCanBeLocal")
    private final ReloadEventHandler<Long, SecuritySummary> reloadHandler =
            new ReloadEventHandler<>(this, "security.action.reload.status.initialize", this::getTableData, getTableModel());

    public SecuritiesPanel(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher,
                           Iterable<SecurityTableExtension> tableExtensions, FinanceTableFactory tableFactory) {
        super(domainEventPublisher, tableFactory.createValidatedTable(new SecurityTableModel(domainEventPublisher, tableExtensions), NAME_INDEX), "security");
        this.assetOperations = serviceLocator.getAssetOperations();
        this.tableFactory = tableFactory;
        for (SecurityTableExtension extension : tableExtensions) {
            if (extension instanceof TableSummary) {
                addSummaries((TableSummary) extension);
            }
        }
        hideZeroSharesAction.putValue(Action.SELECTED_KEY, Boolean.TRUE);
        domainEventPublisher.register(SecuritySummary.class, reloadHandler);
    }

    @Override
    protected boolean isMatch(SecuritySummary tableRow, String criteria) {
        return includeRow(tableRow) && isMatch(tableRow.getSecurity(), criteria);
    }

    private boolean isMatch(Security security, String criteria) {
        return criteria.isEmpty() || containsIgnoreCase(security.getName(), criteria) || containsIgnoreCase(security.getSymbol(), criteria);
    }

    private boolean includeRow(SecuritySummary summary) {
        return ! Boolean.TRUE.equals(hideZeroSharesAction.getValue(Action.SELECTED_KEY)) || summary.getShares().compareTo(BigDecimal.ZERO) != 0;
    }

    @Override
    protected void addActions(JToolBar toolbar) {
        super.addActions(toolbar);
        toolbar.add(ComponentFactory.newToolbarButton(splitsAction));
        toolbar.add(ComponentFactory.newToolbarToggleButton(hideZeroSharesAction));
    }

    @Override
    protected void addActions(JMenu menu) {
        super.addActions(menu);
        menu.add(new JMenuItem(splitsAction));
    }

    @Override
    protected List<SecuritySummary> getTableData() {
        return assetOperations.getSecuritySummaries();
    }

    @Override
    protected SecuritySummary newBean() {
        return new SecuritySummary();
    }

    @Override
    protected void tableSelectionChanged() {
        super.tableSelectionChanged();
        splitsAction.setEnabled(isSingleRowSelected());
    }

    @Override
    protected List<? extends DomainEvent<?, ?>> saveChanges(List<Security> changedSecurities, List<Security> deletedSecurities) {
        List<SecurityEvent> events = new ArrayList<>();
        if (!changedSecurities.isEmpty()) {
            assetOperations.saveAll(changedSecurities);
            events.add(new SecurityEvent(this, EventType.CHANGED, changedSecurities));
        }
        if (!deletedSecurities.isEmpty()) {
            assetOperations.deleteAll(deletedSecurities);
            events.add(new SecurityEvent(this, EventType.DELETED, deletedSecurities));
        }
        return events;
    }

    public class SplitsDialogAction extends DialogAction {
        private StockSplitDialog dialog;
        private Security security;

        public SplitsDialogAction() {
            super(BundleType.LABELS.get(), "action.stockSplits.edit");
        }

        protected void loadDialogData() {
            dialog = new StockSplitDialog((JFrame) getTopLevelAncestor(), tableFactory, getSelectedBean().getSecurity(), assetOperations, eventPublisher);
        }

        protected boolean displayDialog(JComponent owner) {
            dialog.pack();
            dialog.setVisible(true);
            return ! dialog.isCancelled();
        }

        protected void saveDialogData() {
            security = assetOperations.save(getSelectedBean().getSecurity());
        }

        protected void setSaveResultOnUI() {
            eventPublisher.publishEvent(new SecurityEvent(this, EventType.CHANGED, Collections.singleton(security)));
        }
    }
}