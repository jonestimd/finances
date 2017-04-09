package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.swing.asset.SecurityTransactionTableModel;
import io.github.jonestimd.finance.swing.asset.UpdateSharesDialog;
import io.github.jonestimd.finance.swing.transaction.TransactionColumnAdapter;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateSharesActionTest {
    private final Random random = new Random();
    @Mock
    private UpdateSharesDialog dialog;
    @Mock
    private TransactionTable table;
    @Mock
    private SecurityTransactionTableModel tableModel;
    @Mock
    private AssetOperations assetOperations;
    @Mock
    private TransactionCategoryOperations categoryOperations;
    private volatile boolean cancelled;
    private volatile boolean shown = false;

    private final Date date = new Date();
    private final int beanCount = random.nextInt();
    private final int rowCount = beanCount * 2;
    private final int dateColumn = random.nextInt();
//    private final Map<TransactionDetailColumnAdapter<?>, Integer> detailColumnMap = ImmutableMap.of(
//            TYPE_ADAPTER, random.nextInt(),
//            SECURITY_ADAPTER, random.nextInt(),
//            SHARES_ADAPTER, random.nextInt(),
//            AMOUNT_ADAPTER, random.nextInt());
    private final TransactionDetail detail = new TransactionDetail();
    private final Security security = new Security();
    private final TransactionCategory feesAction = new TransactionCategory();
    private final TransactionCategory securityAction = new TransactionCategory();

    @Before
    public void trainMocks() throws Exception {
        when(table.getModel()).thenReturn(tableModel);
        when(tableModel.getRowCount()).thenReturn(rowCount);
        when(tableModel.getColumnIndex(TransactionColumnAdapter.DATE_ADAPTER)).thenReturn(dateColumn);
        when(categoryOperations.getSecurityAction(SecurityAction.SELL.code())).thenReturn(securityAction);
        when(categoryOperations.getSecurityAction(SecurityAction.BUY.code())).thenReturn(securityAction);
        when(categoryOperations.getSecurityAction(SecurityAction.COMMISSION_AND_FEES.code())).thenReturn(feesAction);
        when(dialog.show(anyListOf(SecuritySummary.class), any(TransactionDetail.class)))
                .thenAnswer(invocation -> {
                    shown = true;
                    return !cancelled;
                });
    }

    @Test
    public void disabledForNonSecurityAccount() throws Exception {
        TransactionTable table = mock(TransactionTable.class);
        TransactionTableModel model = mock(TransactionTableModel.class);
        when(table.getModel()).thenReturn(model);

        UpdateSharesAction action = new UpdateSharesAction(table, assetOperations, categoryOperations);

        assertThat(action.isEnabled()).isFalse();
    }

    @Test
    public void enabledForSecurityAccount() throws Exception {
        UpdateSharesAction action = new UpdateSharesAction(table, assetOperations, categoryOperations);

        assertThat(action.isEnabled()).isTrue();
    }

    @Test
    public void updatesEnabledWhenModelChanges() throws Exception {
        TransactionTableModel model2 = mock(TransactionTableModel.class);
        when(tableModel.getAccount()).thenReturn(new AccountBuilder().type(AccountType.BANK).get());
        when(model2.getAccount()).thenReturn(new AccountBuilder().type(AccountType.BROKERAGE).get());

        UpdateSharesAction action = new UpdateSharesAction(table, assetOperations, categoryOperations);

        assertThat(action.isEnabled()).isTrue();
        ArgumentCaptor<PropertyChangeListener> listenerCaptor = ArgumentCaptor.forClass(PropertyChangeListener.class);
        verify(table).addPropertyChangeListener(eq("model"), listenerCaptor.capture());
        listenerCaptor.getValue().propertyChange(new PropertyChangeEvent(table, "model", tableModel, model2));
        assertThat(action.isEnabled()).isFalse();
    }

    protected void expectShowDialog() {
        when(tableModel.getBeanCount()).thenReturn(beanCount);
        when(tableModel.getBean(beanCount - 1)).thenReturn(new TransactionBuilder().details(detail).get());
    }

    @Test
    public void displaysDialogOnActionPerformed() throws Exception {
        expectShowDialog();
        cancelled = true;
        UpdateSharesAction action = new UpdateSharesAction(table, dialog, assetOperations, categoryOperations);

        SwingUtilities.invokeAndWait(() -> action.actionPerformed(new ActionEvent(table, -1, "")));

        waitForDialog();
        verify(tableModel, atLeastOnce()).getAccount();
        verify(tableModel).getBeanCount();
        verify(tableModel).getBean(beanCount - 1);
        verify(dialog).show(Lists.newArrayList(), detail);
        verifyNoMoreInteractions(dialog, tableModel);
    }

    @Test
    public void updatesTableWhenDialogSaved() throws Exception {
        expectShowDialog();
        cancelled = false;
        when(dialog.getShares()).thenReturn(BigDecimal.TEN);
        when(dialog.getSecurity()).thenReturn(security);
        UpdateSharesAction action = new UpdateSharesAction(table, dialog, assetOperations, categoryOperations);

        SwingUtilities.invokeAndWait(() -> action.actionPerformed(new ActionEvent(table, -1, "")));

        waitForDialog();
        verify(tableModel).getAccount();
        verify(tableModel).getRowCount();
        verify(tableModel, times(2)).getBeanCount();
        verify(tableModel, times(2)).getBean(beanCount - 1);
        verify(dialog).show(Lists.newArrayList(), detail);
        verify(dialog).getSecurity();
        verify(dialog, atLeastOnce()).getShares();
        verify(categoryOperations).getSecurityAction(SecurityAction.BUY.code());
        verify(tableModel).setSecurity(security, rowCount - 1);
        verify(tableModel).setTransactionType(securityAction, rowCount - 1);
        verify(tableModel).setShares(BigDecimal.TEN, rowCount - 1);
        verify(table).selectLastTransaction(1);
        verify(table).selectAmountColumn();
        verifyNoMoreInteractions(tableModel);
    }

    @Test
    public void updatesNetAmountWhenDialogSavedForBuy() throws Exception {
        expectShowDialog();
        when(tableModel.queueAppendSubRow(anyInt())).thenReturn(rowCount);
        cancelled = false;
        when(dialog.getDate()).thenReturn(date);
        when(dialog.getShares()).thenReturn(BigDecimal.TEN);
        when(dialog.getSecurity()).thenReturn(security);
        when(dialog.getGrossAmount()).thenReturn(BigDecimal.TEN);
        when(dialog.getFees()).thenReturn(BigDecimal.ONE);
        UpdateSharesAction action = new UpdateSharesAction(table, dialog, assetOperations, categoryOperations);

        SwingUtilities.invokeAndWait(() -> action.actionPerformed(new ActionEvent(table, -1, "")));

        waitForDialog();
        verify(tableModel).getAccount();
        verify(tableModel).getRowCount();
        verify(tableModel, times(2)).getBeanCount();
        verify(tableModel, times(2)).getBean(beanCount - 1);
        verify(dialog).show(Lists.newArrayList(), detail);
        verify(dialog).getDate();
        verify(dialog).getSecurity();
        verify(dialog, atLeastOnce()).getShares();
        verify(categoryOperations).getSecurityAction(SecurityAction.COMMISSION_AND_FEES.code());
        verify(categoryOperations).getSecurityAction(SecurityAction.BUY.code());
        verify(tableModel).setTransactionDate(date, rowCount - 2);
        verify(tableModel).setSecurity(security, rowCount - 1);
        verify(tableModel).setTransactionType(securityAction, rowCount - 1);
        verify(tableModel).setShares(BigDecimal.TEN, rowCount - 1);
        verify(tableModel).setAmount(BigDecimal.valueOf(-9L), rowCount - 1);
        verify(tableModel).queueAppendSubRow(rowCount - 1);
        verify(tableModel).setTransactionType(feesAction, rowCount);
        verify(tableModel).setAmount(BigDecimal.ONE.negate(), rowCount);
        verify(table).selectLastTransaction(1);
        verify(table).selectAmountColumn();
    }

    @Test
    public void updatesNetAmountWhenDialogSavedForSell() throws Exception {
        expectShowDialog();
        cancelled = false;
        when(dialog.getDate()).thenReturn(date);
        when(dialog.getShares()).thenReturn(BigDecimal.TEN.negate());
        when(dialog.getSecurity()).thenReturn(security);
        when(dialog.getGrossAmount()).thenReturn(BigDecimal.TEN);
        when(dialog.getFees()).thenReturn(BigDecimal.ONE);
        UpdateSharesAction action = new UpdateSharesAction(table, dialog, assetOperations, categoryOperations);

        SwingUtilities.invokeAndWait(() -> action.actionPerformed(new ActionEvent(table, -1, "")));

        waitForDialog();
        verify(categoryOperations).getSecurityAction(SecurityAction.SELL.code());
        verify(tableModel).setAmount(BigDecimal.valueOf(9L), rowCount - 1);
    }

    private void waitForDialog() throws InterruptedException {
        while (!shown) {
            Thread.sleep(100L);
        }
    }
}