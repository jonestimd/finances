package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.Date;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.TestSequence;
import io.github.jonestimd.finance.domain.asset.Security;

public class TransactionDetailBuilder {
    private Long id;
    private Long relatedId;
    private TransactionCategory category;
    private String memo;
    private BigDecimal amount = BigDecimal.ONE;
    private Security security;
    private BigDecimal shares;
    private Transaction transaction;

    public TransactionDetailBuilder reset() {
        id = null;
        relatedId = null;
        amount = BigDecimal.ONE;
        security = null;
        return this;
    }

    public TransactionDetailBuilder nextId() {
        this.id = TestSequence.nextId();
        this.relatedId = TestSequence.nextId();
        return this;
    }

    public TransactionDetailBuilder category(String code) {
        this.category = new TransactionCategory(code);
        return this;
    }

    public TransactionDetailBuilder memo(String memo) {
        this.memo = memo;
        return this;
    }

    public TransactionDetailBuilder amount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public TransactionDetailBuilder security(Security security) {
        this.security = security;
        return this;
    }

    public TransactionDetailBuilder shares(BigDecimal shares) {
        this.shares = shares;
        return this;
    }

    public TransactionDetailBuilder onTransaction() {
        transaction = new TransactionBuilder().nextId().get();
        return this;
    }

    public TransactionDetailBuilder onTransaction(Date date) {
        transaction = new TransactionBuilder().nextId().date(date).get();
        return this;
    }

    public TransactionDetail get() {
        return build(id, amount, security, shares);
    }

    private TransactionDetail build(Long detailId, BigDecimal detailAmount, Security security, BigDecimal shares) {
        TransactionDetail transactionDetail = TestDomainUtils.create(TransactionDetail.class, detailId);
        transactionDetail.setMemo(memo);
        transactionDetail.setCategory(category);
        transactionDetail.setAmount(detailAmount);
        transactionDetail.setExchangeAsset(security);
        transactionDetail.setAssetQuantity(shares);
        transactionDetail.setTransaction(transaction);
        return transactionDetail;
    }

    public TransactionDetail persistedTransfer() {
        TransactionDetail transactionDetail = get();
        transactionDetail.setRelatedDetail(build(relatedId, amount.negate(), null, null));
        transactionDetail.getRelatedDetail().setTransaction(new TransactionBuilder().nextId().get());
        return transactionDetail;
    }

    public TransactionDetail newTransfer() {
        TransactionDetail transactionDetail = get();
        transactionDetail.setRelatedDetail(build(null, amount.negate(), null, null));
        transactionDetail.getRelatedDetail().setTransaction(new TransactionBuilder().get());
        return transactionDetail;
    }
}
