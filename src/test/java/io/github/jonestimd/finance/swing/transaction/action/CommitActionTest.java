package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.event.HierarchyListener;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.SwingUtilities;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.TransactionEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.operations.TransactionUpdate;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import io.github.jonestimd.swing.window.StatusFrame;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommitActionTest {
    @Mock
    private StatusFrame frame;
    @Mock
    private TransactionTable transactionTable;
    @Mock
    private TransactionTableModel tableModel;
    @Mock
    private TransactionService transactionService;
    @Mock
    private DomainEventPublisher domainEventPublisher;
    @InjectMocks
    private CommitAction commitAction;
    private Random random = new Random();
    private List<DomainEvent<?, ?>> domainEvents = Arrays.asList(new TransactionEvent(this, null, Collections.emptyList()));

    @Before
    public void trainMocks() throws Exception {
        when(transactionTable.getModel()).thenReturn(tableModel);
        when(transactionService.saveTransaction(any(TransactionUpdate.class))).thenAnswer(invocation -> {
            TransactionUpdate update = (TransactionUpdate) invocation.getArguments()[0];
            update.getTransactions().stream().filter(Transaction::isNew).forEach(TestDomainUtils::setId);
            return domainEvents;
        });
    }

    @Test
    public void doesNotSaveEmptyTransaction() throws Exception {
        when(transactionTable.getSelectedTransaction()).thenReturn(new TransactionBuilder().details(new TransactionDetail()).get());

        commitAction.actionPerformed(null);

        verify(transactionTable).getSelectedTransaction();
        verifyNoMoreInteractions(transactionTable);
        verifyZeroInteractions(transactionService);
    }

    @Test
    public void doesNotSaveInvalidTransaction() throws Exception {
        when(transactionTable.getSelectedTransaction()).thenReturn(new TransactionBuilder().detailAmounts(BigDecimal.ONE, null).get());

        commitAction.actionPerformed(null);

        verify(transactionTable).getSelectedTransaction();
        verifyNoMoreInteractions(transactionTable);
        verifyZeroInteractions(transactionService);
    }

    @Test
    public void doesNotSaveUnchangedTransaction() throws Exception {
        final Transaction transaction = new TransactionBuilder().detailAmounts(BigDecimal.ONE).get();
        final int rowIndex = random.nextInt();
        final int viewIndex = random.nextInt();
        when(transactionTable.getSelectedTransaction()).thenReturn(transaction);
        when(transactionTable.convertRowIndexToView(rowIndex)).thenReturn(viewIndex);
        when(tableModel.rowIndexOf(transaction)).thenReturn(rowIndex);
        when(tableModel.isChanged(transaction)).thenReturn(false);

        commitAction.actionPerformed(null);

        verify(transactionTable).getSelectedTransaction();
        verify(transactionTable, atLeastOnce()).getModel();
        verify(transactionTable).convertRowIndexToView(rowIndex);
        verify(transactionTable).nextTransaction(viewIndex);
        verifyNoMoreInteractions(transactionTable);
        verifyZeroInteractions(transactionService);
    }

    @Test
    public void savesNewTransaction() throws Exception {
        final Transaction transaction = new TransactionBuilder().detailAmounts(BigDecimal.ONE).get();
        final int rowIndex = random.nextInt();
        final int viewIndex = random.nextInt();
        final int beanCount = random.nextInt();
        when(transactionTable.getSelectedTransaction()).thenReturn(transaction);
        when(transactionTable.convertRowIndexToView(rowIndex)).thenReturn(viewIndex);
        when(tableModel.rowIndexOf(transaction)).thenReturn(rowIndex);
        when(tableModel.isChanged(transaction)).thenReturn(true);
        when(tableModel.getBeanCount()).thenReturn(beanCount);
        when(tableModel.getBean(beanCount-1)).thenReturn(new Transaction(-1L));

        commitAction.actionPerformed(null);
        waitForUI();

        verify(transactionTable, atLeastOnce()).isShowing();
        verify(transactionTable).getSelectedTransaction();
        verify(transactionTable, atLeastOnce()).getModel();
        verify(transactionTable).convertRowIndexToView(rowIndex);
        verify(transactionTable).getParent();
        verify(transactionTable).addHierarchyListener(any(HierarchyListener.class));
        verify(transactionTable, timeout(1000)).selectLastTransaction();
        verify(transactionTable, atLeastOnce()).getModel();
        verify(tableModel).addEmptyTransaction();
        verifyNoMoreInteractions(transactionTable);
        verify(transactionService).saveTransaction(any(TransactionUpdate.class));
        domainEvents.forEach(event -> verify(domainEventPublisher).publishEvent(event));
    }

    @Test
    public void updatesExistingTransaction() throws Exception {
        final Transaction transaction = new TransactionBuilder().nextId().detailAmounts(BigDecimal.ONE).get();
        final int rowIndex = random.nextInt();
        final int viewIndex = random.nextInt();
        final int beanCount = random.nextInt();
        when(transactionTable.getSelectedTransaction()).thenReturn(transaction);
        when(transactionTable.convertRowIndexToView(rowIndex)).thenReturn(viewIndex);
        when(tableModel.rowIndexOf(transaction)).thenReturn(rowIndex);
        when(tableModel.isChanged(transaction)).thenReturn(true);
        when(tableModel.getBeanCount()).thenReturn(beanCount);
        when(tableModel.getBean(beanCount-1)).thenReturn(new Transaction());
        when(transactionTable.getSelectedRowCount()).thenReturn(1);

        commitAction.actionPerformed(null);
        waitForUI();

        verify(transactionTable, atLeastOnce()).isShowing();
        verify(transactionTable).getSelectedTransaction();
        verify(transactionTable, atLeastOnce()).getModel();
        verify(transactionTable).convertRowIndexToView(rowIndex);
        verify(transactionTable).getParent();
        verify(transactionTable).addHierarchyListener(any(HierarchyListener.class));
        verify(transactionTable, timeout(1000)).getSelectedRowCount();
        verify(transactionTable, timeout(1000)).nextTransaction(viewIndex);
        verify(transactionTable, atLeastOnce()).getModel();
        verify(tableModel, times(0)).addEmptyTransaction();
        verifyNoMoreInteractions(transactionTable);
        verify(transactionService).saveTransaction(any(TransactionUpdate.class));
    }

    @Test
    public void deletesEmptyTransactionAndSelectsNextTransaction() throws Exception {
        final Transaction transaction = new TransactionBuilder().nextId().detailAmounts(BigDecimal.ONE).get();
        final int rowIndex = random.nextInt();
        final int viewIndex = random.nextInt();
        final int beanCount = random.nextInt();
        when(transactionTable.getSelectedTransaction()).thenReturn(transaction);
        when(transactionTable.convertRowIndexToView(rowIndex)).thenReturn(viewIndex);
        when(transactionTable.getSelectedRowCount()).thenReturn(0);
        when(tableModel.rowIndexOf(transaction)).thenReturn(rowIndex);
        when(tableModel.isChanged(transaction)).thenReturn(true);
        when(tableModel.getDetailDeletes(transaction)).thenReturn(transaction.getDetails());
        when(transactionService.deleteTransaction(transaction)).thenReturn(new ArrayList<>());
        when(tableModel.getBeanCount()).thenReturn(beanCount);
        when(tableModel.getBean(beanCount-1)).thenReturn(new Transaction());

        commitAction.actionPerformed(null);
        waitForUI();

        verify(transactionTable, atLeastOnce()).isShowing();
        verify(transactionTable).getSelectedTransaction();
        verify(transactionTable, atLeastOnce()).getModel();
        verify(transactionTable).convertRowIndexToView(rowIndex);
        verify(transactionTable).getParent();
        verify(transactionTable).addHierarchyListener(any(HierarchyListener.class));
        verify(transactionTable, timeout(1000)).getSelectedRowCount();
        verify(transactionTable, timeout(1000)).selectRowAt(viewIndex);
        verify(transactionTable, atLeastOnce()).getModel();
        verifyNoMoreInteractions(transactionTable);
        verify(transactionService).deleteTransaction(transaction);
    }

    @Test
    public void deletesTransactionAndSelectsNextTransaction() throws Exception {
        final Transaction transaction = new TransactionBuilder().nextId().detailAmounts(BigDecimal.ONE).get();
        final int rowIndex = random.nextInt();
        final int viewIndex = random.nextInt();
        final int beanCount = random.nextInt();
        when(transactionTable.getSelectedTransaction()).thenReturn(transaction);
        when(transactionTable.convertRowIndexToView(rowIndex)).thenReturn(viewIndex);
        when(transactionTable.getSelectedRowCount()).thenReturn(0);
        when(tableModel.rowIndexOf(transaction)).thenReturn(rowIndex);
        when(tableModel.isChanged(transaction)).thenReturn(true);
        when(tableModel.isPendingDelete(transaction)).thenReturn(true);
        when(transactionService.deleteTransaction(transaction)).thenReturn(new ArrayList<>());
        when(tableModel.getBeanCount()).thenReturn(beanCount);
        when(tableModel.getBean(beanCount-1)).thenReturn(new Transaction());

        commitAction.actionPerformed(null);
        waitForUI();

        verify(transactionTable, atLeastOnce()).isShowing();
        verify(transactionTable).getSelectedTransaction();
        verify(transactionTable, atLeastOnce()).getModel();
        verify(transactionTable).convertRowIndexToView(rowIndex);
        verify(transactionTable).getParent();
        verify(transactionTable).addHierarchyListener(any(HierarchyListener.class));
        verify(transactionTable, timeout(1000)).getSelectedRowCount();
        verify(transactionTable, timeout(1000)).selectRowAt(viewIndex);
        verify(transactionTable, atLeastOnce()).getModel();
        verifyNoMoreInteractions(transactionTable);
        verify(transactionService).deleteTransaction(transaction);
    }

    private void waitForUI() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {});
    }
}