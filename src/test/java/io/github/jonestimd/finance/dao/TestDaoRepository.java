package io.github.jonestimd.finance.dao;

import java.sql.Connection;
import java.util.function.Consumer;

public interface TestDaoRepository extends DaoRepository {
    void beginTransaction();
    void rollbackTransaction();
    void doInTransaction(Consumer<Connection> work);
    void flushSession();
    void clearSession();
    long countAll(Class<?> entityClass);
}
