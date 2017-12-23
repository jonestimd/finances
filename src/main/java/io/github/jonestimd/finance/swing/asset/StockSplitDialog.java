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
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.Action;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.SecuritySummaryEvent;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.swing.action.BackgroundAction;
import io.github.jonestimd.swing.component.ValidatedTablePanel;
import io.github.jonestimd.swing.dialog.MessageDialog;
import org.apache.commons.lang.time.DateUtils;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class StockSplitDialog extends MessageDialog {
    private Security security;
    private final StockSplitTableModel tableModel;
    private final AssetOperations assetOperations;
    private final DomainEventPublisher domainEventPublisher;

    public StockSplitDialog(Window owner, FinanceTableFactory tableFactory, Security security, AssetOperations assetOperations, DomainEventPublisher domainEventPublisher) {
        super(owner, LABELS.formatMessage("stockSplit.dialog.title", security.getName()), ModalityType.DOCUMENT_MODAL);
        this.security = security;
        this.assetOperations = assetOperations;
        this.domainEventPublisher = domainEventPublisher;
        this.tableModel = new StockSplitTableModel(security.getSplits());
        setContentPane(new StockSplitPanel(tableFactory));
    }

    public class StockSplitPanel extends ValidatedTablePanel<StockSplit> {
        protected StockSplitPanel(FinanceTableFactory tableFactory) {
            super(LABELS.get(), tableFactory.createValidatedTable(tableModel, StockSplitTableModel.DATE_INDEX), "stockSplit");
        }

        @Override
        protected Action createSaveAction() {
            return new SaveAction();
        }

        @Override
        protected StockSplit newBean() {
            StockSplit split = new StockSplit();
            split.setDate(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH));
            split.setSecurity(security);
            return split;
        }

        @Override
        protected List<StockSplit> confirmDelete(List<StockSplit> items) {
            return items;
        }

        @Override
        protected List<StockSplit> getTableData() {
            return security.getSplits();
        }
    }

    private class SaveAction extends BackgroundAction<List<SecuritySummary>> {
        public SaveAction() {
            super(StockSplitDialog.this, LABELS.get(), "stockSplit.action.save");
        }

        @Override
        protected boolean confirmAction(ActionEvent event) {
            return tableModel.isNoErrors();
        }

        @Override
        public List<SecuritySummary> performTask() {
            security.getSplits().removeAll(tableModel.getPendingDeletes());
            security.getSplits().addAll(tableModel.getPendingAdds());
            return assetOperations.saveSplits(security);
        }

        @Override
        public void updateUI(List<SecuritySummary> result) {
            security = result.get(0).getSecurity();
            tableModel.setBeans(security.getSplits());
            domainEventPublisher.publishEvent(new SecuritySummaryEvent(StockSplitDialog.this, EventType.REPLACED, result));
        }
    }
}