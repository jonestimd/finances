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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.TransactionDetailDao;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Asset;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.hibernate.Query;
import org.hibernate.SessionFactory;

import static io.github.jonestimd.finance.domain.transaction.SecurityAction.*;

public class TransactionDetailDaoImpl extends HibernateDao<TransactionDetail, Long> implements TransactionDetailDao {
    public TransactionDetailDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory, TransactionDetail.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TransactionDetail> findOrphanTransfers() {
        return getSession().getNamedQuery(TransactionDetail.FIND_ORPHAN_TRANSFERS).list();
    }

    @SuppressWarnings("unchecked")
    public List<TransactionDetail> findSecuritySalesWithoutLots(String namePrefix, Date saleDate) {
        Query query = getSession().getNamedQuery(TransactionDetail.SECURITY_SALES_WITHOUT_LOTS);
        query.setParameter("securityName", namePrefix + "%");
        query.setParameter("saleDate", saleDate);
        query.setParameterList("actions", Arrays.asList(SELL.code(), SHARES_OUT.code()));
        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<TransactionDetail> findPurchasesWithRemainingShares(Account account, Asset security, Date purchaseDate) {
        Query query = getSession().getNamedQuery(TransactionDetail.UNSOLD_SECURITY_SHARES_BY_DATE);
        query.setParameter("account", account);
        query.setParameter("security", security);
        query.setParameter("purchaseDate", purchaseDate);
        query.setParameterList("actions", Arrays.asList(BUY.code(), SHARES_IN.code(), REINVEST.code()));
        return query.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TransactionDetail> findAvailablePurchaseShares(TransactionDetail sale) {
        return getSession().getNamedQuery(TransactionDetail.UNSOLD_SECURITY_SHARES)
            .setParameter("account", sale.getTransaction().getAccount())
            .setParameter("security", sale.getTransaction().getSecurity())
            .setParameter("saleDate", sale.getTransaction().getDate())
            .setParameterList("actions", Arrays.asList(BUY.code(), SHARES_IN.code(), REINVEST.code()))
            .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TransactionDetail> findByString(String search) {
        return getSession().getNamedQuery(TransactionDetail.FIND_BY_STRING)
                .setParameter("search", "%"+search.toLowerCase()+"%")
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TransactionDetail> findByCategoryIds(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) return Collections.emptyList();
        return getSession().getNamedQuery(TransactionDetail.FIND_BY_CATEGORY_IDS)
                .setParameterList("categoryIds", categoryIds)
                .list();
    }

    @Override
    public void replaceCategory(List<TransactionCategory> toReplace, TransactionCategory category) {
        getSession().getNamedQuery(TransactionDetail.REPLACE_CATEGORY_QUERY)
                .setParameterList("oldCategoryIds", Lists.transform(toReplace, UniqueId::getId))
                .setParameter("newCategoryId", category.getId())
                .executeUpdate();
    }
}
