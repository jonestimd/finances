package io.github.jonestimd.finance.dao.hibernate;

import java.util.function.Supplier;

import io.github.jonestimd.finance.dao.TestDaoRepository;

public class DaoContextSupplier implements Supplier<TestDaoRepository> {
    public static final DaoContextSupplier INSTANCE = new DaoContextSupplier();
    private static TestDaoRepository daoRepository;

    private DaoContextSupplier() {}

    @Override
    public TestDaoRepository get() {
        if (daoRepository == null) {
            daoRepository = new TestHibernateDaoContext();
        }
        return daoRepository;
    }
}
