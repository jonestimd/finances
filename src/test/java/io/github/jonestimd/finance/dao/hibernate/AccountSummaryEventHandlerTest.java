package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.List;

import io.github.jonestimd.finance.domain.TestSequence;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class AccountSummaryEventHandlerTest {
    private static final String[] TRANSACTION_PROPERTIES = {"id", Transaction.ACCOUNT};
    private static final String[] DETAIL_PROPERTIES = {"id", TransactionDetail.TRANSACTION, TransactionDetail.AMOUNT, TransactionDetail.MEMO};
    private final AccountSummaryEventHandler eventHandler = new AccountSummaryEventHandler(this);

    @Test
    public void noChanges() throws Exception {
        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();

        assertThat(events).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addTransaction() throws Exception {
        final Account account = new Account(TestSequence.nextId());
        final Transaction transaction = newTransaction(account, BigDecimal.TEN, BigDecimal.ONE);

        eventHandler.added(transaction.getDetails().get(0));
        eventHandler.added(transaction.getDetails().get(1));
        eventHandler.added(transaction);

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(AccountSummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifyAccountSummary(((DomainEvent<Long, AccountSummary>) events.get(0)), account, 1L, new BigDecimal("11"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteTransaction() throws Exception {
        final Account account = new Account(TestSequence.nextId());
        final Transaction transaction = newTransaction(account);

        eventHandler.deleted(new TransactionDetail(null, BigDecimal.TEN, null, null), DETAIL_PROPERTIES, previousState(-1L, transaction));
        eventHandler.deleted(new TransactionDetail(null, BigDecimal.ONE, null, null), DETAIL_PROPERTIES, previousState(-1L, transaction));
        eventHandler.deleted(transaction, new String[0], new Object[0]);

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(AccountSummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifyAccountSummary(((DomainEvent<Long, AccountSummary>) events.get(0)), account, -1L, new BigDecimal("-11"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changeTransactionDetailAmount() throws Exception {
        final Account account = new Account(TestSequence.nextId());
        final Transaction transaction = newTransaction(account, BigDecimal.ONE);

        eventHandler.changed(transaction.getDetails().get(0), DETAIL_PROPERTIES, previousState(-1L, transaction, BigDecimal.TEN));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(AccountSummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifyAccountSummary(((DomainEvent<Long, AccountSummary>) events.get(0)), account, 0L, new BigDecimal("-9"));
    }

    @Test
    public void changeTransactionDetailMemo() throws Exception {
        final Account account = new Account(TestSequence.nextId());
        final Transaction transaction = newTransaction(account, BigDecimal.ONE);

        eventHandler.changed(transaction.getDetails().get(0), DETAIL_PROPERTIES, previousState(-1L, transaction, BigDecimal.ONE, "old memo"));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changeTransactionAccount() throws Exception {
        final Account account0 = new Account(TestSequence.nextId(), "account1");
        final Account account1 = new Account(TestSequence.nextId(), "account2");
        final Transaction transaction = newTransaction(account1, BigDecimal.ONE);

        eventHandler.changed(transaction, TRANSACTION_PROPERTIES, previousState(transaction.getId(), account0));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(AccountSummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(2);
        verifyAccountSummary(((DomainEvent<Long, AccountSummary>) events.get(0)), account0, -1L, new BigDecimal("-1"));
        verifyAccountSummary(((DomainEvent<Long, AccountSummary>) events.get(0)), account1, 1L, new BigDecimal("1"));
    }

    private void verifyAccountSummary(DomainEvent<Long, AccountSummary> event, Account account, long transactionCount, BigDecimal balanceChange) {
        final AccountSummary accountSummary = event.getDomainObject(account.getId());
        assertThat(accountSummary.getTransactionAttribute()).isSameAs(account);
        assertThat(accountSummary.getTransactionCount()).isEqualTo(transactionCount);
        assertThat(accountSummary.getBalance()).isEqualTo(balanceChange);
    }

    private Object[] previousState(Object ... values) {
        return values;
    }

    private Transaction newTransaction(Account account, BigDecimal ... amounts) {
        TransactionDetail[] details = new TransactionDetail[amounts.length];
        for (int i = 0; i < amounts.length; i++) {
            details[i] = new TransactionDetail(null, amounts[i], null, null);
        }
        return new TransactionBuilder().nextId()
                .account(account)
                .details(details).get();
    }
}
