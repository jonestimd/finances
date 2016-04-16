package io.github.jonestimd.finance.dao;

import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;

public abstract class TransactionalTestFixture extends HsqlTestFixture {
    protected TransactionalTestFixture() {}

    protected TransactionalTestFixture(Supplier<TestDaoRepository> daoRepositorySupplier) {
        super(daoRepositorySupplier);
    }

    @Before
    public void startTransaction() throws Exception {
        daoContext.beginTransaction();
    }

    @After
    public void rollbackTransaction() throws Exception {
        daoContext.rollbackTransaction();
    }
}