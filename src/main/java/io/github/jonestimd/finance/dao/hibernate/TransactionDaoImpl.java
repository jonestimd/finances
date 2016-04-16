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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public class TransactionDaoImpl extends HibernateDao<Transaction, Long> implements TransactionDao {
    public TransactionDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory, Transaction.class);
    }

    @SuppressWarnings("unchecked")
    public List<TransactionDetail> findTransferDetails(Account account1, String account2Name, Date date,
                                                       BigDecimal amount, TransactionGroup group) {
        Criteria criteria = getSession().createCriteria(TransactionDetail.class);
        criteria.add(Restrictions.eq(TransactionDetail.AMOUNT, amount));
        criteria.add(nullableEquals(TransactionDetail.GROUP, group));
        criteria.createCriteria(TransactionDetail.TRANSACTION)
                .add(Restrictions.eq(Transaction.ACCOUNT, account1))
                .add(Restrictions.eq(Transaction.DATE, date));
        criteria.createCriteria(TransactionDetail.RELATED_DETAIL)
                .createCriteria(TransactionDetail.TRANSACTION)
                .createCriteria(Transaction.ACCOUNT)
                .add(Restrictions.eq(Account.NAME, account2Name));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<Transaction> getTransactions(long accountId) {
        Criteria criteria = getSession().createCriteria(Transaction.class);
        criteria.add(getProperty(Transaction.ACCOUNT, Account.ID).eq(accountId));
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        criteria.addOrder(Order.asc(Transaction.DATE));
        criteria.addOrder(Order.asc(Transaction.ID));
        return criteria.list();
    }

    @Override
    public void replacePayee(List<Payee> toReplace, Payee payee) {
        getSession().getNamedQuery(Transaction.REPLACE_PAYEE_QUERY)
                .setParameterList("oldPayeeIds", Lists.transform(toReplace, UniqueId::getId))
                .setParameter("newPayeeId", payee.getId())
                .executeUpdate();
    }

    @Override
    public Transaction findLatestForPayee(long payeeId) {
        return (Transaction) getSession().getNamedQuery(Transaction.LATEST_FOR_PAYEE_QUERY)
                .setLong("payeeId", payeeId)
                .setMaxResults(1)
                .uniqueResult();
    }
}