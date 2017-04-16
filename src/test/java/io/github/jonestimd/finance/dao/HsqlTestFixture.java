package io.github.jonestimd.finance.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.github.jonestimd.finance.dao.hibernate.DaoContextSupplier;
import io.github.jonestimd.finance.dao.hibernate.TestHibernateDaoContext;
import io.github.jonestimd.finance.domain.account.AccountType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Before;

import static io.github.jonestimd.finance.domain.transaction.SecurityAction.*;

public abstract class HsqlTestFixture {
    private static final String TEST_USERID = "junit";
    private static final Timestamp TEST_DATE = new Timestamp(System.currentTimeMillis());

    private static final String[] COMPANY_COLUMNS = { "id", "name", "change_date", "change_user", "version", };
    private static final Object[][] COMPANY_VALUES = {
            { 1000L, "Company1", TEST_DATE, TEST_USERID, 0, },
            { 1001L, "Company2", TEST_DATE, TEST_USERID, 0, },
    };
    protected static final QueryBatch COMPANY_BATCH = new QueryBatch("company", COMPANY_COLUMNS, COMPANY_VALUES);

    private static final String[] TRANSACTION_CATEGORY_COLUMNS = { "id", "income", "security", "amount_type", "code",
            "parent_id", "description", "change_date", "change_user", "version", };
    private static final Object[][] TRANSACTION_CATEGORY_VALUES = {
            { 1000L, "Y", "N", "DEBIT_DEPOSIT", "Salary", null, "My salary", TEST_DATE, TEST_USERID, 0, },
            { 1001L, "Y", "N", "DEBIT_DEPOSIT", "Spouse", 1000L, "Spouse's salary", TEST_DATE, TEST_USERID, 0, },
            { 1002L, "Y", "N", "DEBIT_DEPOSIT", "Job1", 1001L, "Spouse's 1st salary", TEST_DATE, TEST_USERID, 0, },
            { 1003L, "Y", "N", "DEBIT_DEPOSIT", "Job2", 1001L, "Spouse's 2nd salary", TEST_DATE, TEST_USERID, 0, },
            { 1004L, "Y", "N", "DEBIT_DEPOSIT", "Job1", 1000L, "My 1st salary", TEST_DATE, TEST_USERID, 0, },
            { 1005L, "Y", "N", "DEBIT_DEPOSIT", "Job2", 1000L, "My 2nd salary", TEST_DATE, TEST_USERID, 0, },
            { 1006L, "N", "Y", "DEBIT_DEPOSIT", BUY.code(), null, null, TEST_DATE, TEST_USERID, 0, },
            { 1007L, "N", "N", "DEBIT_DEPOSIT", BUY.code(), null, null, TEST_DATE, TEST_USERID, 0, },
            { 1008L, "Y", "Y", "DEBIT_DEPOSIT", SELL.code(), null, null, TEST_DATE, TEST_USERID, 0, },
            { 1009L, "N", "Y", "ASSET_VALUE", SHARES_IN.code(), null, null, TEST_DATE, TEST_USERID, 0, },
            { 1010L, "Y", "Y", "ASSET_VALUE", SHARES_OUT.code(), null, null, TEST_DATE, TEST_USERID, 0, },
            { 1011L, "N", "Y", "DEBIT_DEPOSIT", COMMISSION_AND_FEES.code(), null, null, TEST_DATE, TEST_USERID, 0, },
            { 1012L, "Y", "Y", "DEBIT_DEPOSIT", DIVIDEND.code(), null, null, TEST_DATE, TEST_USERID, 0, },
    };
    protected static final QueryBatch TRANSACTION_CATEGORY_BATCH = new QueryBatch("tx_category", TRANSACTION_CATEGORY_COLUMNS, TRANSACTION_CATEGORY_VALUES);

    private static final String[] ASSET_COLUMNS = { "id", "type", "scale", "name", "symbol", "change_date", "change_user", "version", };
    private static final Object[][] ASSET_VALUES = {
            { 1000L, "Currency", 2, "USD", "$", TEST_DATE, TEST_USERID, 0 },
            { 1001L, "Security", 6, "Stock 111", "S1", TEST_DATE, TEST_USERID, 0, },
            { 1002L, "Security", 6, "Stock 222", "S2", TEST_DATE, TEST_USERID, 0, },
    };

    protected static final QueryBatch ASSET_BATCH = new QueryBatch("asset", ASSET_COLUMNS, ASSET_VALUES);

    private static final String[] SECURITY_COLUMNS = { "asset_id", "type", };
    private static final Object[][] SECURITY_VALUES = {
            { 1001L, "Stock", },
            { 1002L, "Stock", },
    };
    protected static final QueryBatch SECURITY_BATCH = new QueryBatch("security", SECURITY_COLUMNS, SECURITY_VALUES, ASSET_BATCH);

    private static final String[] ACCOUNT_COLUMNS = { "id", "currency_id", "company_id", "name", "type", "closed",
            "description", "change_date", "change_user", "version", };
    private static final Object[][] ACCOUNT_VALUES = {
            { 1050L, 1000L, 1000L, "Savings", AccountType.BANK.name(), "N", "Savings Account", TEST_DATE, TEST_USERID, 0, },
            { 1051L, 1000L, 1000L, "Checking", AccountType.BANK.name(), "Y", "Checking Account", TEST_DATE, TEST_USERID, 0, },
            { 1052L, 1000L, null, "Cash", AccountType.CASH.name(), "N", "Cash Account", TEST_DATE, TEST_USERID, 0, },
    };
    protected static final QueryBatch ACCOUNT_BATCH = new QueryBatch("account", ACCOUNT_COLUMNS, ACCOUNT_VALUES, ASSET_BATCH, COMPANY_BATCH);

    private static final String[] TRANSACTION_GROUP_COLUMNS = { "id", "name", "description", "change_date", "change_user", "version", };
    private static final Object[][] TRANSACTION_GROUP_VALUES = {
            { 1000L, "Group 1", "Group one", TEST_DATE, TEST_USERID, 0, },
            { 1001L, "Group 2", "Group two", TEST_DATE, TEST_USERID, 0, },
    };
    protected static final QueryBatch TRANSACTION_GROUP_BATCH =
            new QueryBatch("tx_group", TRANSACTION_GROUP_COLUMNS, TRANSACTION_GROUP_VALUES);

    private static final String[] PAYEE_COLUMNS = { "id", "name", "change_date", "change_user", "version", };
    private static final Object[][] PAYEE_VALUES = {
            { 1000L, "Payee 1", TEST_DATE, TEST_USERID, 0, },
            { 1001L, "Payee 2", TEST_DATE, TEST_USERID, 0, },
    };
    protected static final QueryBatch PAYEE_BATCH = new QueryBatch("payee", PAYEE_COLUMNS, PAYEE_VALUES);

    private static final Set<QueryBatch> executedBatches = new HashSet<>();
    private static final Set<Class<?>> initializedClasses = new HashSet<>();
    private static Logger logger = Logger.getLogger(HsqlTestFixture.class);
    private final Supplier<TestDaoRepository> daoRepositorySupplier;
    protected TestDaoRepository daoContext;

    /**
     * Use {@link TestHibernateDaoContext}.
     */
    protected  HsqlTestFixture() {
        this(DaoContextSupplier.INSTANCE);
    }

    protected HsqlTestFixture(Supplier<TestDaoRepository> daoRepositorySupplier) {
        this.daoRepositorySupplier = daoRepositorySupplier;
    }

    @Before
    public void initDatabase() throws Exception {
        daoContext = daoRepositorySupplier.get();
        if (initializedClasses.isEmpty()) {
            new SchemaBuilder(daoContext).createSchemaTables(Collections.emptyList());
        }
        // TODO tear down the database after each test?
        // only run insert queries once per subclass
        if (! initializedClasses.contains(getClass())) {
            insertData();
            initializedClasses.add(getClass());
        }
    }

    protected void flushSession() {
        daoContext.flushSession();
    }

    protected void clearSession() {
        daoContext.clearSession();
    }

    private void insertData() throws Exception {
        daoContext.doInTransaction(connection -> getInsertQueries().forEach(batch -> runBatchIfUnexecuted(connection, batch)));
    }

    private static void runBatchIfUnexecuted(Connection connection, QueryBatch batch) {
        if (! executedBatches.contains(batch)) {
            try {
                batch.execute(connection);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    protected abstract List<QueryBatch> getInsertQueries();

    protected static class QueryBatch {
        private QueryBatch[] prerequisites;
        private String sql;
        private String[] columns;
        private Object[][] batchParameters;

        public QueryBatch(String table, String[] columns, Object[][] batchParameters, QueryBatch ... prerequisites) {
            this.prerequisites = prerequisites;
            this.sql = "insert into " + table + " (" + StringUtils.join(columns, ',')
                    + ") values (" + getPlaceholderList(columns.length) + ")";
            this.columns = columns;
            this.batchParameters = batchParameters;
        }

        private String getPlaceholderList(int count) {
            StringBuilder buffer = new StringBuilder();
            for (int i=0; i<count; i++) buffer.append(",?");
            return buffer.substring(1);
        }

        public int size() {
            return batchParameters.length;
        }

        public Object getValue(int row, String column) {
            for (int i=0; i<columns.length; i++) {
                if (columns[i].equals(column)) {
                    return batchParameters[row][i];
                }
            }
            return null;
        }

        public void execute(Connection connection) throws Exception {
            if (prerequisites != null) {
                for (QueryBatch queryBatch : prerequisites) {
                    runBatchIfUnexecuted(connection, queryBatch);
                }
            }
            logger.info("executing query batch: " + sql);
            PreparedStatement stmt = connection.prepareStatement(sql);
            for (Object[] values : batchParameters) {
                stmt.clearParameters();
                for (int i=0; i<values.length; i++) {
                    if (values[i] instanceof Timestamp) {
                        stmt.setTimestamp(i+1, (Timestamp)values[i]);
                    }
                    else {
                        stmt.setObject(i+1, values[i]);
                    }
                }
                stmt.executeUpdate();
            }
            executedBatches.add(this);
        }
    }
}
