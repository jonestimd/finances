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
import java.util.List;
import java.util.stream.Stream;

import io.github.jonestimd.finance.dao.BaseDao;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

public class HibernateDao<ENTITY, PK extends Serializable> implements BaseDao<ENTITY, PK> {
    private final SessionFactory sessionFactory;
    private final Class<ENTITY> entityClass;

    public HibernateDao(SessionFactory sessionFactory, Class<ENTITY> entityClass) {
        this.sessionFactory = sessionFactory;
        this.entityClass = entityClass;
    }

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public List<ENTITY> getAll(String ... associations) {
        return getAll(entityClass, associations);
    }

    @SuppressWarnings("unchecked")
    protected  <T extends ENTITY> List<T> getAll(Class<T> criteriaClass, String ... associations) {
        return buildGetAllCriteria(criteriaClass, associations).list();
    }

    protected <T extends ENTITY> Criteria buildGetAllCriteria(Class<T> criteriaClass, String ... associations) {
        Criteria criteria = getSession().createCriteria(criteriaClass);
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        criteria.setCacheable(true);
        for (String path : associations) {
            criteria.setFetchMode(path, FetchMode.JOIN);
        }
        return criteria;
    }

    @SuppressWarnings("unchecked")
    public ENTITY get(PK id) {
        return (ENTITY) getSession().get(entityClass, id);
    }

    public <T extends ENTITY> T save(T persistentEntity) {
        getSession().saveOrUpdate(persistentEntity);
        return persistentEntity;
    }

    public <T extends Iterable<? extends ENTITY>> T saveAll(T entities) {
        Session session = getSession();
        entities.forEach(session::saveOrUpdate);
        return entities;
    }

    @Override
    public void saveAll(Stream<? extends ENTITY> entities) {
        Session session = getSession();
        entities.forEach(session::saveOrUpdate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ENTITY> T merge(T persistentEntity) {
        return (T) getSession().merge(persistentEntity);
    }

    public <T extends ENTITY> void delete(T persistentEntity) {
        getSession().delete(persistentEntity);
    }

    public void deleteAll(Iterable<? extends ENTITY> entities) {
        Session session = getSession();
        entities.forEach(session::delete);
    }

    @Override
    public void deleteAll(Stream<? extends ENTITY> entities) {
        Session session = getSession();
        entities.forEach(session::delete);
    }

    protected Criterion nullableEquals(String propertyName, Object value) {
        return value == null ? Restrictions.isNull(propertyName) : Restrictions.eq(propertyName, value);
    }

    protected ENTITY findUnique(Criterion ... restrictions) {
        return findUnique(entityClass, restrictions);
    }

    @SuppressWarnings("unchecked")
    protected <T extends ENTITY> T findUnique(Class<T> criteriaClass, Criterion ... restrictions) {
        Criteria criteria = getSession().createCriteria(criteriaClass);
        for (Criterion criterion : restrictions) {
            criteria.add(criterion);
        }
        return (T) criteria.uniqueResult();
    }

    protected Property getProperty(String name, String ... paths) {
        Property property = Property.forName(name);
        for (String path : paths) {
            property = property.getProperty(path);
        }
        return property;
    }
}