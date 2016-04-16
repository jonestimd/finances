package io.github.jonestimd.finance.file;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class ReconcilerTest {
    @Test
    public void ignoreClearedTransactions() throws Exception {
        Transaction transaction1 = existingTransaction(null, true, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(null, false, BigDecimal.ONE, BigDecimal.TEN);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2);

        new Reconciler(tableModel).reconcile(Arrays.asList(newTransaction(null, false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(2);
        assertThat(transaction1.isCleared()).as("still cleared").isTrue();
        assertThat(transaction2.isCleared()).as("reconciled").isTrue();
    }

    @Test
    public void addTransactionIfNoMatch() throws Exception {
        Transaction transaction1 = existingTransaction(null, false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(null, false, BigDecimal.ONE);
        Transaction transaction3 = newTransaction(null, false, new BigDecimal("3.0"));
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, newTransaction(null, false, BigDecimal.ZERO));

        new Reconciler(tableModel).reconcile(Arrays.asList(transaction3));

        assertThat(tableModel.getBeanCount()).isEqualTo(4);
        assertThat(tableModel.getBean(2)).isSameAs(transaction3).as("add before 'new transaction' row");
        assertThat(transaction3.isCleared()).isTrue();
        assertThat(tableModel.getBean(0).isCleared()).isFalse();
        assertThat(tableModel.getBean(1).isCleared()).isFalse();
        assertThat(tableModel.getBean(3).isCleared()).isFalse();
    }

    @Test
    public void selectTransactionWithSameAmount() throws Exception {
        Transaction transaction1 = existingTransaction(null, false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(null, false, BigDecimal.ONE);
        Transaction transaction3 = existingTransaction(null, false, BigDecimal.TEN);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, transaction3);

        new Reconciler(tableModel).reconcile(Arrays.asList(newTransaction(null, false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(3);
        assertThat(transaction1.isCleared()).isTrue();
    }

    @Test
    public void selectTransactionWithSameAmountAndPayee() throws Exception {
        final Payee payee = new Payee("payee");
        Transaction transaction1 = existingTransaction(null, false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(payee, false, new BigDecimal("11.00"));
        Transaction transaction3 = existingTransaction(null, false, BigDecimal.TEN);
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, transaction3);

        new Reconciler(tableModel).reconcile(Arrays.asList(newTransaction(payee, false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(3);
        assertThat(transaction2.isCleared()).isTrue();
    }

    @Test
    public void selectFirstTransactionWithSameAmountAndPayee() throws Exception {
        final Payee payee = new Payee("payee");
        Transaction transaction1 = existingTransaction(null, false, BigDecimal.ONE, BigDecimal.TEN);
        Transaction transaction2 = existingTransaction(payee, false, new BigDecimal("11.00"));
        Transaction transaction3 = existingTransaction(payee, false, new BigDecimal("11.0"));
        TransactionTableModel tableModel = newTableModel(transaction1, transaction2, transaction3);

        new Reconciler(tableModel).reconcile(Arrays.asList(newTransaction(payee, false, new BigDecimal("11.0"))));

        assertThat(tableModel.getBeanCount()).isEqualTo(3);
        assertThat(transaction2.isCleared()).isTrue();
    }

    private TransactionTableModel newTableModel(Transaction ... transactions) {
        final TransactionTableModel model = new TransactionTableModel(null);
        model.setBeans(Arrays.asList(transactions));
        return model;
    }

    private Transaction newTransaction(Payee payee, boolean cleared, BigDecimal... amounts) {
        return transactionBuilder(payee, cleared, amounts).get();
    }

    private Transaction existingTransaction(Payee payee, boolean cleared, BigDecimal... amounts) {
        return transactionBuilder(payee, cleared, amounts).nextId().get();
    }

    private TransactionBuilder transactionBuilder(Payee payee, boolean cleared, BigDecimal... amounts) {
        return new TransactionBuilder().payee(payee).cleared(cleared).details(Stream.of(amounts).map(this::newDetail));
    }

    private TransactionDetail newDetail(BigDecimal amount) {
        return new TransactionDetail(null, amount, null, null);
    }
}