package io.github.jonestimd.finance.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.dao.hibernate.DomainEventRecorder;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.DomainEventHolder;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.operations.TransactionOperations;
import io.github.jonestimd.finance.operations.TransactionUpdate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTest {
    @Mock
    private TransactionOperations transactionOperations;
    @Mock
    private DomainEventRecorder eventRecorder;
    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    public void testSaveTransaction() throws Exception {
        Transaction transaction = new Transaction();
        when(eventRecorder.beginRecording()).thenReturn(mock(DomainEventHolder.class));

        transactionService.saveTransaction(transaction);

        ArgumentCaptor<TransactionUpdate> updateCaptor = ArgumentCaptor.forClass(TransactionUpdate.class);
        InOrder inOrder = inOrder(transactionOperations, eventRecorder);
        inOrder.verify(eventRecorder).beginRecording();
        inOrder.verify(transactionOperations).saveTransaction(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getTransaction()).isSameAs(transaction);
        assertThat(updateCaptor.getValue().getDeletes()).isEmpty();
    }

    @Test
    public void testSaveTransactionUpdate() throws Exception {
        TransactionUpdate update = new TransactionUpdate(new Transaction());
        when(eventRecorder.beginRecording()).thenReturn(mock(DomainEventHolder.class));

        transactionService.saveTransaction(update);

        InOrder inOrder = inOrder(transactionOperations, eventRecorder);
        inOrder.verify(eventRecorder).beginRecording();
        inOrder.verify(transactionOperations).saveTransaction(same(update));
    }

    @Test
    public void testSaveTransactions() throws Exception {
        List<Transaction> transactions = new ArrayList<Transaction>();
        List<Transaction> result = new ArrayList<Transaction>();
        when(transactionOperations.saveTransactions(anyCollectionOf(Transaction.class)))
            .thenReturn(result);

        assertThat(transactionService.saveTransactions(transactions)).isSameAs(result);

        verify(transactionOperations).saveTransactions(transactions);
    }

    @Test
    public void testMoveTransaction() throws Exception {
        Transaction transaction = new Transaction();
        Account account = new Account();
        DomainEventHolder eventHolder = mock(DomainEventHolder.class);
        when(eventRecorder.beginRecording()).thenReturn(eventHolder);
        List<DomainEvent<?, ? extends UniqueId<?>>> domainEvents = new ArrayList<>();
        doReturn(domainEvents).when(eventHolder).getEvents();

        assertThat(transactionService.moveTransaction(transaction, account)).isSameAs(domainEvents);

        verify(transactionOperations).moveTransaction(transaction, account);
    }

    @Test
    public void testSaveDetail() throws Exception {
        TransactionDetail detail = new TransactionDetail();
        TransactionDetail result = new TransactionDetail();
        when(transactionOperations.saveDetail(any(TransactionDetail.class)))
            .thenReturn(result);

        assertThat(transactionService.saveDetail(detail)).isSameAs(result);

        verify(transactionOperations).saveDetail(detail);
    }

    @Test
    public void testDeleteTransaction() throws Exception {
        Transaction transaction = new Transaction();
        when(eventRecorder.beginRecording()).thenReturn(mock(DomainEventHolder.class));

        transactionService.deleteTransaction(transaction);

        InOrder inOrder = inOrder(transactionOperations, eventRecorder);
        inOrder.verify(eventRecorder).beginRecording();
        inOrder.verify(transactionOperations).deleteTransaction(same(transaction));
    }

    @Test
    public void testGetTransactionsByAccount() throws Exception {
        List<Transaction> transactions = new ArrayList<Transaction>();
        when(transactionOperations.getTransactions(anyLong()))
            .thenReturn(transactions);

        assertThat(transactionService.getTransactions(-1L)).isSameAs(transactions);

        verify(transactionOperations).getTransactions(-1L);
    }

    @Test
    public void testFindLatestByPayee() throws Exception {
        Transaction transaction = new Transaction();
        when(transactionOperations.findLatestForPayee(anyLong())).thenReturn(transaction);

        assertThat(transactionService.findLatestForPayee(-1L)).isSameAs(transaction);

        verify(transactionOperations).findLatestForPayee(-1L);
    }

    @Test
    public void testFindSecuritySalesWithoutLots() throws Exception {
        List<TransactionDetail> sales = new ArrayList<TransactionDetail>();
        Date saleDate = new Date();
        when(transactionOperations.findSecuritySalesWithoutLots(anyString(), any(Date.class)))
            .thenReturn(sales);

        assertThat(transactionService.findSecuritySalesWithoutLots("prefix", saleDate)).isSameAs(sales);

        verify(transactionOperations).findSecuritySalesWithoutLots("prefix", saleDate);
    }

    @Test
    public void testFindPurchasesWithRemainingLots() throws Exception {
        List<TransactionDetail> purchases = new ArrayList<TransactionDetail>();
        Account account = new Account();
        Security security = new Security();
        Date saleDate = new Date();
        when(transactionOperations.findPurchasesWithRemainingLots(any(Account.class), any(Security.class), any(Date.class)))
            .thenReturn(purchases);

        assertThat(transactionService.findPurchasesWithRemainingLots(account, security, saleDate)).isSameAs(purchases);

        verify(transactionOperations).findPurchasesWithRemainingLots(account, security, saleDate);
    }

    @Test
    public void testGetPurchaseLots() throws Exception {
        List<SecurityLot> lots = new ArrayList<SecurityLot>();
        when(transactionOperations.findAvailableLots(any(TransactionDetail.class)))
            .thenReturn(lots);

        TransactionDetail sale = new TransactionDetail();
        assertThat(transactionService.findAvailableLots(sale)).isSameAs(lots);

        verify(transactionOperations).findAvailableLots(same(sale));
    }

    @Test
    public void testSaveSecurityLots() throws Exception {
        List<SecurityLot> lots = new ArrayList<SecurityLot>();

        transactionService.saveSecurityLots(lots);

        verify(transactionOperations).saveSecurityLots(lots);
    }
}