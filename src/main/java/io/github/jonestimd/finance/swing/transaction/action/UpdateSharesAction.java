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
import java.beans.PropertyChangeEvent;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;

import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.asset.SecurityTransactionTableModel;
import io.github.jonestimd.finance.swing.asset.UpdateSharesDialog;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.swing.action.DialogAction;

public class UpdateSharesAction extends DialogAction {
    private final AssetOperations assetOperations;
    private final TransactionCategoryOperations categoryOperations;
    private UpdateSharesDialog dialog;
    private TransactionTable table;
    private SecurityTransactionTableModel tableModel;
    private List<SecuritySummary> securitySummaries;
    private TransactionCategory feesAction;
    private TransactionCategory securityAction;

    public UpdateSharesAction(TransactionTable table, AssetOperations assetOperations, TransactionCategoryOperations categoryOperations) {
        this(table, new UpdateSharesDialog((Window) table.getTopLevelAncestor()), assetOperations, categoryOperations);
    }

    protected UpdateSharesAction(TransactionTable table, UpdateSharesDialog dialog, AssetOperations assetOperations, TransactionCategoryOperations categoryOperations) {
        super(BundleType.LABELS.get(), "action.updateShares");
        this.assetOperations = assetOperations;
        this.categoryOperations = categoryOperations;
        this.dialog = dialog;
        this.table = table;
        updateModel(table.getModel());
        table.addPropertyChangeListener("model", UpdateSharesAction.this::onAccountChange);
    }

    private void onAccountChange(PropertyChangeEvent event) {
        updateModel(event.getNewValue());
    }

    private void updateModel(Object newModel) {
        tableModel = newModel instanceof SecurityTransactionTableModel ? (SecurityTransactionTableModel) newModel : null;
        setEnabled(tableModel != null);
    }

    @Override
    protected void loadDialogData() {
        securitySummaries = assetOperations.getSecuritySummaries(tableModel.getAccount());
    }

    @Override
    protected boolean displayDialog(JComponent owner) {
        return dialog.show(securitySummaries, selectDetail());
    }

    private TransactionDetail selectDetail() {
        Transaction transaction = tableModel.getBean(tableModel.getBeanCount() - 1);
        return transaction.getDetails().get(transaction.getDetails().size() - 1);
    }

    @Override
    protected void saveDialogData() {
        feesAction = categoryOperations.getSecurityAction(SecurityAction.COMMISSION_AND_FEES.code());
        if (isBuy()) {
            securityAction = categoryOperations.getSecurityAction(SecurityAction.BUY.code());
        } else {
            securityAction = categoryOperations.getSecurityAction(SecurityAction.SELL.code());
        }
    }

    private boolean isBuy() {
        return dialog.getShares().signum() >= 0;
    }

    @Override
    protected void setSaveResultOnUI() {
        int rowIndex = tableModel.getRowCount() - 1;
        setDate(dialog.getDate(), rowIndex - 1);
        tableModel.setTransactionType(securityAction, rowIndex);
        tableModel.setSecurity(dialog.getSecurity(), rowIndex);
        tableModel.setShares(dialog.getShares(), rowIndex);
        int detailCount = tableModel.getBean(tableModel.getBeanCount()-1).getDetails().size();
        setAmount(rowIndex, dialog.getGrossAmount());
        table.selectLastTransaction(detailCount);
        table.selectAmountColumn();
    }

    private void setDate(Date date, int rowIndex) {
        if (date != null) {
            tableModel.setTransactionDate(date, rowIndex);
        }
    }

    private void setAmount(int rowIndex, BigDecimal grossAmount) {
        if (grossAmount != null) {
            BigDecimal fees = dialog.getFees();
            BigDecimal amount = fees == null ? grossAmount : grossAmount.subtract(fees);
            tableModel.setAmount(isBuy() ? amount.negate() : amount, rowIndex);
            setCommissionAndFees(rowIndex, fees);
        }
    }

    private void setCommissionAndFees(int rowIndex, BigDecimal fees) {
        if (fees != null) {
            int newRow = tableModel.queueAppendSubRow(rowIndex);
            tableModel.setTransactionType(feesAction, newRow);
            tableModel.setAmount(fees.negate(), newRow);
        }
    }
}
