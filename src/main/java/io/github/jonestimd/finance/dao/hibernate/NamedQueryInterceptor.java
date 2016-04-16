// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.dao.hibernate;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.github.jonestimd.finance.dao.BaseDao;
import io.github.jonestimd.reflect.GenericsHelper;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class NamedQueryInterceptor<ENTITY, PK extends Serializable> implements InvocationHandler {
    private final BaseDao<ENTITY, PK> target;
    private final String queryPrefix;
    private final SessionFactory sessionFactory;

    @SuppressWarnings("unchecked")
    public static <ENTITY, PK extends Serializable, T extends BaseDao<ENTITY, PK>> T createDao(SessionFactory sessionFactory, Class<T> daoClass) {
        return (T) Proxy.newProxyInstance(daoClass.getClassLoader(), new Class[]{daoClass}, new NamedQueryInterceptor<ENTITY, PK>(getEntityClass(daoClass), sessionFactory));
    }

    @SuppressWarnings("unchecked")
    private static <ENTITY, PK extends Serializable, T extends BaseDao<ENTITY, PK>> Class<ENTITY> getEntityClass(Class<T> daoClass) {
        return (Class<ENTITY>) GenericsHelper.getInterfaceParameterType(daoClass, BaseDao.class, "ENTITY");
    }

    private static String getQueryPrefix(Class<?> entity) {
        return entity.getSimpleName() + '.';
    }

    public NamedQueryInterceptor(Class<ENTITY> entityClass, SessionFactory sessionFactory) {
        this(new HibernateDao<ENTITY, PK>(sessionFactory, entityClass), entityClass, sessionFactory);
    }

    public NamedQueryInterceptor(BaseDao<ENTITY, PK> target, Class<?> entityClass, SessionFactory sessionFactory) {
        this.target = target;
        this.queryPrefix = getQueryPrefix(entityClass);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(BaseDao.class)) {
            return method.invoke(target, args);
        }
        Session session = sessionFactory.getCurrentSession();
        try {
            Query query = session.getNamedQuery(queryPrefix + method.getName());
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                query.setParameter(i, args[i]);
            }
            if (method.getName().startsWith("findOne")) {
                return query.uniqueResult();
            }
            if (method.getName().startsWith("find")) {
                return query.list();
            }
            return query.executeUpdate();
        } catch (MappingException ex) {
            return method.invoke(target, args);
        }
    }
}
