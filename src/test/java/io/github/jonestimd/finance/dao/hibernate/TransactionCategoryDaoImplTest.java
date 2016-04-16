package io.github.jonestimd.finance.dao.hibernate;

import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.util.Streams;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class TransactionCategoryDaoImplTest extends HsqlTestFixture {
    private static final QueryBatch[] SETUP_BATCH = {
        TRANSACTION_CATEGORY_BATCH, ACCOUNT_BATCH,
    };

    private TransactionCategoryDao transactionCategoryDao;

    @Before
    public void setUpDaos() throws Exception {
        this.transactionCategoryDao = daoContext.getTransactionCategoryDao();
    }

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(SETUP_BATCH);
    }

    @Test
    public void getTransactionCategoryMatchesParent() throws Exception {
        String grandparentCode = "Salary";
        String parentCode = "Spouse";
        String code = "Job1";
        TransactionCategory grandparent = transactionCategoryDao.getTransactionCategory(grandparentCode);
        TransactionCategory parent = transactionCategoryDao.getTransactionCategory(grandparentCode, parentCode);
        TransactionCategory type = transactionCategoryDao.getTransactionCategory(grandparentCode, parentCode, code);

        assertThat(type.isSecurity()).isFalse();
        assertThat(grandparent.getParent()).isNull();
        assertThat(grandparent.getId()).isEqualTo(1000L);
        assertThat(grandparent.getCode()).isEqualTo(grandparentCode);
        assertThat(parent.getParent().getId()).isEqualTo(grandparent.getId());
        assertThat(parent.getId()).isEqualTo(1001L);
        assertThat(parent.getCode()).isEqualTo(parentCode);
        assertThat(type.getId()).isEqualTo(1002L);
        assertThat(type.getCode()).isEqualTo(code);
    }

    @Test
    public void getSecurityActionMatchesCode() throws Exception {
        TransactionCategory securityAction = transactionCategoryDao.getSecurityAction("Buy");

        assertThat(securityAction.getId()).isEqualTo(1006L);
        assertThat(securityAction.getCode()).isEqualTo("Buy");
        assertThat(securityAction.getParent()).isNull();
        assertThat(securityAction.isSecurity()).isTrue();
    }

    @Test
    public void getAllReturnsAllTransactionCategorys() throws Exception {
        List<TransactionCategory> types = transactionCategoryDao.getAll();

        assertThat(types.size()).isEqualTo(transactionCategoryDao.getAll().size());
    }

    @Test
    public void getTrannsactionCategorySummaries() throws Exception {
        List<TransactionCategorySummary> categories = transactionCategoryDao.getTransactionCategorySummaries();

        assertThat(categories).isNotEmpty();
        assertThat(categories.get(0).getClass()).isEqualTo(TransactionCategorySummary.class);
    }

    @Test
    public void getParentCategories() throws Exception {
        List<TransactionCategory> categories = transactionCategoryDao.getParentCategories();

        assertThat(categories).isNotEmpty();
        assertThat(Streams.map(categories, UniqueId::getId)).contains(1000L, 1001L);
    }
}