package io.github.jonestimd.finance.swing.transaction.action;

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
import io.github.jonestimd.swing.window.StatusFrame;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AutofillTaskTest {
    @Mock
    private StatusFrame frame;
    @Mock
    private TransactionService transactionService;
    @Mock
    private TransactionTable transactionTable;
    @Mock
    private TransactionTableModel transactionTableModel;
    private final Account account = new Account(TestSequence.nextId());

    @Before
    public void setupTableMock() throws Exception {
        when(transactionTable.getModel()).thenReturn(transactionTableModel);
        when(transactionTableModel.getAccount()).thenReturn(account);
    }

    @Test
    public void executeDoesNothingIfTransactionIsNotEmpty() throws Exception {
        when(transactionTable.getParent()).thenReturn(frame);
        Transaction transaction = new TransactionBuilder().details(new TransactionDetail(null, BigDecimal.TEN, null, null)).get();

        assertThat(AutofillTask.execute(transactionService, transaction, transactionTable)).isNull();
    }

    @Test
    public void executeHandlesNoTransactionForPayee() throws Exception {
        when(transactionTable.getParent()).thenReturn(frame);
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();

        AutofillTask.execute(transactionService, transaction, transactionTable).get();

        verify(transactionService, timeout(1000)).findLatestForPayee(payee.getId());
        verify(transactionTable, timeout(1000)).getParent();
        verifyNoMoreInteractions(transactionTable);
    }

    @Test
    public void executeSetsDetailsOnTransaction() throws Exception {
        when(transactionTable.getParent()).thenReturn(frame);
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        Transaction latestForPayee = new TransactionBuilder().get();
        when(transactionService.findLatestForPayee(anyLong())).thenReturn(latestForPayee);

        AutofillTask.execute(transactionService, transaction, transactionTable).get();

        verify(transactionService, timeout(1000)).findLatestForPayee(payee.getId());
        verify(transactionTable, timeout(1000)).setDetails(latestForPayee, transaction);
        verify(transactionTable, timeout(1000)).selectLastTransaction(1);
        verify(transactionTable, timeout(1000)).selectAmountColumn();
    }

    @Test
    public void executeUsesTransferTransactionWithMultipleDetails() throws Exception {
        when(transactionTable.getParent()).thenReturn(frame);
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        Transaction latestForPayee = new TransactionBuilder().details(new TransactionDetailBuilder().newTransfer()).get();
        latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction().addDetails(new TransactionDetail());
        when(transactionService.findLatestForPayee(anyLong())).thenReturn(latestForPayee);

        AutofillTask.execute(transactionService, transaction, transactionTable).get();

        verify(transactionService, timeout(1000)).findLatestForPayee(payee.getId());
        verify(transactionTable, timeout(1000)).setDetails(latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction(), transaction);
        verify(transactionTable, timeout(1000)).selectLastTransaction(1);
        verify(transactionTable, timeout(1000)).selectAmountColumn();
    }

    @Test
    public void executeUsesTransferTransactionWithSameAccount() throws Exception {
        when(transactionTable.getParent()).thenReturn(frame);
        Payee payee = new Payee(TestSequence.nextId(), "payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        Transaction latestForPayee = new TransactionBuilder().details(new TransactionDetailBuilder().newTransfer()).get();
        latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction().setAccount(account);
        when(transactionService.findLatestForPayee(anyLong())).thenReturn(latestForPayee);

        AutofillTask.execute(transactionService, transaction, transactionTable).get();

        verify(transactionService, timeout(1000)).findLatestForPayee(payee.getId());
        verify(transactionTable, timeout(1000)).setDetails(latestForPayee.getDetails().get(0).getRelatedDetail().getTransaction(), transaction);
        verify(transactionTable, timeout(1000)).selectLastTransaction(1);
        verify(transactionTable, timeout(1000)).selectAmountColumn();
    }
}
