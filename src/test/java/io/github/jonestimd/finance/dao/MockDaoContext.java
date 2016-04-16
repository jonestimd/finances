package io.github.jonestimd.finance.dao;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;

import io.github.jonestimd.finance.dao.hibernate.DomainEventRecorder;
import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.UniqueId;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.mockito.internal.stubbing.defaultanswers.GloballyConfiguredAnswer;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

public class MockDaoContext implements DaoRepository {
    private static final Answer<Object> DAO_ANSWER = new GloballyConfiguredAnswer() {
        @SuppressWarnings("unchecked")
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if ("save".equals(invocation.getMethod().getName())) {
                UniqueId<Long> entity = (UniqueId<Long>) invocation.getArguments()[0];
                assignId(entity);
                return entity;
            }
            if ("saveAll".equals(invocation.getMethod().getName()) && invocation.getArguments()[0] instanceof Iterable) {
                Iterable<UniqueId<Long>> entities = (Iterable<UniqueId<Long>>) invocation.getArguments()[0];
                for (UniqueId<Long> entity : entities) {
                    assignId(entity);
                }
                return entities;
            }
            if ("merge".equals(invocation.getMethod().getName())) {
                return invocation.getArguments()[0];
            }
            return super.answer(invocation);
        }

        private void assignId(UniqueId<Long> entity) throws Exception {
            if (entity.getId() == null) {
                TestDomainUtils.setId(entity);
            }
            cascadeSave(entity);
        }

        private void cascadeSave(UniqueId<Long> entity) throws Exception {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (isLongUniqueId(field.getType()) && isCascade(field, CascadeType.SAVE_UPDATE)) {
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    UniqueId<Long> fieldValue = (UniqueId<Long>) field.get(entity);
                    if (fieldValue != null) {
                        assignId(fieldValue);
                    }
                }
            }
        }

        private boolean isLongUniqueId(Class<?> type) throws Exception {
            return UniqueId.class.isAssignableFrom(type) && type.getMethod("getId").getReturnType().equals(Long.class);
        }

        private boolean isCascade(Field field, CascadeType cascadeType) {
            Cascade cascade = field.getAnnotation(Cascade.class);
            return cascade != null && Arrays.asList(cascade.value()).contains(cascadeType);
        }
    };

    private SessionFactory sessionFactory = mock(SessionFactory.class);
    private AccountDao accountDao = mock(AccountDao.class, DAO_ANSWER);
    private CompanyDao companyDao = mock(CompanyDao.class, DAO_ANSWER);
    private PayeeDao payeeDao = mock(PayeeDao.class, DAO_ANSWER);
    private CurrencyDao currencyDao = mock(CurrencyDao.class, DAO_ANSWER);
    private SecurityDao securityDao = mock(SecurityDao.class, DAO_ANSWER);
    private StockSplitDao stockSplitDao = mock(StockSplitDao.class, DAO_ANSWER);
    private SecurityLotDao securityLotDao = mock(SecurityLotDao.class, DAO_ANSWER);
    private TransactionDao transactionDao = mock(TransactionDao.class, DAO_ANSWER);
    private TransactionDetailDao transactionDetailDao = mock(TransactionDetailDao.class, DAO_ANSWER);
    private TransactionCategoryDao TransactionCategoryDao = mock(TransactionCategoryDao.class, DAO_ANSWER);
    private TransactionGroupDao transactionGroupDao = mock(TransactionGroupDao.class, DAO_ANSWER);
    private ImportFileDao importFileDao = mock(ImportFileDao.class, DAO_ANSWER);
    private DomainEventRecorder domainEventRecorder = mock(DomainEventRecorder.class);
    private Session session = mock(Session.class);
    private Transaction transaction = mock(Transaction.class);

    @Override
    public void generateSchema() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I, T extends I> I transactional(T target, Class<I> iface) {
        return target;
    }

    @Override
    public void doInTransaction(Runnable work) {
        throw new UnsupportedOperationException();
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    public CompanyDao getCompanyDao() {
        return companyDao;
    }

    public PayeeDao getPayeeDao() {
        return payeeDao;
    }

    public CurrencyDao getCurrencyDao() {
        return currencyDao;
    }

    public SecurityDao getSecurityDao() {
        return securityDao;
    }

    public StockSplitDao getStockSplitDao() {
        return stockSplitDao;
    }

    public SecurityLotDao getSecurityLotDao() {
        return securityLotDao;
    }

    public TransactionDao getTransactionDao() {
        return transactionDao;
    }

    @Override
    public TransactionDetailDao getTransactionDetailDao() {
        return transactionDetailDao;
    }

    public TransactionGroupDao getTransactionGroupDao() {
        return transactionGroupDao;
    }

    public TransactionCategoryDao getTransactionCategoryDao() {
        return TransactionCategoryDao;
    }

    @Override
    public ImportFileDao getImportFileDao() {
        return importFileDao;
    }

    @Override
    public DomainEventRecorder getDomainEventRecorder() {
        return domainEventRecorder;
    }

    public void resetMocks() {
        reset(sessionFactory, accountDao, companyDao, payeeDao, securityDao, stockSplitDao, transactionDao, transactionDetailDao,
                transactionGroupDao, TransactionCategoryDao, session, transaction, securityLotDao, domainEventRecorder);
    }

    private void beginTransaction() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        when(session.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(false);
        when(transaction.wasCommitted()).thenReturn(false);
        when(transaction.wasRolledBack()).thenReturn(false);
        transaction.begin();
    }

    public void expectCommit() {
        beginTransaction();
        transaction.commit();
        when(session.isOpen()).thenReturn(false);
    }

    public void expectRollback() {
        beginTransaction();
        transaction.rollback();
        when(session.isOpen()).thenReturn(false);
    }
}