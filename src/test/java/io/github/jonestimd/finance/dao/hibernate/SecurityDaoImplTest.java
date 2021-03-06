package io.github.jonestimd.finance.dao.hibernate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.dao.TransactionalTestFixture;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class SecurityDaoImplTest extends TransactionalTestFixture {
    private SecurityDao securityDao;
    private AccountDao accountDao;
    private TransactionDao transactionDao;

    @Before
    public void setUpDao() {
        securityDao = daoContext.getSecurityDao();
        accountDao = daoContext.getAccountDao();
        transactionDao = daoContext.getTransactionDao();
    }

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(ACCOUNT_BATCH, SECURITY_BATCH);
    }

    private Predicate<SecuritySummary> hasId(Long securityId) {
        return s -> s.getSecurity().getId().equals(securityId);
    }

    @Test
    public void getSecurityMatchesSymbol() throws Exception {
        Security result = securityDao.getSecurity("S1");

        assertThat(SECURITY_BATCH.getValue(0, "asset_id")).isEqualTo(result.getId());
        assertThat("S1").isEqualTo(result.getSymbol());
    }

    @Test
    public void findByNameMatchesName() throws Exception {
        Security result = securityDao.findByName("Stock 111");

        assertThat(SECURITY_BATCH.getValue(0, "asset_id")).isEqualTo(result.getId());
        assertThat("Stock 111").isEqualTo(result.getName());
    }

    @Test
    public void getSecuritySummaries() throws Exception {
        Transaction buy = createSecurityTransaction(BigDecimal.TEN, "-123.45");
        Transaction dividend = createSecurityTransaction(null, "150.00");

        List<SecuritySummary> securities = securityDao.getSecuritySummaries();

        assertThat(securities.size()).isGreaterThanOrEqualTo(2);
        SecuritySummary buySummary = securities.stream().filter(hasId(buy.getSecurity().getId())).findFirst().get();
        assertThat(buySummary.getShares()).isEqualByComparingTo(buy.getAssetQuantity());
        assertThat(buySummary.getCostBasis()).isEqualByComparingTo(buy.getAmount().negate());
        assertThat(buySummary.getDividends()).isEqualByComparingTo(dividend.getAmount());
        assertThat(buySummary.getFirstAcquired().getTime()).isEqualTo(DateUtils.truncate(buy.getDate(), Calendar.DAY_OF_MONTH).getTime());
        assertThat(buySummary.getTransactionCount()).isEqualTo(2);
        for (SecuritySummary security : securities) {
            if (! security.getSecurity().getId().equals(buy.getSecurity().getId())) {
                assertThat(security.getShares()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(security.getFirstAcquired()).isNull();
            }
        }
    }

    @Test
    public void getSecuritySummariesIncludesSoldSecurities() throws Exception {
        Transaction buy = createSecurityTransaction(BigDecimal.TEN, "-123.45");
        Transaction dividend = createSecurityTransaction(null, "150.00");
        createSecurityTransaction(new BigDecimal("-10"), "456.78");

        List<SecuritySummary> securities = securityDao.getSecuritySummaries();

        assertThat(securities.size()).isGreaterThanOrEqualTo(2);
        SecuritySummary buySummary = securities.stream().filter(hasId(buy.getSecurity().getId())).findFirst().get();
        assertThat(buySummary.getShares()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(buySummary.getCostBasis()).isEqualByComparingTo(buy.getAmount().negate());
        assertThat(buySummary.getDividends()).isEqualByComparingTo(dividend.getAmount());
        assertThat(buySummary.getFirstAcquired().getTime()).isEqualTo(DateUtils.truncate(buy.getDate(), Calendar.DAY_OF_MONTH).getTime());
        assertThat(buySummary.getTransactionCount()).isEqualTo(3);
        for (SecuritySummary security : securities) {
            if (! security.getSecurity().getId().equals(buy.getSecurity().getId())) {
                assertThat(security.getShares()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(security.getFirstAcquired()).isNull();
            }
        }
    }

    @Test
    public void getSecuritySummariesForAccount() throws Exception {
        Transaction buy = createSecurityTransaction(BigDecimal.TEN, "-123.45");

        List<SecuritySummary> securities = securityDao.getSecuritySummaries(buy.getAccount().getId());

        assertThat(securities).isNotEmpty();
        assertThat(securities.get(0).getSecurity()).isEqualTo(buy.getSecurity());
        assertThat(securities.get(0).getShares().compareTo(buy.getAssetQuantity())).isEqualTo(0);
    }

    @Test
    public void getSecuritySummariesByAccount() throws Exception {
        Transaction buy = createSecurityTransaction(BigDecimal.TEN, "-123.45");
        buy.addDetails(new TransactionDetail(null, BigDecimal.ONE.negate(), null, null));
        Transaction dividend = createSecurityTransaction(null, "150.00");

        List<SecuritySummary> securities = securityDao.getSecuritySummariesByAccount();

        assertThat(securities).hasSize(1);
        assertThat(securities.get(0).getAccount()).isEqualTo(buy.getAccount());
        assertThat(securities.get(0).getSecurity()).isEqualTo(buy.getSecurity());
        assertThat(securities.get(0).getShares()).isEqualByComparingTo(buy.getAssetQuantity());
        assertThat(securities.get(0).getCostBasis()).isEqualByComparingTo(buy.getDetails().get(0).getAmount().negate());
        assertThat(securities.get(0).getDividends()).isEqualTo(dividend.getAmount());
        assertThat(securities.get(0).getFirstAcquired().getTime()).isEqualTo(DateUtils.truncate(buy.getDate(), Calendar.DAY_OF_MONTH).getTime());
        assertThat(securities.get(0).getTransactionCount()).isEqualTo(2);
    }

    @Test
    public void getSecuritySummaryByAccount() throws Exception {
        Transaction buy = createSecurityTransaction(BigDecimal.TEN, "-123.45");
        buy.addDetails(new TransactionDetail(null, BigDecimal.ONE.negate(), null, null));
        Transaction dividend = createSecurityTransaction(null, "150.00");

        List<SecuritySummary> securities = securityDao.getSecuritySummaryByAccount(buy.getSecurity().getId());

        assertThat(securities).hasSize(1);
        assertThat(securities.get(0).getAccount()).isEqualTo(buy.getAccount());
        assertThat(securities.get(0).getSecurity()).isEqualTo(buy.getSecurity());
        assertThat(securities.get(0).getShares()).isEqualByComparingTo(buy.getAssetQuantity());
        assertThat(securities.get(0).getCostBasis()).isEqualByComparingTo(buy.getDetails().get(0).getAmount().negate());
        assertThat(securities.get(0).getDividends()).isEqualTo(dividend.getAmount());
        assertThat(securities.get(0).getFirstAcquired().getTime()).isEqualTo(DateUtils.truncate(buy.getDate(), Calendar.DAY_OF_MONTH).getTime());
        assertThat(securities.get(0).getTransactionCount()).isEqualTo(2);
    }

    private Transaction createSecurityTransaction(BigDecimal shares, String amount) {
        Account account = accountDao.get((Long) ACCOUNT_BATCH.getValue(0, "id"));
        Security security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        TransactionDetail detail = new TransactionDetail(null, new BigDecimal(amount), null, null);
        detail.setAssetQuantity(shares);
        return transactionDao.save(new TransactionBuilder().date(new Date()).account(account).security(security).details(detail).get());
    }
}