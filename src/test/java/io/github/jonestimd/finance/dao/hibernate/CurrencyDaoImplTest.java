package io.github.jonestimd.finance.dao.hibernate;

import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.dao.CurrencyDao;
import io.github.jonestimd.finance.dao.HsqlTestFixture;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CurrencyDaoImplTest extends HsqlTestFixture {
    private CurrencyDao currencyDao;

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(ASSET_BATCH);
    }

    @Before
    public void setUpDao() {
        currencyDao = daoContext.getCurrencyDao();
    }

    @Test
    public void getCurrencyByCode() throws Exception {
        assertEquals("USD", currencyDao.getCurrency("USD").getName());
        assertNull(currencyDao.getCurrency("xxx"));
    }
}