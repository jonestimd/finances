package io.github.jonestimd.finance.swing.transaction.action;

import java.awt.EventQueue;
import java.awt.event.HierarchyListener;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.TestSequence;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class AutofillTaskTest {
    private final TransactionService transactionService = mock(TransactionService.class);
    private final TransactionTable transactionTable = mock(TransactionTable.class);
    private final TransactionTableModel transactionTableModel = mock(TransactionTableModel.class);
    private final Account account = new Account(TestSequence.nextId());

    @Before
    public void setupTableMock() {
        when(transactionTable.getModel()).thenReturn(transactionTableModel);
        when(transactionTableModel.getAccount()).thenReturn(account);
    }

    @Test
    public void executeDoesNothingIfTransactionIsNotEmpty() throws Exception {
        Transaction transaction = new TransactionBuilder().details(new TransactionDetail(null, BigDecimal.TEN, null, null)).get();

        AutofillTask.execute(transactionService, transaction, transactionTable);
        waitForSwing();

        verifyZeroInteractions(transactionService, transactionTable);
    }

    @Test
    public void executeHandlesNoTransactionForPayee() throws Exception {
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();

        AutofillTask.execute(transactionService, transaction, transactionTable);
        waitForSwing();

        verify(transactionService).findLatestForPayee(payee.getId());
        verify(transactionTable).getParent();
        verify(transactionTable).addHierarchyListener(any(HierarchyListener.class));
        verifyNoMoreInteractions(transactionTable);
    }

    @Test
    public void executeSetsDetailsOnTransaction() throws Exception {
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        Transaction latestForPayee = new TransactionBuilder().get();
        when(transactionService.findLatestForPayee(anyLong())).thenReturn(latestForPayee);

        AutofillTask.execute(transactionService, transaction, transactionTable);
        waitForSwing();

        verify(transactionService).findLatestForPayee(payee.getId());
        verify(transactionTable).setDetails(latestForPayee, transaction);
        verify(transactionTable).selectLastTransaction(1);
        verify(transactionTable).selectAmountColumn();
    }

    @Test
    public void executeUsesTransferTransactionWithMultipleDetails() throws Exception {
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        Transaction latestForPayee = new TransactionBuilder().details(new TransactionDetailBuilder().newTransfer()).get();
        latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction().addDetails(new TransactionDetail());
        when(transactionService.findLatestForPayee(anyLong())).thenReturn(latestForPayee);

        AutofillTask.execute(transactionService, transaction, transactionTable);
        waitForSwing();

        verify(transactionService).findLatestForPayee(payee.getId());
        verify(transactionTable).setDetails(latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction(), transaction);
        verify(transactionTable).selectLastTransaction(1);
        verify(transactionTable).selectAmountColumn();
    }

    @Test
    public void executeUsesTransferTransactionWithSameAccount() throws Exception {
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        Transaction latestForPayee = new TransactionBuilder().details(new TransactionDetailBuilder().newTransfer()).get();
        latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction().setAccount(account);
        when(transactionService.findLatestForPayee(anyLong())).thenReturn(latestForPayee);

        AutofillTask.execute(transactionService, transaction, transactionTable);
        waitForSwing();

        verify(transactionService).findLatestForPayee(payee.getId());
        verify(transactionTable).setDetails(latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction(), transaction);
        verify(transactionTable).selectLastTransaction(1);
        verify(transactionTable).selectAmountColumn();
    }

    private void waitForSwing() throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {}
        });
    }
}
