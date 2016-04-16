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

import java.util.List;

import io.github.jonestimd.finance.dao.SecurityLotDao;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;

public class SecurityLotDaoImpl extends HibernateDao<SecurityLot, Long> implements SecurityLotDao {
    public SecurityLotDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory, SecurityLot.class);
    }

    @SuppressWarnings("unchecked")
    public List<SecurityLot> findLotsForPurchases(List<? extends TransactionDetail> purchases) {
        Criteria criteria = getSession().createCriteria(SecurityLot.class);
        criteria.add(getProperty(SecurityLot.PURCHASE).in(purchases));
        return criteria.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SecurityLot> findBySale(TransactionDetail sale) {
        return getSession().getNamedQuery(SecurityLot.FIND_BY_SALE_ID)
                .setParameter("saleId", sale.getId())
                .list();
    }

    @Override
    public void deleteSaleLots(TransactionDetail sale) {
        getSession().getNamedQuery(SecurityLot.DELETE_BY_SALE_ID)
                .setParameter("saleId", sale.getId())
                .executeUpdate();
    }
}