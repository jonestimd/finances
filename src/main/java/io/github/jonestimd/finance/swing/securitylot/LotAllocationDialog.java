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
package io.github.jonestimd.finance.swing.securitylot;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;

import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;

import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.operations.inventory.LotAllocationStrategy;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.swing.ButtonBarFactory;
import io.github.jonestimd.swing.action.ActionAdapter;
import io.github.jonestimd.swing.action.MnemonicAction;
import io.github.jonestimd.swing.component.TextField;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class LotAllocationDialog extends FormDialog {
    private static final String RESOURCE_PREFIX = "dialog.lotAllocation.";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(LABELS.getString("format.date.pattern"));
    private final MnemonicAction firstInAction = new AllocationAction(LotAllocationStrategy.FIRST_IN, "firstIn");
    private final MnemonicAction lastInAction = new AllocationAction(LotAllocationStrategy.LAST_IN, "lastIn");
    private final MnemonicAction lowestPriceAction = new AllocationAction(LotAllocationStrategy.LOWEST_PRICE, "lowestPrice");
    private final MnemonicAction highestPriceAction = new AllocationAction(LotAllocationStrategy.HIGHEST_PRICE, "highestPrice");
    private final SaleLotsTableModel saleLotsTableModel = new SaleLotsTableModel();
    private final JTextField accountNameField;
    private final JTextField securityNameField;
    private final JTextField saleDateField;
    private final JTextField saleSharesField;
    private final JTextField purchaseSharesField;

    private BigDecimal totalShares;

    public LotAllocationDialog(Window owner, FinanceTableFactory tableFactory) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        saleLotsTableModel.addTableModelListener(this::setAllocatedShares);

        GridBagBuilder builder = new GridBagBuilder(getFormPanel(), LABELS.get(), RESOURCE_PREFIX);
        accountNameField = builder.append("account.name", TextField.plain().readOnly().get());
        saleDateField = builder.append("saleDate.name", TextField.plain().readOnly().get());
        securityNameField = builder.append("security.name", TextField.plain().readOnly().get());
        builder.append("purchaseTable", tableFactory.createSortedTable(saleLotsTableModel, SaleLotsTableModel.PURCHASE_DATE));
        builder.append(new ButtonBarFactory().alignRight().border(BUTTON_BAR_BORDER, 0).add(firstInAction, lastInAction, lowestPriceAction, highestPriceAction).get());
        saleSharesField = builder.append("saleShares.name", TextField.plain().readOnly().get());
        purchaseSharesField = builder.append("purchaseShares.name", TextField.plain().readOnly().get());
        addButton(ActionAdapter.forMnemonicAndName(this::discardLots, LABELS.getString(RESOURCE_PREFIX + "action.discardLots.mnemonicAndName")));
    }

    private void discardLots(ActionEvent event) {
        for (int row = 0; row < saleLotsTableModel.getRowCount(); row++) {
            saleLotsTableModel.setValueAt(BigDecimal.ZERO, row, SaleLotsTableModel.ALLOCATED_SHARES);
        }
    }

    public void show(TransactionDetail sale, Collection<SecurityLot> availableLots) {
        this.totalShares = sale.getAssetQuantity().negate();
        accountNameField.setText(sale.getTransaction().getAccount().getName());
        saleDateField.setText(dateFormat.format(sale.getTransaction().getDate()));
        securityNameField.setText(sale.getTransaction().getSecurity().getName());
        saleLotsTableModel.setBeans(availableLots);
        saleSharesField.setText(sale.getAssetQuantity().negate().toString());
        setAllocatedShares(null);
        pack();
        setSize(LABELS.getInt(RESOURCE_PREFIX + "width"), LABELS.getInt(RESOURCE_PREFIX + "height"));
        setVisible(true);
    }

    private void setAllocatedShares(TableModelEvent event) {
        BigDecimal allocatedShares = BigDecimal.ZERO;
        for (SecurityLot row : saleLotsTableModel.getBeans()) {
            allocatedShares = allocatedShares.add(row.getSaleShares());
        }
        purchaseSharesField.setText(allocatedShares.toString());
        setSaveEnabled(saleLotsTableModel.isChanged() && (allocatedShares.signum() == 0 || totalShares.compareTo(allocatedShares) == 0));
    }

    private class AllocationAction extends MnemonicAction {
        private final LotAllocationStrategy strategy;

        public AllocationAction(LotAllocationStrategy strategy, String nameKey) {
            super(LABELS.getString(RESOURCE_PREFIX + "action." + nameKey + ".mnemonicAndName"));
            this.strategy = strategy;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            strategy.allocateLots(Streams.map(saleLotsTableModel.getBeans(), SecurityLotProxy::new), totalShares);
            saleLotsTableModel.fireTableDataChanged();
        }

        private class SecurityLotProxy extends SecurityLot {
            private final SecurityLot lot;

            public SecurityLotProxy(SecurityLot lot) {
                this.lot = lot;
                setId(lot.getId());
                setPurchase(lot.getPurchase());
                setSale(lot.getSale());
                setSaleShares(lot.getSaleShares());
            }

            @Override
            public BigDecimal allocateShares(BigDecimal maxShares) {
                BigDecimal remainingShares = super.allocateShares(maxShares);
                int row = saleLotsTableModel.indexOf(lot);
                saleLotsTableModel.setValueAt(getSaleShares(), row, SaleLotsTableModel.ALLOCATED_SHARES);
                return remainingShares;
            }
        }
    }
}