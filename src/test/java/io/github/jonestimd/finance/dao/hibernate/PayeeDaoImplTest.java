package io.github.jonestimd.finance.dao.hibernate;

import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PayeeDaoImplTest extends HsqlTestFixture {
    private PayeeDao payeeDao;

    @Before
    public void setUpDao() {
        payeeDao = daoContext.getPayeeDao();
    }

    protected List<QueryBatch> getInsertQueries() {
        return Collections.singletonList(PAYEE_BATCH);
    }

    @Test
    public void getPayeeMatchesName() throws Exception {
        Payee result = payeeDao.getPayee("Payee 1");

        assertEquals(1000, (long)result.getId());
        assertEquals("Payee 1", result.getName());
    }

    @Test
    public void getPayeeSummaries() throws Exception {
        List<PayeeSummary> payees = payeeDao.getPayeeSummaries();

        assertFalse(payees.isEmpty());
        assertEquals(PayeeSummary.class, payees.get(0).getClass());
    }
}