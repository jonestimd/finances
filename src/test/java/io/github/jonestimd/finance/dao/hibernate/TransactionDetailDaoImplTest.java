package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.SecurityLotDao;
import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.dao.TransactionDetailDao;
import io.github.jonestimd.finance.dao.TransactionalTestFixture;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.util.Streams;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static io.github.jonestimd.finance.domain.transaction.SecurityAction.*;
import static org.fest.assertions.Assertions.*;

public class TransactionDetailDaoImplTest extends TransactionalTestFixture {
    private static final QueryBatch[] SETUP_BATCH = {
        ACCOUNT_BATCH,
        TRANSACTION_CATEGORY_BATCH,
        PAYEE_BATCH,
        SECURITY_BATCH, };

    private TransactionDao transactionDao;
    private TransactionDetailDao transactionDetailDao;
    private AccountDao accountDao;
    private TransactionCategoryDao transactionCategoryDao;
    private SecurityDao securityDao;
    private SecurityLotDao securityLotDao;
    private Account account;

    @Override
    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(SETUP_BATCH);
    }

    @Before
    public void setUpDaos() throws Exception {
        transactionDao = daoContext.getTransactionDao();
        transactionDetailDao = daoContext.getTransactionDetailDao();
        accountDao = daoContext.getAccountDao();
        transactionCategoryDao = daoContext.getTransactionCategoryDao();
        securityDao = daoContext.getSecurityDao();
        securityLotDao = daoContext.getSecurityLotDao();
        account = accountDao.get((Long) ACCOUNT_BATCH.getValue(0, "id"));
    }

    private Transaction createTransfer(double amount) {
        Account account1 = accountDao.get((Long) ACCOUNT_BATCH.getValue(0, "id"));
        Account account2 = accountDao.get((Long) ACCOUNT_BATCH.getValue(2, "id"));
        TransactionDetail detail1 = new TransactionDetail(new BigDecimal(amount), null, null);
        Date date = new Date();
        Transaction transaction1 = new Transaction(account1, date, null, true, null, detail1);
        new Transaction(account2, date, null, true, null, detail1.getRelatedDetail());
        return transaction1;
    }

    @Test
    public void testFindOrphanTransfers() throws Exception {
        Transaction transaction1 = transactionDao.save(createTransfer(15d));
        transaction1.getDetails().get(0).getRelatedDetail().setRelatedDetail(null);
        transactionDao.save(createTransfer(30d));

        List<TransactionDetail> result = transactionDetailDao.findOrphanTransfers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(transaction1.getDetails().get(0).getId());
    }

    @Test
    public void testFindSecuritySalesWithoutLots() throws Exception {
        Date sellDate = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        Security security1 = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        Security security2 = securityDao.get((Long) SECURITY_BATCH.getValue(1, "asset_id"));
        Transaction saleWithoutLot1 = createTransaction(security1, sellDate,
                createTransactionDetail(SELL.code(), "123.45", "-10.0002"),
                createTransactionDetail(SELL.code(), "234.45", "-10.0002"));
        Transaction saleWithoutLot2 = createTransaction(security2, sellDate,
                createTransactionDetail(SELL.code(), "123.45", "-10.000"),
                createTransactionDetail(SELL.code(), "234.45", "-10.000"));
        Transaction saleWithoutLot3 = createTransaction(security1, DateUtils.addDays(sellDate, 1),
                createTransactionDetail(SELL.code(), "123.45", "-10.000"),
                createTransactionDetail(SELL.code(), "234.45", "-10.000"));
        Transaction sharesOutWithoutLot = createTransaction(security1, sellDate,
                createTransactionDetail(SHARES_OUT.code(), "123.45", "-10.0001"),
                createTransactionDetail(SHARES_OUT.code(), "234.45", "-10.0001"));
        Transaction buy = createTransaction(security1, new Date(),
                createTransactionDetail(BUY.code(), "-123.45", "10.000"),
                createTransactionDetail(BUY.code(), "-234.45", "10.000"));
        Transaction saleWithLot = createTransaction(security1, sellDate,
                createTransactionDetail(SELL.code(), "123.45", "-10.000"),
                createTransactionDetail(SELL.code(), "234.45", "-10.000"));
        securityLotDao.save(new SecurityLot(buy.getDetails().get(0), saleWithLot.getDetails().get(0), BigDecimal.TEN));

        List<TransactionDetail> withoutLots = transactionDetailDao.findSecuritySalesWithoutLots(security1.getName().substring(0, 7).toUpperCase(), sellDate);

        assertThat(withoutLots).isNotEmpty();
        assertThat(withoutLots.size()).isEqualTo(new HashSet<>(withoutLots).size()).as("no duplicates");
        List<Object> withoutLotIds = Lists.transform(withoutLots, UniqueId::getId);
        assertThat(withoutLotIds).contains(getIds(saleWithoutLot1.getDetails()));
        assertThat(withoutLotIds).excludes(getIds(saleWithoutLot2.getDetails())).as("wrong security");
        assertThat(withoutLotIds).excludes(getIds(saleWithoutLot3.getDetails())).as("wrong date");
        assertThat(withoutLotIds).contains(getIds(sharesOutWithoutLot.getDetails()));
        assertThat(withoutLotIds).excludes(getIds(buy.getDetails()));
        assertThat(withoutLotIds).excludes(saleWithLot.getDetails().get(0).getId());
        assertThat(withoutLotIds).contains(saleWithLot.getDetails().get(1).getId());
    }

    @Test
    public void testFindPurchasesWithRemainingShares() throws Exception {
        Date buyDate = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        Account account2 = accountDao.get((Long) ACCOUNT_BATCH.getValue(1, "id"));
        Security security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        Transaction buyWithoutLot1 = createTransaction(security, buyDate,
                createTransactionDetail(BUY.code(), "-100.00", "5.0"),
                createTransactionDetail(BUY.code(), "-200.00", "5.0"),
                createTransactionDetail("Dividend", "300.90"),
                createTransactionDetail("Commission", "-0.90"));
        Transaction sharesInWithoutLot = createTransaction(security, buyDate,
                createTransactionDetail(SHARES_IN.code(), "-300.90", "10.0"),
                createTransactionDetail(SHARES_IN.code(), "-150.45", "5.0"));
        Transaction buyWithWrongAccount = createTransaction(account2, security, buyDate,
                createTransactionDetail(BUY.code(), "-300.90", "10.0"),
                createTransactionDetail(BUY.code(), "-300.90", "10.0"));
        Transaction sharesInWithZeroAmount = createTransaction(account, security, buyDate,
                createTransactionDetail(SHARES_IN.code(), "0.00", "10.0"));
        Transaction buyWithNoShares = createTransaction(security, buyDate,
                createTransactionDetail(BUY.code(), "-300.90", "10.0"),
                createTransactionDetail(BUY.code(), "-300.90", "10.0"));
        Transaction sharesOutWithoutLot = createTransaction(security, new Date(),
                createTransactionDetail(SHARES_OUT.code(), "300.90", "-10.0"),
                createTransactionDetail(SHARES_OUT.code(), "300.90", "-10.0"));
        Transaction sellWithLot = createTransaction(security, new Date(),
                createTransactionDetail(SELL.code(), "300.90", "-10.0"),
                createTransactionDetail(SELL.code(), "300.90", "-10.0"));
        securityLotDao.save(new SecurityLot(buyWithNoShares.getDetails().get(0), sellWithLot.getDetails().get(1), BigDecimal.TEN));
        securityLotDao.save(new SecurityLot(buyWithNoShares.getDetails().get(1), sellWithLot.getDetails().get(0), BigDecimal.TEN));
        Transaction buyWithLot2 = createTransaction(security, buyDate,
                createTransactionDetail(BUY.code(), "-400.90", "10.0"),
                createTransactionDetail(BUY.code(), "-200.90", "10.0"));
        Transaction sellWithLot2 = createTransaction(security, new Date(),
                createTransactionDetail(SELL.code(), "300.90", "-10.0"),
                createTransactionDetail(SELL.code(), "300.90", "-10.0"));
        securityLotDao.save(new SecurityLot(buyWithLot2.getDetails().get(0), sellWithLot2.getDetails().get(1), new BigDecimal("9")));

        List<TransactionDetail> withoutLots = transactionDetailDao.findPurchasesWithRemainingShares(account, security, buyDate);

        assertThat(withoutLots).isNotEmpty();
        assertThat(withoutLots.size()).isEqualTo(new HashSet<>(withoutLots).size()).as("no duplicates");
        List<Object> withoutLotIds = Lists.transform(withoutLots, UniqueId::getId);
        assertThat(withoutLotIds).contains(getIds(buyWithoutLot1.getDetails().subList(0, 2)));
        assertThat(withoutLotIds).excludes(getIds(buyWithoutLot1.getDetails().subList(2, 4)));
        assertThat(withoutLotIds).contains(getIds(sharesInWithoutLot.getDetails()));
        assertThat(withoutLotIds).excludes(getIds(buyWithWrongAccount.getDetails())).as("purchase from wrong account");
        assertThat(withoutLotIds).contains(getIds(sharesInWithZeroAmount.getDetails())).as("purchase with wrong price");
        assertThat(withoutLotIds).excludes(getIds(buyWithNoShares.getDetails())).as("purchase with no available shares");
        assertThat(withoutLotIds).excludes(getIds(sellWithLot.getDetails())).as("sale");
        assertThat(withoutLotIds).excludes(getIds(sharesOutWithoutLot.getDetails())).as("shares out");
        assertThat(withoutLotIds).contains(getIds(buyWithLot2.getDetails()));
    }

    @Test
    public void testFindAvailablePurchaseShares() throws Exception {
        Date sellDate = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        Account account2 = accountDao.get((Long) ACCOUNT_BATCH.getValue(1, "id"));
        Security security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        Transaction buyWithoutLot1 = createTransaction(security, sellDate,
                createTransactionDetail(BUY.code(), "-100.00", "5.0"),
                createTransactionDetail(BUY.code(), "-200.00", "5.0"),
                createTransactionDetail("Dividend", "300.90"),
                createTransactionDetail("Commission", "-0.90"));
        Transaction sharesInWithoutLot = createTransaction(security, sellDate,
                createTransactionDetail(SHARES_IN.code(), "-300.90", "10.0"),
                createTransactionDetail(SHARES_IN.code(), "-150.45", "5.0"));
        Transaction buyWithWrongAccount = createTransaction(account2, security, sellDate,
                createTransactionDetail(BUY.code(), "-300.90", "10.0"),
                createTransactionDetail(BUY.code(), "-300.90", "10.0"));
        Transaction sharesInWithZeroAmount = createTransaction(account, security, sellDate,
                createTransactionDetail(SHARES_IN.code(), "0.00", "10.0"));
        Transaction buyWithNoShares = createTransaction(security, sellDate,
                createTransactionDetail(BUY.code(), "-300.90", "10.0"),
                createTransactionDetail(BUY.code(), "-300.90", "10.0"));
        Transaction sharesOutWithoutLot = createTransaction(security, sellDate,
                createTransactionDetail(SHARES_OUT.code(), "300.90", "-10.0"),
                createTransactionDetail(SHARES_OUT.code(), "300.90", "-10.0"));
        Transaction sellWithLot = createTransaction(security, sellDate,
                createTransactionDetail(SELL.code(), "300.90", "-10.0"),
                createTransactionDetail(SELL.code(), "300.90", "-10.0"));
        securityLotDao.save(new SecurityLot(buyWithNoShares.getDetails().get(0), sellWithLot.getDetails().get(1), BigDecimal.TEN));
        securityLotDao.save(new SecurityLot(buyWithNoShares.getDetails().get(1), sellWithLot.getDetails().get(0), BigDecimal.TEN));
        Transaction buyWithLot2 = createTransaction(security, DateUtils.addDays(sellDate, 1),
                createTransactionDetail(BUY.code(), "-400.90", "10.0"),
                createTransactionDetail(BUY.code(), "-200.90", "10.0"));
        Transaction sellWithLot2 = createTransaction(security, sellDate,
                createTransactionDetail(SELL.code(), "300.90", "-10.0"),
                createTransactionDetail(SELL.code(), "300.90", "-10.0"));
        securityLotDao.save(new SecurityLot(buyWithLot2.getDetails().get(0), sellWithLot2.getDetails().get(1), new BigDecimal("9")));

        TransactionDetail sale = createTransaction(security, sellDate, createTransactionDetail("Sell", "9")).getDetails().get(0);
        List<TransactionDetail> withoutLots = transactionDetailDao.findAvailablePurchaseShares(sale);

        assertThat(withoutLots).isNotEmpty();
        assertThat(withoutLots.size()).isEqualTo(new HashSet<>(withoutLots).size()).as("no duplicates");
        List<Object> withoutLotIds = Lists.transform(withoutLots, UniqueId::getId);
        assertThat(withoutLotIds).contains(getIds(buyWithoutLot1.getDetails().subList(0, 2)));
        assertThat(withoutLotIds).excludes(getIds(buyWithoutLot1.getDetails().subList(2, 4)));
        assertThat(withoutLotIds).contains(getIds(sharesInWithoutLot.getDetails()));
        assertThat(withoutLotIds).excludes(getIds(buyWithWrongAccount.getDetails())).as("purchase from wrong account");
        assertThat(withoutLotIds).contains(getIds(sharesInWithZeroAmount.getDetails())).as("purchase with wrong price");
        assertThat(withoutLotIds).excludes(getIds(buyWithNoShares.getDetails())).as("purchase with no available shares");
        assertThat(withoutLotIds).excludes(getIds(sellWithLot.getDetails())).as("sale");
        assertThat(withoutLotIds).excludes(getIds(sharesOutWithoutLot.getDetails())).as("shares out");
        assertThat(withoutLotIds).excludes(getIds(buyWithLot2.getDetails())).as("after sell date");
    }

    @Test
    public void mergeCategories() throws Exception {
        TransactionCategory oldCategory = transactionCategoryDao.save(new TransactionCategory("old category"));
        TransactionCategory newCategory = transactionCategoryDao.save(new TransactionCategory("replacement category"));
        Transaction transaction = createTransaction(null, new Date(), new TransactionDetail(oldCategory, BigDecimal.ONE, null, null));
        transactionDao.save(transaction);

        transactionDetailDao.replaceCategory(Streams.map(transaction.getDetails(), TransactionDetail::getCategory), newCategory);

        clearSession();
        assertThat(transactionDao.get(transaction.getId()).getDetails().get(0).getCategory().getId()).isEqualTo(newCategory.getId());
    }

    private Object[] getIds(List<TransactionDetail> details) {
        return Lists.transform(details, UniqueId::getId).toArray();
    }

    private Transaction createTransaction(Security security, Date date, TransactionDetail... details) {
        return createTransaction(account, security, date, details);
    }

    private Transaction createTransaction(Account txAccount, Security security, Date date, TransactionDetail... details) {
        Transaction transaction = new Transaction(txAccount, date, null, true, null, details);
        transaction.setSecurity(security);
        return transactionDao.save(transaction);
    }

    private TransactionDetail createTransactionDetail(String type, String amount) {
        return new TransactionDetail(transactionCategoryDao.getSecurityAction(type), new BigDecimal(amount), null, null);
    }

    private TransactionDetail createTransactionDetail(String type, String amount, String shares) {
        TransactionDetail detail = createTransactionDetail(type, amount);
        detail.setAssetQuantity(new BigDecimal(shares));
        return detail;
    }
}