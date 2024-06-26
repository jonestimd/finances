package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
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
import static org.assertj.core.api.Assertions.*;

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
        assertThat(withoutLotIds).doesNotContain(getIds(saleWithoutLot2.getDetails())).as("wrong security");
        assertThat(withoutLotIds).doesNotContain(getIds(saleWithoutLot3.getDetails())).as("wrong date");
        assertThat(withoutLotIds).contains(getIds(sharesOutWithoutLot.getDetails()));
        assertThat(withoutLotIds).doesNotContain(getIds(buy.getDetails()));
        assertThat(withoutLotIds).doesNotContain(saleWithLot.getDetails().get(0).getId());
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
        assertThat(withoutLotIds).doesNotContain(getIds(buyWithoutLot1.getDetails().subList(2, 4)));
        assertThat(withoutLotIds).contains(getIds(sharesInWithoutLot.getDetails()));
        assertThat(withoutLotIds).doesNotContain(getIds(buyWithWrongAccount.getDetails())).as("purchase from wrong account");
        assertThat(withoutLotIds).contains(getIds(sharesInWithZeroAmount.getDetails())).as("purchase with wrong price");
        assertThat(withoutLotIds).doesNotContain(getIds(buyWithNoShares.getDetails())).as("purchase with no available shares");
        assertThat(withoutLotIds).doesNotContain(getIds(sellWithLot.getDetails())).as("sale");
        assertThat(withoutLotIds).doesNotContain(getIds(sharesOutWithoutLot.getDetails())).as("shares out");
        assertThat(withoutLotIds).contains(getIds(buyWithLot2.getDetails()));
    }

    @Test
    public void testFindPreviousPurchases() throws Exception {
        Date sellDate = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        Account account2 = accountDao.get((Long) ACCOUNT_BATCH.getValue(1, "id"));
        Security security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        Transaction buyOnSaleDate = createTransaction(security, sellDate,
                createTransactionDetail(BUY.code(), "-100.00", "5.0"),
                createTransactionDetail(BUY.code(), "-200.00", "5.0"),
                createTransactionDetail("Dividend", "300.90"),
                createTransactionDetail("Commission", "-0.90"));
        Transaction sharesInOnSaleDate = createTransaction(security, sellDate,
                createTransactionDetail(SHARES_IN.code(), "-300.90", "10.0"),
                createTransactionDetail(SHARES_IN.code(), "-150.45", "5.0"));
        Transaction buyFromAccount2 = createTransaction(account2, security, sellDate,
                createTransactionDetail(BUY.code(), "-300.90", "10.0"),
                createTransactionDetail(BUY.code(), "-300.90", "10.0"));
        Transaction buyAfterSaleDate = createTransaction(account, security, DateUtils.addDays(sellDate, 1),
                createTransactionDetail(SHARES_IN.code(), "300.00", "10.0"));
        Transaction xferInOnSaleDate = createTransaction(security, sellDate,
                TransactionDetail.newTransfer(account2, "0", "10.0"));
        Transaction xferOut = createTransaction(security, sellDate,
                TransactionDetail.newTransfer(account2, "0", "-10.0"));
        Transaction sharesOut = createTransaction(security, sellDate,
                createTransactionDetail(SHARES_OUT.code(), "300.90", "-10.0"),
                createTransactionDetail(SHARES_OUT.code(), "300.90", "-10.0"));
        Transaction sell = createTransaction(security, sellDate,
                createTransactionDetail(SELL.code(), "300.90", "-10.0"),
                createTransactionDetail(SELL.code(), "300.90", "-10.0"));

        TransactionDetail sale = createTransaction(security, sellDate, createTransactionDetail("Sell", "9")).getDetails().get(0);
        List<TransactionDetail> purchases = transactionDetailDao.findPreviousPurchases(sale);

        assertThat(purchases).isNotEmpty();
        assertThat(purchases.size()).isEqualTo(new HashSet<>(purchases).size()).as("no duplicates");
        List<Object> withoutLotIds = Lists.transform(purchases, UniqueId::getId);
        assertThat(withoutLotIds).contains(getIds(buyOnSaleDate.getDetails().subList(0, 2)));
        assertThat(withoutLotIds).contains(getIds(sharesInOnSaleDate.getDetails()));
        assertThat(withoutLotIds).contains(getIds(xferInOnSaleDate.getDetails()));
        assertThat(withoutLotIds).doesNotContain(getIds(buyAfterSaleDate.getDetails()));
        assertThat(withoutLotIds).doesNotContain(getIds(buyFromAccount2.getDetails()));
        assertThat(withoutLotIds).doesNotContain(getIds(xferOut.getDetails()));
        assertThat(withoutLotIds).doesNotContain(getIds(sharesOut.getDetails()));
        assertThat(withoutLotIds).doesNotContain(getIds(sell.getDetails()));
    }

    @Test
    public void findByString() throws Exception {
        List<TransactionDetail> matches = transactionDetailDao.findByString("x");

        assertThat(matches).isEmpty();
    }

    @Test
    public void findByCategoryIdsReturnsEmptyListForEmptyIds() throws Exception {
        assertThat(transactionDetailDao.findByCategoryIds(Collections.emptyList())).isEmpty();
    }

    @Test
    public void findByCategoryIdsRunsQueryForNonemptyIds() throws Exception {
        TransactionCategory category = transactionCategoryDao.save(new TransactionCategory("the category"));
        createTransaction(null, new Date(), new TransactionDetail(category, BigDecimal.ONE, null, null));

        assertThat(transactionDetailDao.findByCategoryIds(Collections.singletonList(category.getId()))).hasSize(1);
    }

    @Test
    public void replaceCategory() throws Exception {
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