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
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;

public class PayeeDaoImpl extends HibernateDao<Payee, Long> implements PayeeDao {
    public PayeeDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory, Payee.class);
    }

    public Payee getPayee(String name) {
        return findUnique(Restrictions.eq(Payee.NAME, name));
    }

    @SuppressWarnings("unchecked")
    public List<PayeeSummary> getPayeeSummaries() {
        return getSession().getNamedQuery(Payee.SUMMARY_QUERY)
            .setResultTransformer(new AliasToBeanConstructorResultTransformer(getPayeeSummaryConstructor()))
            .list();
    }

    private Constructor<PayeeSummary> getPayeeSummaryConstructor() {
        try {
            return PayeeSummary.class.getConstructor(Payee.class, long.class, Date.class);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }
}