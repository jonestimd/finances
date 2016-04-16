package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.List;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.SecuritySummaryEvent;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class SecuritySummaryEventHandlerTest {
    private static final String[] DETAIL_PROPERTIES = {
            "id",
            TransactionDetail.TRANSACTION,
            TransactionDetail.ASSET_QUANTITY,
            TransactionDetail.MEMO
    };
    private static final String[] TRANSACTION_PROPERTIES = {
            "id",
            Transaction.ACCOUNT,
            Transaction.SECURITY
    };
    private final SecuritySummaryEventHandler eventHandler = new SecuritySummaryEventHandler(this);
    private final Security security1 = new SecurityBuilder().nextId().name("stock1").get();
    private final Security security2 = new SecurityBuilder().nextId().name("stock2").get();
    private final Account account = new AccountBuilder().nextId().name("account").get();
    private final Transaction transaction = new TransactionBuilder().nextId().account(account).security(security1).get();

    @Test
    public void noChanges() throws Exception {
        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();

        assertThat(events).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void addTransactionAndDetails() throws Exception {
        final TransactionDetail detail1 = newTransactionDetail(BigDecimal.TEN);
        final TransactionDetail detail2 = newTransactionDetail(BigDecimal.ONE);
        transaction.addDetails(detail1, detail2);

        eventHandler.added(detail1);
        eventHandler.added(detail2);
        eventHandler.added(transaction);

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifySecuritySummary(((SecuritySummaryEvent) events.get(0)), security1, 1L, new BigDecimal("11"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deleteTransactionAndDetails() throws Exception {
        final TransactionDetail detail1 = newTransactionDetail(BigDecimal.TEN);
        final TransactionDetail detail2 = newTransactionDetail(BigDecimal.ONE);
        transaction.addDetails(detail1, detail2);

        eventHandler.deleted(detail1, DETAIL_PROPERTIES, previousState(-1L, transaction, BigDecimal.TEN));
        eventHandler.deleted(detail2, DETAIL_PROPERTIES, previousState(-1L, transaction, BigDecimal.ONE));
        eventHandler.deleted(transaction, TRANSACTION_PROPERTIES, previousState(-1L, account, security1));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifySecuritySummary(((SecuritySummaryEvent) events.get(0)), security1, -1L, new BigDecimal("-11"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changeTransactionDetailShares() throws Exception {
        final TransactionDetail detail1 = newTransactionDetail(BigDecimal.ONE);
        transaction.addDetails(detail1);

        eventHandler.changed(detail1, DETAIL_PROPERTIES, previousState(-1L, transaction, BigDecimal.TEN));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifySecuritySummary(((SecuritySummaryEvent) events.get(0)), security1, 0L, new BigDecimal("-9"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changeTransactionSecurity() throws Exception {
        final TransactionDetail detail1 = newTransactionDetail(BigDecimal.TEN);
        transaction.addDetails(detail1);

        eventHandler.changed(transaction, TRANSACTION_PROPERTIES, previousState(-1L, account, security2));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(2);
        verifySecuritySummary(((SecuritySummaryEvent) events.get(0)), security2, -1L, new BigDecimal("-10"));
        verifySecuritySummary(((SecuritySummaryEvent) events.get(0)), security1, 1L, BigDecimal.TEN);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removeTransactionSecurity() throws Exception {
        final TransactionDetail detail1 = newTransactionDetail(BigDecimal.TEN);
        transaction.addDetails(detail1);
        transaction.setSecurity(null);

        eventHandler.changed(transaction, TRANSACTION_PROPERTIES, previousState(-1L, account, security1));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifySecuritySummary(((SecuritySummaryEvent) events.get(0)), security1, -1L, BigDecimal.TEN.negate());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setTransactionSecurity() throws Exception {
        final TransactionDetail detail1 = newTransactionDetail(BigDecimal.TEN);
        transaction.addDetails(detail1);

        eventHandler.changed(transaction, TRANSACTION_PROPERTIES, previousState(-1L, account, null));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(1);
        verifySecuritySummary(((SecuritySummaryEvent) events.get(0)), security1, 1L, BigDecimal.TEN);
    }

    @Test
    public void changeTransactionDetailMemo() throws Exception {
        final TransactionDetail detail1 = newTransactionDetail(BigDecimal.TEN);
        transaction.addDetails(detail1);

        eventHandler.changed(detail1, DETAIL_PROPERTIES, previousState(-1L, transaction, BigDecimal.TEN, "old memo"));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changeTransactionAccount() throws Exception {
        Account toAccount = new AccountBuilder().nextId().name("toAccount").get();
        transaction.setAccount(toAccount);
        transaction.addDetails(newTransactionDetail(BigDecimal.TEN));

        eventHandler.changed(transaction, TRANSACTION_PROPERTIES, previousState(-1L, account, security1));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(2);
        verifySecuritySummary((SecuritySummaryEvent) events.get(0), account, security1, -1L, BigDecimal.TEN.negate());
        verifySecuritySummary((SecuritySummaryEvent) events.get(0), toAccount, security1, 1L, BigDecimal.TEN);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void changeTransactionAccountAndSecurity() throws Exception {
        Account toAccount = new AccountBuilder().nextId().name("toAccount").get();
        transaction.setAccount(toAccount);
        transaction.addDetails(newTransactionDetail(BigDecimal.TEN));

        eventHandler.changed(transaction, TRANSACTION_PROPERTIES, previousState(-1L, account, security2));

        List<? extends DomainEvent<?, ?>> events = eventHandler.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDomainClass()).isEqualTo(SecuritySummary.class);
        assertThat(events.get(0).getDomainObjects()).hasSize(2);
        verifySecuritySummary((SecuritySummaryEvent) events.get(0), account, security2, -1L, BigDecimal.TEN.negate());
        verifySecuritySummary((SecuritySummaryEvent) events.get(0), toAccount, security1, 1L, BigDecimal.TEN);
    }

    private void verifySecuritySummary(SecuritySummaryEvent event, Account account, Security security, long transactionCount, BigDecimal sharesChange) {
        SecuritySummary summary = event.getDomainObjects(security.getId()).stream().filter(s -> s.getAccount().equals(account)).findFirst().get();
        verifySecuritySummary(summary, security, transactionCount, sharesChange);
    }

    private void verifySecuritySummary(SecuritySummaryEvent event, Security security, long transactionCount, BigDecimal sharesChange) {
        verifySecuritySummary(event.getDomainObject(security.getId()), security, transactionCount, sharesChange);
    }

    private void verifySecuritySummary(SecuritySummary securitySummary, Security security, long transactionCount, BigDecimal sharesChange) {
        assertThat(securitySummary.getTransactionAttribute()).isSameAs(security);
        assertThat(securitySummary.getTransactionCount()).isEqualTo(transactionCount);
        assertThat(securitySummary.getShares()).isEqualTo(sharesChange);
    }

    private Object[] previousState(Object ... values) {
        return values;
    }

    private TransactionDetail newTransactionDetail(BigDecimal shares) throws Exception {
        return new TransactionDetailBuilder().nextId()
                .amount(BigDecimal.TEN)
                .shares(shares).get();
    }
}
