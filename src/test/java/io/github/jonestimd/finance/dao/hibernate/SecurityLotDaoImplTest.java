package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.SecurityLotDao;
import io.github.jonestimd.finance.dao.TransactionDetailDao;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

public class SecurityLotDaoImplTest extends HsqlTestFixture {
    private static final QueryBatch[] SETUP_BATCH = {
        ACCOUNT_BATCH,
        TRANSACTION_CATEGORY_BATCH,
        SECURITY_BATCH,
    };

    private SecurityLotDao securityLotDao;
    private TransactionDetailDao transactionDetailDao;
    private AccountDao accountDao;
    private SecurityDao securityDao;
    private Account account;
    private Security security;

    @Before
    public void setUpDao() {
        this.securityLotDao = daoContext.getSecurityLotDao();
        this.transactionDetailDao = daoContext.getTransactionDetailDao();
        this.accountDao = daoContext.getAccountDao();
        this.securityDao = daoContext.getSecurityDao();
        account = accountDao.get((Long) ACCOUNT_BATCH.getValue(0, "id"));
        security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
    }

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(SETUP_BATCH);
    }

    @Test
    public void findLotsForPurchases() throws Exception {
        TransactionDetail sale = createTransaction(new Date());
        List<TransactionDetail> purchases = Arrays.asList(
                createTransaction(new Date()), createTransaction(new Date()), createTransaction(new Date()));
        List<SecurityLot> lots = Arrays.asList(
                securityLotDao.save(new SecurityLot(purchases.get(0), sale, BigDecimal.TEN)),
                securityLotDao.save(new SecurityLot(purchases.get(1), sale, BigDecimal.TEN)),
                securityLotDao.save(new SecurityLot(purchases.get(2), sale, BigDecimal.TEN)));

        List<SecurityLot> forPurchases = securityLotDao.findLotsForPurchases(purchases.subList(0, 2));

        assertThat(forPurchases).hasSize(2);
        assertThat(findLot(forPurchases, lots.get(0).getId())).isNotNull();
        assertThat(findLot(forPurchases, lots.get(1).getId())).isNotNull();
        assertThat(findLot(forPurchases, lots.get(2).getId())).isNull();
    }

    private SecurityLot findLot(List<SecurityLot> lots, long id) {
        for (SecurityLot lot : lots) {
            if (lot.getId().longValue() == id) {
                return lot;
            }
        }
        return null;
    }

    private TransactionDetail createTransaction(Date date) {
        TransactionDetail detail = new TransactionDetail();
        detail.setAmount(BigDecimal.TEN);
        detail.setTransaction(new TransactionBuilder().account(account).date(date).security(security).cleared(true).get());
        return transactionDetailDao.save(detail);
    }

    @Test
    public void findBySale() throws Exception {
        TransactionDetail sale = createTransaction(new Date());
        TransactionDetail purchase = createTransaction(new Date());
        SecurityLot lot = securityLotDao.save(new SecurityLot(purchase, sale, BigDecimal.TEN));

        List<SecurityLot> purchaseLots = securityLotDao.findBySale(sale);

        assertThat(Lists.transform(purchaseLots, UniqueId::getId)).containsOnly(lot.getId());
    }

    @Test
    public void deleteSaleLots() throws Exception {
        TransactionDetail sale = createTransaction(new Date());
        TransactionDetail purchase = createTransaction(new Date());
        SecurityLot lot = securityLotDao.save(new SecurityLot(purchase, sale, BigDecimal.TEN));

        securityLotDao.deleteSaleLots(sale);

        assertThat(securityLotDao.get(lot.getId())).isNull();
    }
}