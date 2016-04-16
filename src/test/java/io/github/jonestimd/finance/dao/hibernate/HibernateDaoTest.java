package io.github.jonestimd.finance.dao.hibernate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

public class HibernateDaoTest {
    protected SessionFactory sessionFactory = mock(SessionFactory.class);
    protected MockSession session = mock(MockSession.class);
    protected Criteria criteria = mock(Criteria.class);

    private TestDao dao = new TestDao(sessionFactory);

    @Before
    public final void setUp() throws Exception {
        when(sessionFactory.getCurrentSession()).thenReturn(session);
    }

    @Test
    public void getAllIsCacheableAndReturnsDistinctResult() throws Exception {
        List<Object> expectedResult = new ArrayList<Object>();
        when(session.createCriteria(Object.class)).thenReturn(criteria);
        when(criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).thenReturn(criteria);
        when(criteria.setCacheable(true)).thenReturn(criteria);
        when(criteria.setFetchMode("association", FetchMode.JOIN)).thenReturn(criteria);
        when(criteria.list()).thenReturn(expectedResult);

        assertSame(expectedResult, dao.getAll("association"));

        verify(sessionFactory, atLeastOnce()).getCurrentSession();
        verify(criteria).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        verify(criteria).setCacheable(true);
        verify(criteria).setFetchMode("association", FetchMode.JOIN);
    }

    @Test
    public void getSearchesById() throws Exception {
        Long id = new Long(0);
        Object expectedResult = new Object();
        when(session.get(Object.class, id)).thenReturn(expectedResult);

        Object result = dao.get(id);

        verify(sessionFactory, atLeastOnce()).getCurrentSession();
        assertSame(expectedResult, result);
    }

    @Test
    public void saveCallsSaveOrUpdate() throws Exception {
        Object expectedResult = new Object();

        Object result = dao.save(expectedResult);

        verify(sessionFactory, atLeastOnce()).getCurrentSession();
        verify(session).saveOrUpdate(expectedResult);
        assertSame(expectedResult, result);
    }

    @Test
    public void testSaveAllCallsSaveOrUpdate() throws Exception {
        List<Object> inObjects = Arrays.asList(new Object(), new Object());

        List<Object> result = dao.saveAll(inObjects);

        verify(sessionFactory, atLeastOnce()).getCurrentSession();
        verify(session).saveOrUpdate(inObjects.get(0));
        verify(session).saveOrUpdate(inObjects.get(1));
        assertSame(inObjects, result);
    }

    @Test
    public void testDeleteAllCallsDelete() throws Exception {
        List<Object> inObjects = Arrays.asList(new Object(), new Object());

        dao.deleteAll(inObjects);

        verify(sessionFactory, atLeastOnce()).getCurrentSession();
        verify(session).delete(inObjects.get(0));
        verify(session).delete(inObjects.get(1));
    }

    private class TestDao extends HibernateDao<Object, Long> {
        protected TestDao(SessionFactory sessionFactory) {
            super(sessionFactory, Object.class);
        }
    }

    public static interface MockSession extends Session, SessionImplementor {}
}