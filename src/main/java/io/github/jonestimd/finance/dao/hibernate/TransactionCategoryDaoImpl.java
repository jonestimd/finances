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

import java.lang.reflect.Constructor;
import java.util.List;

import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;

public class TransactionCategoryDaoImpl extends HibernateDao<TransactionCategory, Long> implements TransactionCategoryDao {
    public TransactionCategoryDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory, TransactionCategory.class);
    }

    public TransactionCategory getTransactionCategory(final String ... codes) {
        Criteria criteria = getSession().createCriteria(TransactionCategory.class);
        criteria.add(Restrictions.eq(TransactionCategory.CODE, codes[codes.length-1]));
        criteria.add(Restrictions.eq(TransactionCategory.SECURITY, false));
        for (int i = codes.length-2; i >= 0; i--) {
            criteria = criteria.createCriteria(TransactionCategory.PARENT);
            criteria.add(Restrictions.eq(TransactionCategory.CODE, codes[i]));
        }
        criteria.add(Restrictions.isNull(TransactionCategory.PARENT));
        return (TransactionCategory) criteria.uniqueResult();
    }

    public TransactionCategory getSecurityAction(String code) {
        return findUnique(TransactionCategory.class, Restrictions.eq(TransactionCategory.CODE, code),
                Restrictions.isNull(TransactionCategory.PARENT),
                Restrictions.eq(TransactionCategory.SECURITY, true));
    }

    @SuppressWarnings("unchecked")
    public List<TransactionCategorySummary> getTransactionCategorySummaries() {
        return getSession().getNamedQuery(TransactionCategory.SUMMARY_QUERY)
            .setResultTransformer(new AliasToBeanConstructorResultTransformer(getSummaryConstructor()))
            .list();
    }

    private Constructor<TransactionCategorySummary> getSummaryConstructor() {
        try {
            return TransactionCategorySummary.class.getConstructor(TransactionCategory.class, long.class);
        }
        catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TransactionCategory> getParentCategories() {
        return getSession().getNamedQuery(TransactionCategory.PARENTS_QUERY).list();
    }
}