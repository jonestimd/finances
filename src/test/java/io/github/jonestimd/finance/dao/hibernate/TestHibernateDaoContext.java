package io.github.jonestimd.finance.dao.hibernate;

import java.sql.Connection;
import java.util.Properties;
import java.util.function.Consumer;

import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.dao.TestDaoRepository;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;

public class TestHibernateDaoContext extends HibernateDaoContext implements TestDaoRepository {
    public static final Properties CONNECTION_PROPERTIES = new Properties();
    static {
        CONNECTION_PROPERTIES.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        CONNECTION_PROPERTIES.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        CONNECTION_PROPERTIES.put("hibernate.connection.url", "jdbc:hsqldb:mem:finances");
        CONNECTION_PROPERTIES.put("hibernate.connection.username", "sa");
        CONNECTION_PROPERTIES.put("hibernate.connection.password", "");
    }

    private org.hibernate.Transaction transaction;

    public TestHibernateDaoContext() {
        super(CONNECTION_PROPERTIES);
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
