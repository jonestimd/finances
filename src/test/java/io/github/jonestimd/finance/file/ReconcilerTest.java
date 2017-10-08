package io.github.jonestimd.finance.file;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static java.util.Collections.*;
import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.*;

public class ReconcilerTest {
    @Test
    public void ignoreClearedTransactions() throws Exception {
        Transaction transaction1 = existingTransaction(true, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(false, BigDecimal.ONE, BigDecimal.TEN);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2);

        new Reconciler(tableModel).reconcile(singletonList(newTransaction(false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(2);
        assertThat(transaction1.isCleared()).as("still cleared").isTrue();
        assertThat(transaction2.isCleared()).as("reconciled").isTrue();
    }

    @Test
    public void ignoreUnsavedTransactions() throws Exception {
        Transaction transaction1 = newTransaction(false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(false, BigDecimal.ONE, BigDecimal.TEN);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2);

        new Reconciler(tableModel).reconcile(singletonList(newTransaction(false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(2);
        assertThat(transaction1.isCleared()).isFalse();
        assertThat(transaction2.isCleared()).as("reconciled").isTrue();
    }

    @Test
    public void addTransactionIfNoMatch() throws Exception {
        Transaction transaction1 = existingTransaction(false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(false, BigDecimal.ONE);
        Transaction transaction3 = newTransaction(false, new BigDecimal("3.0"));
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, newTransaction(false, BigDecimal.ZERO));

        new Reconciler(tableModel).reconcile(singletonList(transaction3));

        assertThat(tableModel.getBeanCount()).isEqualTo(4);
        assertThat(tableModel.getBean(2)).isSameAs(transaction3).as("add before 'new transaction' row");
        assertThat(transaction3.isCleared()).isTrue();
        assertThat(tableModel.getBean(0).isCleared()).isFalse();
        assertThat(tableModel.getBean(1).isCleared()).isFalse();
        assertThat(tableModel.getBean(3).isCleared()).isFalse();
    }

    @Test
    public void selectTransactionWithSameAmount() throws Exception {
        Transaction transaction1 = existingTransaction(false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(false, BigDecimal.ONE);
        Transaction transaction3 = existingTransaction(false, BigDecimal.TEN);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, transaction3);

        new Reconciler(tableModel).reconcile(singletonList(newTransaction(false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(3);
        assertThat(transaction1.isCleared()).isTrue();
    }

    @Test
    public void selectTransactionWithSameAmountAndPayee() throws Exception {
        final Payee payee = new Payee("payee");
        Transaction transaction1 = existingTransaction(false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(payee, false, new BigDecimal("11.00"));
        Transaction transaction3 = existingTransaction(false, BigDecimal.TEN);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, transaction3);

        new Reconciler(tableModel).reconcile(singletonList(newTransaction(payee, false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(3);
        assertThat(transaction2.isCleared()).isTrue();
    }

    @Test
    public void selectNearestDateTransactionWithSameAmountAndPayee() throws Exception {
        Date date = new Date();
        final Payee payee = new Payee("payee");
        Transaction transaction1 = existingTransaction(false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = transactionBuilder(false, new BigDecimal("11.00")).nextId().payee(payee).date(addDays(date, -2)).get();
        Transaction transaction3 = transactionBuilder(false, new BigDecimal("11.0")).nextId().payee(payee).date(addDays(date, 1)).get();
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, transaction3);

        new Reconciler(tableModel).reconcile(singletonList(transactionBuilder(false, new BigDecimal("11.0")).payee(payee).date(date).get()));

        assertThat(tableModel.getBeanCount()).isEqualTo(3);
        assertThat(transaction3.isCleared()).isTrue();
    }

    @Test
    public void selectTransactionWithSameAmountAndSecurity() throws Exception {
        final Security security = new Security("security", SecurityType.STOCK);
        Transaction transaction1 = existingTransaction(new Security("other", SecurityType.MUTUAL_FUND), false, BigDecimal.TEN, BigDecimal.ONE);
        Transaction transaction2 = existingTransaction(security, false, new BigDecimal("11.00"), BigDecimal.ONE);
        Transaction transaction3 = existingTransaction(security, false, BigDecimal.TEN, BigDecimal.TEN);
        Transaction transaction4 = existingTransaction(security, false, BigDecimal.TEN, BigDecimal.ONE);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, transaction3, transaction4);

        new Reconciler(tableModel).reconcile(singletonList(newTransaction(security, false, BigDecimal.TEN, BigDecimal.ONE)));

        assertThat(tableModel.getBeanCount()).isEqualTo(4);
        assertThat(transaction4.isCleared()).isTrue();
    }

    private TransactionTableModel newTableModel(Transaction ... transactions) {
        final TransactionTableModel model = new TransactionTableModel(null);
        model.setBeans(Arrays.asList(transactions));
        return model;
    }

    private Transaction newTransaction(boolean cleared, BigDecimal... amounts) {
        return transactionBuilder(cleared, amounts).get();
    }

    private Transaction newTransaction(Payee payee, boolean cleared, BigDecimal... amounts) {
        return transactionBuilder(cleared, amounts).payee(payee).get();
    }

    private Transaction newTransaction(Security security, boolean cleared, BigDecimal amount, BigDecimal shares) {
        return transactionBuilder(cleared).security(security).details(newDetail(amount, shares)).get();
    }

    private Transaction existingTransaction(boolean cleared, BigDecimal... amounts) {
        return transactionBuilder(cleared, amounts).nextId().get();
    }

    private Transaction existingTransaction(Payee payee, boolean cleared, BigDecimal... amounts) {
        return transactionBuilder(cleared, amounts).payee(payee).nextId().get();
    }

    private Transaction existingTransaction(Security security, boolean cleared, BigDecimal amount, BigDecimal shares) {
        return transactionBuilder(cleared).security(security).details(newDetail(amount, shares)).nextId().get();
    }

    private TransactionBuilder transactionBuilder(boolean cleared, BigDecimal... amounts) {
        return new TransactionBuilder().cleared(cleared).details(Stream.of(amounts).map(this::newDetail));
    }

    private TransactionDetail newDetail(BigDecimal amount) {
        return new TransactionDetail(null, amount, null, null);
    }

    private TransactionDetail newDetail(BigDecimal amount, BigDecimal shares) {
        return new TransactionDetailBuilder().amount(amount).shares(shares).get();
    }
}