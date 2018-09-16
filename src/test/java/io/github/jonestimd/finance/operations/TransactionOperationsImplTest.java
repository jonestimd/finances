package io.github.jonestimd.finance.operations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.MockDaoContext;
import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.SecurityLotDao;
import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.dao.TransactionDetailDao;
import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.SecurityLotBuilder;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import io.github.jonestimd.mockito.MockitoHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.*;

public class TransactionOperationsImplTest {
    private MockDaoContext daoRepository = new MockDaoContext();
    private TransactionDao transactionDao = daoRepository.getTransactionDao();
    private TransactionDetailDao transactionDetailDao = daoRepository.getTransactionDetailDao();
    private TransactionCategoryDao categoryDao = daoRepository.getTransactionCategoryDao();
    private PayeeDao payeeDao = daoRepository.getPayeeDao();
    private SecurityDao securityDao = daoRepository.getSecurityDao();
    private SecurityLotDao securityLotDao = daoRepository.getSecurityLotDao();
    private TransactionOperations transactionOperations = new TransactionOperationsImpl(daoRepository);

    @Test
    public void saveDetailCallsDao() throws Exception {
        daoRepository.expectCommit();
        TransactionDetail detail = new TransactionDetail();

        transactionOperations.saveDetail(detail);

        verify(transactionDetailDao).save(detail);
    }

    @Test
    public void saveTransactionPersistsNewPayee() throws Exception {
        daoRepository.expectCommit();
        Payee payee = new Payee("unsaved payee");
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        TransactionDetail transferDetail = new TransactionDetailBuilder().persistedTransfer();
        transaction.addDetails(transferDetail, new TransactionDetail());

        transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        InOrder inOrder = inOrder(payeeDao, transactionDao);
        inOrder.verify(payeeDao).save(same(payee));
        inOrder.verify(transactionDao).save(transaction);
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, categoryDao, payeeDao, securityLotDao);
    }

    @Test
    public void saveTransactionPersistsNewSecurity() throws Exception {
        daoRepository.expectCommit();
        Security newSecurity = new Security("unsaved Security", SecurityType.STOCK);
        Transaction transaction = new TransactionBuilder().security(newSecurity).get();
        transaction.addDetails(new TransactionDetailBuilder().shares(BigDecimal.ONE).amount(BigDecimal.TEN).get());
        transaction.addDetails(new TransactionDetailBuilder().shares(BigDecimal.ONE).amount(BigDecimal.TEN).get());

        transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        InOrder inOrder = inOrder(securityDao, transactionDao);
        inOrder.verify(securityDao).save(same(newSecurity));
        inOrder.verify(transactionDao).save(transaction);
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, categoryDao, securityDao, securityLotDao);
    }

    @Test
    public void saveTransactionPersistsNewCategories() throws Exception {
        daoRepository.expectCommit();
        TransactionDetail detail = new TransactionDetailBuilder().category("category").amount(BigDecimal.TEN).get();
        Transaction transaction = new TransactionBuilder().details(detail).get();

        transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        InOrder inOrder = inOrder(categoryDao, transactionDao);
        inOrder.verify(categoryDao).save(same(detail.getCategory()));
        inOrder.verify(transactionDao).save(transaction);
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, securityDao, securityLotDao);
    }

    @Test
    public void saveTransactionWithExistingPayee() throws Exception {
        daoRepository.expectCommit();
        Payee payee = new Payee("unsaved payee");
        TestDomainUtils.setId(payee, -1L);
        Transaction transaction = new TransactionBuilder().payee(payee).get();
        TransactionDetail transferDetail = new TransactionDetailBuilder().persistedTransfer();
        transaction.addDetails(transferDetail, new TransactionDetail());

        transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        verify(transactionDao).save(transaction);
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, payeeDao, securityLotDao);
    }

    @Test
    public void saveNewTransactionRemovesEmptyDetails() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new Transaction();
        TransactionDetail transferDetail = new TransactionDetailBuilder().persistedTransfer();
        transaction.addDetails(transferDetail, new TransactionDetail());

        Transaction updatedTransaction = transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        verify(transactionDao).save(transaction);
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, payeeDao, securityLotDao);
        assertThat(updatedTransaction).isSameAs(transaction);
        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getDetails().get(0)).isSameAs(transferDetail);
    }

    @Test
    public void saveExistingTransactionRemovesEmptyDetails() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new Transaction(-1L);
        TransactionDetail transferDetail = new TransactionDetailBuilder().persistedTransfer();
        transaction.addDetails(transferDetail, new TransactionDetail());

        Transaction updatedTransaction = transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        InOrder inOrder = inOrder(transactionDao, transactionDetailDao);
        inOrder.verify(transactionDao).merge(transaction);
        inOrder.verify(transactionDetailDao).findOrphanTransfers();
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, securityLotDao);
        assertThat(updatedTransaction).isSameAs(transaction);
        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getDetails().get(0)).isSameAs(transferDetail);
    }

    @Test
    public void saveExistingTransactionDeletesOrphanDetails() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new Transaction(-1L);
        TransactionDetail orphanDetail = new TransactionDetail();
        Transaction orphanTransfer = new Transaction(null, null, null, false, null, orphanDetail, new TransactionDetail());
        when(transactionDetailDao.findOrphanTransfers())
            .thenReturn(Collections.singletonList(orphanDetail));

        Transaction updatedTransaction = transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        InOrder inOrder = inOrder(transactionDao, transactionDetailDao, securityLotDao);
        inOrder.verify(transactionDao).merge(transaction);
        inOrder.verify(transactionDetailDao).findOrphanTransfers();
        inOrder.verify(securityLotDao).deleteSaleLots(orphanDetail);
        inOrder.verify(transactionDetailDao).delete(orphanDetail);
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, securityLotDao);
        assertThat(updatedTransaction).isSameAs(transaction);
        assertThat(orphanDetail.getTransaction()).isNull();
        assertThat(orphanTransfer.getDetails().contains(orphanDetail)).isFalse();
    }

    @Test
    public void saveExistingTransactionDeletesOrphanTransfer() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new Transaction(-1L);
        TransactionDetail orphanDetail = new TransactionDetail();
        Transaction orphanTransfer = new Transaction(null, null, null, false, null, orphanDetail);
        when(transactionDetailDao.findOrphanTransfers())
            .thenReturn(Collections.singletonList(orphanDetail));

        Transaction updatedTransaction = transactionOperations.saveTransaction(new TransactionUpdate(transaction));

        InOrder inOrder = inOrder(transactionDao, transactionDetailDao, securityLotDao);
        inOrder.verify(transactionDao).merge(transaction);
        inOrder.verify(transactionDetailDao).findOrphanTransfers();
        inOrder.verify(securityLotDao).deleteSaleLots(orphanDetail);
        inOrder.verify(transactionDao).delete(orphanTransfer);
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, securityLotDao);
        assertThat(updatedTransaction).isSameAs(transaction);
    }

    @Test
    public void saveExistingTransactionWithDeletedDetails() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new TransactionBuilder().nextId()
            .details(new TransactionDetailBuilder().nextId().get())
            .details(new TransactionDetailBuilder().nextId().get()).get();
        TransactionUpdate update = new TransactionUpdate(transaction, transaction.getDetails().subList(0, 1));

        Transaction updatedTransaction = transactionOperations.saveTransaction(update);

        InOrder inOrder = inOrder(transactionDao, transactionDetailDao, securityLotDao);
        inOrder.verify(transactionDao).merge(transaction);
        inOrder.verify(securityLotDao).deleteSaleLots(update.getDeletes().get(0));
        inOrder.verify(transactionDetailDao).delete(update.getDeletes().get(0));
        inOrder.verify(transactionDetailDao).findOrphanTransfers();
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, securityLotDao);
        assertThat(updatedTransaction).isSameAs(transaction);
        assertThat(updatedTransaction.getDetails()).hasSize(1);
        assertThat(updatedTransaction.getDetails().contains(update.getDeletes().get(0))).isFalse();
    }

    @Test
    public void saveExistingTransactionDeletesTransferTransaction() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new TransactionBuilder().nextId()
            .details(new TransactionDetailBuilder().nextId().persistedTransfer())
            .details(new TransactionDetailBuilder().nextId().get()).get();
        TransactionUpdate update = new TransactionUpdate(transaction, transaction.getDetails().subList(0, 1));
        TransactionDetail relatedDetail = update.getDeletes().get(0).getRelatedDetail();
        Transaction relatedTransaction = relatedDetail.getTransaction();

        Transaction updatedTransaction = transactionOperations.saveTransaction(update);

        InOrder inOrder = inOrder(transactionDao, transactionDetailDao, securityLotDao);
        inOrder.verify(transactionDao).merge(transaction);
        inOrder.verify(securityLotDao).deleteSaleLots(update.getDeletes().get(0));
        inOrder.verify(securityLotDao).deleteSaleLots(relatedDetail);
        inOrder.verify(transactionDao).delete(relatedTransaction);
        inOrder.verify(transactionDetailDao).delete(update.getDeletes().get(0));
        inOrder.verify(transactionDetailDao).findOrphanTransfers();
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, securityLotDao);
        assertThat(updatedTransaction).isSameAs(transaction);
        assertThat(updatedTransaction.getDetails()).hasSize(1);
        assertThat(updatedTransaction.getDetails().contains(update.getDeletes().get(0))).isFalse();
    }

    @Test
    public void saveExistingTransactionWithDeletesTransferDetail() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new TransactionBuilder().nextId()
            .details(new TransactionDetailBuilder().nextId().persistedTransfer())
            .details(new TransactionDetailBuilder().nextId().get()).get();
        TransactionUpdate update = new TransactionUpdate(transaction, transaction.getDetails().subList(0, 1));
        TransactionDetail relatedDetail = update.getDeletes().get(0).getRelatedDetail();
        relatedDetail.getTransaction().addDetails(new TransactionDetailBuilder().nextId().get());

        Transaction updatedTransaction = transactionOperations.saveTransaction(update);

        InOrder inOrder = inOrder(transactionDao, transactionDetailDao, securityLotDao);
        inOrder.verify(transactionDao).merge(transaction);
        inOrder.verify(securityLotDao).deleteSaleLots(update.getDeletes().get(0));
        inOrder.verify(securityLotDao).deleteSaleLots(relatedDetail);
        inOrder.verify(transactionDetailDao).delete(relatedDetail);
        inOrder.verify(transactionDetailDao).delete(update.getDeletes().get(0));
        inOrder.verify(transactionDetailDao).findOrphanTransfers();
        verifyNoMoreInteractions(transactionDao, transactionDetailDao, securityLotDao);
        assertThat(updatedTransaction).isSameAs(transaction);
        assertThat(updatedTransaction.getDetails()).hasSize(1);
        assertThat(updatedTransaction.getDetails().contains(update.getDeletes().get(0))).isFalse();
        assertThat(relatedDetail.getTransaction()).isNull();
    }

    @Test
    public void moveTransactionSetsAccount() throws Exception {
        Transaction transaction = new Transaction();
        Account account = new Account();

        transactionOperations.moveTransaction(transaction, account);

        assertThat(transaction.getAccount()).isSameAs(account);
        verify(transactionDao).merge(transaction);
    }

    @Test
    public void moveTransactionSetsAccountOnTransfer() throws Exception {
        Account account1 = new Account(1L);
        Account account2 = new Account(2L);
        Transaction transaction = new Transaction(account1, new Date(), null, false, null, TransactionDetail.newTransfer(account2, BigDecimal.ONE));

        transactionOperations.moveTransaction(transaction, account2);

        assertThat(transaction.getAccount()).isSameAs(account2);
        assertThat(transaction.getDetails().get(0).getRelatedDetail().getTransaction().getAccount()).isSameAs(account1);
        verify(transactionDao).merge(transaction);
    }

    @Test
    public void saveAllCallsDao() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction1 = new Transaction();
        Transaction transaction2 = new Transaction();
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
        when(transactionDao.saveAll(transactions)).thenReturn(transactions);

        assertThat(transactionOperations.saveTransactions(transactions)).isSameAs(transactions);

        verify(transactionDao).saveAll(transactions);
    }

    @Test
    public void deleteCallsDao() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new Transaction();

        transactionOperations.deleteTransaction(transaction);

        verify(transactionDao).delete(transaction);
        verifyNoMoreInteractions(securityLotDao);
    }

    @Test
    public void deleteDeletesEmptyTransferTransaction() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new TransactionBuilder().nextId()
            .details(new TransactionDetailBuilder().nextId().persistedTransfer()).get();

        transactionOperations.deleteTransaction(transaction);

        InOrder inOrder = inOrder(transactionDao, securityLotDao);
        inOrder.verify(transactionDao).delete(transaction.getDetails().get(0).getRelatedDetail().getTransaction());
        inOrder.verify(transactionDao).delete(transaction);
        verifyNoMoreInteractions(transactionDao, securityLotDao);
    }

    @Test
    public void deleteDoesNotDeleteNonEmptyTransferTransaction() throws Exception {
        daoRepository.expectCommit();
        TransactionDetail detail = new TransactionDetailBuilder().nextId().persistedTransfer();
        Transaction transaction = new TransactionBuilder().nextId().details(detail).get();
        Transaction relatedTransaction = detail.getRelatedDetail().getTransaction();
        relatedTransaction.addDetails(new TransactionDetailBuilder().nextId().get());

        transactionOperations.deleteTransaction(transaction);

        verify(transactionDao).delete(transaction);
        verifyNoMoreInteractions(transactionDao, securityLotDao);
        assertThat(relatedTransaction.getDetails()).hasSize(1);
    }

    @Test
    public void deleteRemovesLotsForSecuritySale() throws Exception {
        daoRepository.expectCommit();
        TransactionDetail detail = new TransactionDetailBuilder().nextId().category(SecurityAction.SELL.code()).get();
        Transaction transaction = new TransactionBuilder().nextId().details(detail).get();

        transactionOperations.deleteTransaction(transaction);

        verify(transactionDao).delete(transaction);
        verify(securityLotDao).deleteSaleLots(detail);
        verifyNoMoreInteractions(transactionDao, securityLotDao);
    }

    @Test
    public void getTransactionsByAccountId() throws Exception {
        daoRepository.expectCommit();
        final long accountId = 1;
        List<Transaction> expectedTransactions = Collections.singletonList(new Transaction());
        when(transactionDao.getTransactions(accountId)).thenReturn(expectedTransactions);

        assertThat(transactionOperations.getTransactions(accountId)).isSameAs(expectedTransactions);
    }

    @Test
    public void findLatestForPayee() throws Exception {
        daoRepository.expectCommit();
        Transaction transaction = new Transaction();
        when(transactionDao.findLatestForPayee(anyLong())).thenReturn(transaction);

        assertThat(transactionOperations.findLatestForPayee(-1L)).isSameAs(transaction);

        verify(transactionDao).findLatestForPayee(-1L);
    }

    @Test
    public void findSecuritySalesWithoutLots() throws Exception {
        daoRepository.expectCommit();
        List<TransactionDetail> expectedDetails = Collections.singletonList(new TransactionDetail());
        Date saleDate = new Date();
        when(transactionDetailDao.findSecuritySalesWithoutLots("security 1", saleDate)).thenReturn(expectedDetails);

        assertThat(transactionOperations.findSecuritySalesWithoutLots("security 1", saleDate)).isSameAs(expectedDetails);
    }

    @Test
    public void saveAllSecurityLotsIgnoresEmptyUnsavedLots() throws Exception {
        daoRepository.expectCommit();
        List<SecurityLot> lots = Lists.newArrayList(
                new SecurityLot(),
                new SecurityLotBuilder().nextId().purchase(new TransactionDetail(), BigDecimal.TEN).get());

        transactionOperations.saveSecurityLots(lots);

        ArgumentCaptor<Stream<SecurityLot>> captor = MockitoHelper.captor();
        verify(securityLotDao).deleteAll(captor.capture());
        assertThat(captor.getValue().count()).isEqualTo(0);
        verify(securityLotDao).saveAll(captor.capture());
        List<SecurityLot> saved = captor.getValue().collect(Collectors.toList());
        assertThat(saved).containsOnly(lots.get(1));
    }

    @Test
    public void saveAllSecurityLotsDeletesEmptyExistingLots() throws Exception {
        daoRepository.expectCommit();
        List<SecurityLot> lots = Lists.newArrayList(
                new SecurityLotBuilder().nextId().get(),
                new SecurityLotBuilder().nextId().purchase(null, BigDecimal.ZERO).get(),
                new SecurityLotBuilder().nextId().purchase(null, BigDecimal.ZERO.setScale(6)).get(),
                new SecurityLotBuilder().nextId().purchase(new TransactionDetail(), BigDecimal.TEN).get());

        transactionOperations.saveSecurityLots(lots);

        ArgumentCaptor<Stream<SecurityLot>> captor = MockitoHelper.captor();
        verify(securityLotDao).deleteAll(captor.capture());
        List<SecurityLot> deleted = captor.getValue().collect(Collectors.toList());
        assertThat(deleted).containsOnly(lots.get(0), lots.get(1), lots.get(2));
        verify(securityLotDao).saveAll(captor.capture());
        List<SecurityLot> saved = captor.getValue().collect(Collectors.toList());
        assertThat(saved).containsOnly(lots.get(3));
    }

    @Test
    public void findAvailableLotsReturnsExistingLots() throws Exception {
        List<SecurityLot> lots = new ArrayList<>();
        when(securityLotDao.findBySale(any(TransactionDetail.class)))
            .thenReturn(lots);

        TransactionDetail sale = new TransactionDetail();
        assertThat(transactionOperations.findAvailableLots(sale)).isSameAs(lots);

        verify(securityLotDao).findBySale(same(sale));
    }

    @Test
    public void findAvaliableLotsReturnsLotsForUnallocatedShares() throws Exception {
        Security security = new Security();
        TransactionDetail sale = new TransactionDetailBuilder().nextId().onTransaction().get();
        TransactionDetail allocatedPurchase = new TransactionDetailBuilder().nextId().onTransaction().get();
        allocatedPurchase.getTransaction().setSecurity(security);
        TransactionDetail unallocatedPurchase = new TransactionDetailBuilder().nextId().onTransaction().get();
        unallocatedPurchase.getTransaction().setSecurity(security);
        List<SecurityLot> lots = Lists.newArrayList(
                new SecurityLotBuilder().purchase(allocatedPurchase, null).get());
        when(securityLotDao.findBySale(any(TransactionDetail.class)))
            .thenReturn(lots);
        when(transactionDetailDao.findAvailablePurchaseShares(any(TransactionDetail.class)))
            .thenReturn(Arrays.asList(allocatedPurchase, unallocatedPurchase));

        assertThat(transactionOperations.findAvailableLots(sale)).isSameAs(lots);

        verify(securityLotDao).findBySale(same(sale));
        verify(transactionDetailDao).findAvailablePurchaseShares(same(sale));
        assertThat(lots).hasSize(2);
        assertThat(lots.get(0).getPurchase()).isSameAs(allocatedPurchase);
        assertThat(lots.get(1).getId()).isNull();
        assertThat(lots.get(1).getPurchase()).isSameAs(unallocatedPurchase);
        assertThat(lots.get(1).getPurchaseShares().compareTo(BigDecimal.ZERO)).isEqualTo(0);
        assertThat(lots.get(1).getSale()).isSameAs(sale);
        assertThat(lots.get(1).getSaleShares()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void findPurchasesWithRemainingLots() throws Exception {
        Account account = new Account();
        Security security = new Security();
        Date purchaseDate = new Date();
        List<TransactionDetail> details = Arrays.asList(
                createTransaction(security, BigDecimal.ONE, BigDecimal.valueOf(3L)),
                createTransaction(security),
                createTransaction(security, BigDecimal.valueOf(2L)));
        daoRepository.expectCommit();
        when(transactionDetailDao.findPurchasesWithRemainingShares(account, security, purchaseDate)).thenReturn(details);

        List<TransactionDetail> purchases = transactionOperations.findPurchasesWithRemainingLots(account, security, purchaseDate);

        assertThat(purchases.size()).isEqualTo(3);
        assertThat(purchases.get(0)).isSameAs(details.get(0));
        assertThat(purchases.get(0).getRemainingShares().compareTo(BigDecimal.valueOf(6L))).isEqualTo(0);
        assertThat(purchases.get(1)).isSameAs(details.get(1));
        assertThat(purchases.get(1).getRemainingShares()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(purchases.get(2)).isSameAs(details.get(2));
        assertThat(purchases.get(2).getRemainingShares()).isEqualByComparingTo(BigDecimal.valueOf(8L));
    }

    private TransactionDetail createTransaction(Security security, BigDecimal... lotShares) throws Exception {
        TransactionDetail detail = new TransactionDetailBuilder().shares(BigDecimal.TEN).amount(BigDecimal.ZERO).onTransaction().get();
        detail.getTransaction().setSecurity(security);
        for (BigDecimal lotShare : lotShares) {
            new SecurityLot(detail, new TransactionDetailBuilder().onTransaction().get(), lotShare);
        }
        return detail;
    }

    @Test
    public void findAllDetails() throws Exception {
        String searchText = "search text";
        ArrayList<TransactionDetail> details = new ArrayList<>();
        when(transactionDetailDao.findAll(anyString())).thenReturn(details);

        assertThat(transactionOperations.findAllDetails(searchText)).isSameAs(details);

        verify(transactionDetailDao).findAll(searchText);
    }
}