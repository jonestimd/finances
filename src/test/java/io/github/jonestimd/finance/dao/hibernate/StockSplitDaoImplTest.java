package io.github.jonestimd.finance.dao.hibernate;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.StockSplitDao;
import io.github.jonestimd.finance.dao.TransactionalTestFixture;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SplitRatio;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class StockSplitDaoImplTest extends TransactionalTestFixture {
    private static final QueryBatch[] SETUP_BATCH = {SECURITY_BATCH};

    private SecurityDao securityDao;
    private StockSplitDao stockSplitDao;

    @Before
    public void setUpDaos() throws Exception {
        this.securityDao = daoContext.getSecurityDao();
        this.stockSplitDao = daoContext.getStockSplitDao();
    }

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(SETUP_BATCH);
    }

    @Test
    public void findBySecurityAndDate() throws Exception {
        Security security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        Date date = new Date();
        stockSplitDao.save(createStockSplit(security, DateUtils.addDays(date, 1), 3));
        stockSplitDao.save(createStockSplit(security, DateUtils.addDays(date, -1), 4));
        StockSplit stockSplit = stockSplitDao.save(createStockSplit(security, date, 2));

        assertThat(stockSplitDao.find(security, date)).isEqualTo(stockSplit);
    }

    @Test
    public void uniqueKeyOnDateAndSecurity() throws Exception {
        Security security = securityDao.get((Long) SECURITY_BATCH.getValue(0, "asset_id"));
        Date splitDate = new Date();
        stockSplitDao.save(createStockSplit(security, splitDate, 2));
        try {
            stockSplitDao.save(createStockSplit(security, splitDate, 2));
            fail("expected exception");
        } catch (UndeclaredThrowableException ex) {
            assertThat(ExceptionUtils.getRootCause(ex).getMessage().toLowerCase()).startsWith("integrity constraint violation");
        }
    }

    private StockSplit createStockSplit(Security security, Date date, int ratio) {
        StockSplit split1 = new StockSplit();
        split1.setDate(date);
        split1.setSecurity(security);
        split1.setSplitRatio(new SplitRatio(BigDecimal.ONE, new BigDecimal(ratio)));
        return split1;
    }
}