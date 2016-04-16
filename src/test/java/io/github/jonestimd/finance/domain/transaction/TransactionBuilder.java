package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import io.github.jonestimd.finance.domain.TestSequence;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;

public class TransactionBuilder {
    private Long id;
    private Date date;
    private boolean cleared;
    private Account account;
    private Payee payee;
    private String memo;
    private Security security;
    private List<TransactionDetail> details = new ArrayList<>();

    public TransactionBuilder reset() {
        id = null;
        account = null;
        payee = null;
        details.clear();
        return this;
    }

    public TransactionBuilder nextId() {
        this.id = TestSequence.nextId();
        return this;
    }

    public TransactionBuilder date(Date date) {
        this.date = date;
        return this;
    }

    public TransactionBuilder account(Account account) {
        this.account = account;
        return this;
    }

    public TransactionBuilder cleared(boolean cleared) {
        this.cleared = cleared;
        return this;
    }

    public TransactionBuilder payee(Payee payee) {
        this.payee = payee;
        return this;
    }

    public TransactionBuilder memo(String memo) {
        this.memo = memo;
        return this;
    }

    public TransactionBuilder security(Security security) {
        this.security = security;
        return this;
    }

    public TransactionBuilder details(Stream<TransactionDetail> details) {
        details.forEach(this.details::add);
        return this;
    }

    public TransactionBuilder detailAmounts(BigDecimal... amounts) {
        details(Stream.of(amounts).map(TransactionBuilder::newDetail));
        return this;
    }

    public TransactionBuilder details(TransactionDetail ... details) {
        this.details.addAll(Arrays.asList(details));
        return this;
    }

    public Transaction get() {
        Transaction transaction = new Transaction(id);
        transaction.setDate(date);
        transaction.setAccount(account);
        transaction.setPayee(payee);
        transaction.setMemo(memo);
        transaction.setSecurity(security);
        transaction.setCleared(cleared);
        transaction.addDetails(details);
        return transaction;
    }

    private static TransactionDetail newDetail(BigDecimal amount) {
        return new TransactionDetail(new TransactionCategory(), amount, null, null);
    }
}
