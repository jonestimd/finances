// The MIT License (MIT)
//
// Copyright (c) 2024 Tim Jones
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
package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.Window;
import java.util.List;

import javax.swing.JComponent;

import com.google.common.collect.ImmutableList;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.securitylot.LotAllocationDialog;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import io.github.jonestimd.swing.action.DialogAction;
import io.github.jonestimd.util.Streams;

public class EditLotsAction extends DialogAction {
    private static final List<String> SECURITY_ACTIONS = ImmutableList.of(SecurityAction.SELL.code(), SecurityAction.SHARES_OUT.code());
    private final TransactionTable table;
    private final LotAllocationDialog dialog;
    private final TransactionService transactionService;
    private List<SecurityLot> availableLots;

    public EditLotsAction(TransactionTable table, TransactionService transactionService, FinanceTableFactory tableFactory) {
        super(BundleType.LABELS.get(), "action.editSecurityLots");
        this.table = table;
        this.dialog = new LotAllocationDialog((Window) table.getTopLevelAncestor(), tableFactory);
        this.transactionService = transactionService;
        table.getSelectionModel().addListSelectionListener(event -> setEnabled(isValidSecurityDetail()));
    }

    private boolean isValidSecurityDetail() {
        if (table.getSelectedRowCount() == 1) {
            int row = table.getLeadSelectionModelIndex();
            TransactionTableModel model = table.getModel();
            if (model.isSubRow(row)) {
                TransactionDetail detail = model.getBeanAtRow(row).getDetails().get(model.getSubRowIndex(row)-1);
                return detail.getCategory() != null && SECURITY_ACTIONS.contains(detail.getCategory().getKey().getCode())
                        || detail.isTransfer() && detail.getAssetQuantity() != null && detail.getTransaction().isSecurity();
            }
        }
        return false;
    }

    private TransactionDetail getSelectedDetail() {
        TransactionTableModel model = table.getModel();
        int row = table.getLeadSelectionModelIndex();
        TransactionDetail detail = model.getBeanAtRow(row).getDetails().get(model.getSubRowIndex(row) - 1);
        if (detail.isTransfer() && detail.getAssetQuantity().signum() > 0) return detail.getRelatedDetail();
        return detail;
    }

    @Override
    public void loadDialogData() {
        availableLots = transactionService.findAvailableLots(getSelectedDetail());
    }

    @Override
    public boolean displayDialog(JComponent owner) {
        dialog.show(getSelectedDetail(), availableLots);
        return ! dialog.isCancelled();
    }

    @Override
    public void saveDialogData() {
        transactionService.saveSecurityLots(availableLots);
    }

    @Override
    public void setSaveResultOnUI() {
        TransactionDetail detail = getSelectedDetail();
        detail.getPurchaseLots().clear();
        detail.getPurchaseLots().addAll(Streams.filter(availableLots, lot -> !lot.isEmpty()));
        int row = table.getLeadSelectionModelIndex();
        table.getModel().fireTableRowsUpdated(row, row);
    }
}
