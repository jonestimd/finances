package io.github.jonestimd.hibernate;

import java.lang.reflect.Method;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TransactionInterceptorTest {
    private SessionFactory sessionFactory = mock(SessionFactory.class);
    private Session session1 = mock(Session.class);
    private Session session2 = mock(Session.class);
    private Transaction transaction1 = mock(Transaction.class);
    private Transaction transaction2 = mock(Transaction.class);
    private TestTarget target = mock(TestTarget.class);
    private TransactionInterceptor interceptor = new TransactionInterceptor(target, sessionFactory);

    private Method getMethod() throws Exception {
        return TestTarget.class.getDeclaredMethod("doSomething");
    }

    private void beginTransaction(Session session, Transaction transaction) {
        when(session.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(false);
        when(transaction.wasCommitted()).thenReturn(false);
        when(transaction.wasRolledBack()).thenReturn(false);
    }

    @Test
    public void testCommitTransaction() throws Throwable {
        when(sessionFactory.getCurrentSession()).thenReturn(session1, session2);
        when(target.doSomething()).thenReturn("result1", "result2");
        beginTransaction(session1, transaction1);
        when(session1.isOpen()).thenReturn(true);
        beginTransaction(session2, transaction2);
        when(session2.isOpen()).thenReturn(false);

        assertThat(interceptor.invoke(null, getMethod(), new Object[0])).isEqualTo("result1");
        assertThat(interceptor.invoke(null, getMethod(), new Object[0])).isEqualTo("result2");

        verify(transaction1).begin();
        verify(transaction1).commit();
        verify(transaction2).begin();
        verify(transaction2).commit();
        verify(session1).close();
    }

    @Test
    public void testUseExistingTransaction() throws Throwable {
        when(sessionFactory.getCurrentSession()).thenReturn(session1);
        beginTransaction(session1, transaction1);
        when(target.doSomething()).thenReturn("result");
        when(session1.isOpen()).thenReturn(false);

        assertThat(new TransactionInterceptor(new NestedTarget(), sessionFactory).invoke(null, getMethod(), new Object[0]))
            .isEqualTo("result");

        verify(transaction1).begin();
        verify(transaction1).commit();
    }

    @Test
    public void testRollbackTransactionOnException() throws Throwable {
        when(sessionFactory.getCurrentSession()).thenReturn(session1, session2);
        when(target.doSomething())
            .thenThrow(new RuntimeException())
            .thenReturn("result2");
        beginTransaction(session1, transaction1);
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(transaction1).rollback();
        when(session1.isOpen()).thenReturn(false);

        beginTransaction(session2, transaction2);
        when(session2.isOpen()).thenReturn(true);

        try {
            interceptor.invoke(null, getMethod(), new Object[0]);
            fail("expected exception");
        }
        catch (RuntimeException ex) {
            assertThat(ex).isSameAs(exception);
        }
        assertThat(interceptor.invoke(null, getMethod(), new Object[0])).isEqualTo("result2");

        verify(transaction1).begin();
    }

    @Test
    public void testRollbackTransactionOnNestedException() throws Throwable {
        when(sessionFactory.getCurrentSession()).thenReturn(session1);
        beginTransaction(session1, transaction1);
        RuntimeException exception = new RuntimeException();
        when(target.doSomething()).thenReturn("result");
        doThrow(exception).when(transaction1).commit();
        when(session1.isOpen()).thenReturn(true);

        try {
            new TransactionInterceptor(new NestedTarget(), sessionFactory).invoke(null, getMethod(), new Object[0]);
            fail("expected exception");
        }
        catch (RuntimeException ex) {
            assertThat(ex).isSameAs(exception);
        }

        verify(transaction1).begin();
        verify(transaction1).rollback();
        verify(session1).close();
    }

    @Test
    public void testRollbackTransactionOnInvocationException() throws Throwable {
        when(sessionFactory.getCurrentSession()).thenReturn(session1);
        beginTransaction(session1, transaction1);
        RuntimeException exception = new RuntimeException();
        when(target.doSomething()).thenThrow(exception);
        when(session1.isOpen()).thenReturn(true);

        try {
            interceptor.invoke(null, getMethod(), new Object[0]);
            fail("expected exception");
        }
        catch (Throwable ex) {
            assertThat(ex).isSameAs(exception);
        }

        verify(transaction1).begin();
        verify(transaction1).rollback();
        verify(session1).close();
    }

    public static interface TestTarget {
        String doSomething();
    }

    private class NestedTarget implements TestTarget {
        public String doSomething() {
            try {
                return (String) interceptor.invoke(null, getMethod(), new Object[0]);
            }
            catch (Throwable e) {
                fail("unexpected exception");
                return null;
            }
        }
    }
}