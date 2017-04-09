package io.github.jonestimd.finance.dao.hibernate;

import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.dao.TransactionGroupDao;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionGroupSummary;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TransactionGroupDaoImplTest extends HsqlTestFixture {
    private static final QueryBatch[] SETUP_BATCH = {
        TRANSACTION_GROUP_BATCH,
    };

    private TransactionGroupDao transactionGroupDao;

    @Before
    public void setUpDaos() throws Exception {
        this.transactionGroupDao = daoContext.getTransactionGroupDao();
    }

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(SETUP_BATCH);
    }

    @Test
    public void getTransactionGroupMatchesName() throws Exception {
        String name = "Group 2";
        TransactionGroup group = transactionGroupDao.getTransactionGroup(name);

        assertThat(group.getId().intValue()).isEqualTo(1001);
        assertThat(group.getName()).isEqualTo(name);
        assertThat(group.getDescription()).isEqualTo("Group two");
    }

    @Test
    public void getTrannsactionGroupSummaries() throws Exception {
        List<TransactionGroupSummary> groups = transactionGroupDao.getTransactionGroupSummaries();

        assertThat(groups.isEmpty()).isFalse();
        assertThat(groups.get(0).getClass()).isEqualTo(TransactionGroupSummary.class);
    }
}