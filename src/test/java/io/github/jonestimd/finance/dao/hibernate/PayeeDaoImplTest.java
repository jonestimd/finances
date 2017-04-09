package io.github.jonestimd.finance.dao.hibernate;

import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

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

        assertThat((long)result.getId()).isEqualTo(1000);
        assertThat(result.getName()).isEqualTo("Payee 1");
    }

    @Test
    public void getPayeeSummaries() throws Exception {
        List<PayeeSummary> payees = payeeDao.getPayeeSummaries();

        assertThat(payees.isEmpty()).isFalse();
        assertThat(payees.get(0).getClass()).isEqualTo(PayeeSummary.class);
    }
}