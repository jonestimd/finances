package io.github.jonestimd.finance.dao.hibernate;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.dao.TestDaoRepository;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.DriverService;
import io.github.jonestimd.finance.plugin.EmbeddedDriverConnectionService;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;

public class TestHibernateDaoContext extends HibernateDaoContext implements TestDaoRepository {
    private static final Config CONFIG = ConfigFactory.empty().withValue(DIRECTORY.toString(), ConfigValueFactory.fromAnyRef("finances"));
    private static class HsqlDriverConfigurationService extends EmbeddedDriverConnectionService {
        public HsqlDriverConfigurationService() {
            super("Hsql", "org.hibernate.dialect.HSQLDialect", "org.hsqldb.jdbcDriver", "hsqldb:mem:");
        }

        @Override
        public boolean isEnabled(Field field) {
            return false;
        }

        @Override
        public boolean isRequired(Field field) {
            return false;
        }

        @Override
        public Map<Field, String> getDefaultValues() {
            return Collections.emptyMap();
        }
    }

    private org.hibernate.Transaction transaction;

    public TestHibernateDaoContext() {
        super(new DriverService(new HsqlDriverConfigurationService(), CONFIG), ConfigFactory.load());
    }

    @Override
    public void beginTransaction() {
        transaction = sessionFactory.getCurrentSession().beginTransaction();
    }

    @Override
    public void rollbackTransaction() {
        Session session = sessionFactory.getCurrentSession();
        transaction.rollback();
        if (session.isOpen()) {
            session.close();
        }
    }

    @Override
    public void doInTransaction(Consumer<Connection> work) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            session.doWork(work::accept);
            transaction.commit();
        } catch (Exception ex) {
            transaction.rollback();
            throw ex;
        } finally {
            session.close();
        }
    }

    @Override
    public void flushSession() {
        sessionFactory.getCurrentSession().flush();
    }

    @Override
    public void clearSession() {
        sessionFactory.getCurrentSession().clear();
    }

    @Override
    public long countAll(Class<?> entityClass) {
        Session session = sessionFactory.openSession();
        try {
            Criteria criteria = session.createCriteria(entityClass);
            criteria.setProjection(Projections.rowCount());
            return (Long) criteria.uniqueResult();
        }
        finally {
            session.close();
        }
    }
}
