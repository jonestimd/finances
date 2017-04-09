package io.github.jonestimd.finance.dao.hibernate;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.dao.TransactionalTestFixture;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.TransientObjectException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TransactionDaoImpTest extends TransactionalTestFixture {
    private static final QueryBatch[] SETUP_BATCH = {
            ACCOUNT_BATCH,
            TRANSACTION_CATEGORY_BATCH,
            PAYEE_BATCH,
            SECURITY_BATCH,
    };

    private TransactionCategoryDao transactionCategoryDao;
    private PayeeDao payeeDao;
    private TransactionDao transactionDao;
    private AccountDao accountDao;
    private Account account;
    private SecurityDao securityDao;

    @Before
    public void setUpDaos() throws Exception {
        this.transactionCategoryDao = daoContext.getTransactionCategoryDao();
        this.payeeDao = daoContext.getPayeeDao();
        this.transactionDao = daoContext.getTransactionDao();
        this.accountDao = daoContext.getAccountDao();
        account = accountDao.get((Long) ACCOUNT_BATCH.getValue(0, "id"));
        this.securityDao = daoContext.getSecurityDao();
    }

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(SETUP_BATCH);
    }

    @Test
    public void testSaveTransactionCascadesDetails() throws Exception {
        Transaction transaction = createTransaction();

        transaction = transactionDao.save(transaction);

        Transaction savedTransaction = transactionDao.get(transaction.getId());
        assertThat(savedTransaction.getDetails()).isNotEmpty();
        assertThat(savedTransaction.getDetails().get(0).getTransaction()).isSameAs(savedTransaction);
    }

    @Test
    public void testSaveTransactionDoesNotCascadePayee() throws Exception {
        Transaction transaction = createTransaction();
        transaction.setPayee(new Payee("unsaved payee"));

        transaction = transactionDao.save(transaction);

        assertThat(transaction.getPayee().getId()).isNull();
        Transaction savedTransaction = transactionDao.get(transaction.getId());
        assertThat(savedTransaction.getPayee().getId()).isNull();
    }

    @Test
    public void testMergeTransactionDoesNotCascadePayee() throws Exception {
        Transaction transaction = createTransaction();
        transaction.setPayee(new Payee("unsaved payee"));

        try {
            transactionDao.merge(transaction);
        } catch (UndeclaredThrowableException ex) {
            assertThat(ex.getCause().getCause()).isInstanceOf(TransientObjectException.class);
            assertThat(ex.getCause().getCause().getMessage()).endsWith(Payee.class.getName());
        }
    }

    @Test
    public void testSaveTransactionDoesNotCascadeSecurity() throws Exception {
        Transaction transaction = createTransaction();
        transaction.setSecurity(new Security("unsaved security", SecurityType.STOCK));

        transaction = transactionDao.save(transaction);

        assertThat(transaction.getSecurity().getId()).isNull();
        Transaction savedTransaction = transactionDao.get(transaction.getId());
        assertThat(savedTransaction.getDetails()).isNotEmpty();
        assertThat(savedTransaction.getDetails().get(0).getTransaction()).isSameAs(savedTransaction);
    }

    @Test
    public void testMergeTransactionDoesNotCascadeSecurity() throws Exception {
        Security security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        clearSession();
        security.setName("unsaved security");
        Transaction transaction = createTransaction();
        transaction.setSecurity(security);

        transaction = transactionDao.merge(transaction);

        assertThat(transaction.getSecurity().getId()).isEqualTo(security.getId());
        assertThat(transaction.getSecurity().getName()).isEqualTo("Stock 111");
        Transaction savedTransaction = transactionDao.get(transaction.getId());
        assertThat(savedTransaction.getDetails()).isNotEmpty();
        assertThat(savedTransaction.getDetails().get(0).getTransaction()).isSameAs(savedTransaction);
    }

    private Transaction createTransaction() {
        return createTransaction(new Date());
    }

    private Transaction createTransaction(Date date) {
        Payee payee = payeeDao.getPayee((String) PAYEE_BATCH.getValue(0, "name"));
        TransactionCategory type = transactionCategoryDao.getTransactionCategory((String) TRANSACTION_CATEGORY_BATCH.getValue(0, "code"));
        TransactionDetail detail = new TransactionDetail(type, new BigDecimal(123.45d), null, null);
        return new Transaction(account, date, payee, true, null, detail);
    }

    private Transaction[] createTransfer(double amount) {
        Account account1 = accountDao.get((Long) ACCOUNT_BATCH.getValue(0, "id"));
        Account account2 = accountDao.get((Long) ACCOUNT_BATCH.getValue(2, "id"));
        Payee payee = payeeDao.getPayee((String) PAYEE_BATCH.getValue(0, "name"));
        TransactionDetail detail1 = new TransactionDetail(new BigDecimal(amount), null, null);
        Date date = new Date();
        Transaction transaction1 = new Transaction(account1, date, payee, true, null, detail1);
        TransactionDetail detail2 = detail1.getRelatedDetail();
        Transaction transaction2 = new Transaction(account2, date, payee, true, null, detail2);
        return new Transaction[]{transaction1, transaction2};
    }

    @Test
    public void testTransferMappingIsValid() throws Exception {
        Transaction[] transactions = createTransfer(123.45);

        Transaction saved1 = transactionDao.save(transactions[0]);
        Transaction saved2 = transactionDao.save(transactions[1]);

        TransactionDetail savedDetail1 = saved1.getDetails().get(0);
        TransactionDetail savedDetail2 = saved2.getDetails().get(0);
        assertThat(saved1.getId()).isNotNull();
        assertThat(savedDetail1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(savedDetail2.getId()).isNotNull();
        assertThat(savedDetail2.getRelatedDetail().getId()).isEqualTo(savedDetail1.getId());
        assertThat(savedDetail1.getRelatedDetail().getId()).isEqualTo(savedDetail2.getId());
        assertThat(savedDetail2.getRelatedDetail().getTransaction().getId()).isEqualTo(saved1.getId());
        assertThat(savedDetail1.getRelatedDetail().getTransaction().getId()).isEqualTo(saved2.getId());
    }

    @Test
    public void testGetTransactionsByAccountId() throws Exception {
        Transaction transaction = createTransaction();
        TransactionDetail detail = new TransactionDetail();
        detail.setAmount(new BigDecimal("987.65"));
        transaction.addDetails(detail);
        transactionDao.save(transaction);

        List<Transaction> transactions = transactionDao.getTransactions(transaction.getAccount().getId());

        assertThat(transactions).isNotEmpty();
        assertThat(transactions.size()).isEqualTo(new HashSet<Transaction>(transactions).size()).as("no duplicates");
        assertThat(getTransaction(transactions, transaction.getId())).isNotNull();
    }

    @Test
    public void testReplacePayees() throws Exception {
        Transaction transaction = createTransaction();
        TransactionDetail detail = new TransactionDetail();
        detail.setAmount(new BigDecimal("987.65"));
        transaction.addDetails(detail);
        transactionDao.save(transaction);
        Payee newPayee = new Payee("replacement payee");
        payeeDao.save(newPayee);

        transactionDao.replacePayee(Arrays.asList(transaction.getPayee()), newPayee);

        clearSession();
        assertThat(transactionDao.get(transaction.getId()).getPayee().getId()).isEqualTo(newPayee.getId());
    }

    private Transaction getTransaction(List<Transaction> transactions, long transactionId) {
        return transactions.stream().filter(transaction -> transaction.getId().equals(transactionId)).findFirst().orElse(null);
    }

    @Test
    public void testFindLatestForPayeeReturnsNullIfNoTransaction() throws Exception {
        assertThat(transactionDao.findLatestForPayee(-1L)).isNull();
    }

    @Test
    public void testFindLatestForPayeeReturnsTransactionWithLatestDate() throws Exception {
        Transaction transaction = transactionDao.save(createTransaction(DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -1)));
        transactionDao.save(createTransaction(DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -2)));

        assertThat(transactionDao.findLatestForPayee(transaction.getPayee().getId()).getId()).isEqualTo(transaction.getId());
    }

    @Test
    public void testFindLatestForPayeeReturnsTransactionWithLatestDateAndId() throws Exception {
        transactionDao.save(createTransaction(DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -1)));
        Transaction transaction = transactionDao.save(createTransaction(DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -1)));

        assertThat(transactionDao.findLatestForPayee(transaction.getPayee().getId()).getId()).isEqualTo(transaction.getId());
    }
}