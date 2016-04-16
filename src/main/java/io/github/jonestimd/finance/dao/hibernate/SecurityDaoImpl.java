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

import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.reflect.ReflectionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.ResultTransformer;

import static io.github.jonestimd.reflect.ReflectionUtils.*;

public class SecurityDaoImpl extends HibernateDao<Security, Long> implements SecurityDao {
    private static final ResultTransformer SUMMARY_TRANSFORMER = new AliasToBeanConstructorResultTransformer(
            constructor(SecuritySummary.class, Security.class, long.class, BigDecimal.class, Date.class, BigDecimal.class, BigDecimal.class));
    private static final ResultTransformer ACCOUNT_SUMMARY_TRANSFORMER = new AliasToBeanConstructorResultTransformer(
            constructor(SecuritySummary.class, Security.class, long.class, BigDecimal.class));
    private static final ResultTransformer ACCOUNT_SUMMARIES_TRANSFORMER = new AliasToBeanConstructorResultTransformer(
            ReflectionUtils.constructor(SecuritySummary.class,
                    Account.class, Security.class, long.class, BigDecimal.class, Date.class, BigDecimal.class, BigDecimal.class));

    public SecurityDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory, Security.class);
    }

    @Override
    public Security getSecurity(String symbol) {
        return findUnique(Restrictions.eq(Security.SYMBOL, symbol));
    }

    @Override
    public Security findByName(String name) {
        return findUnique(Restrictions.eq(Security.NAME, name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SecuritySummary> getSecuritySummaries() {
        return getSession().getNamedQuery(Security.SECURITY_SUMMARY)
            .setResultTransformer(SUMMARY_TRANSFORMER)
            .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SecuritySummary> getSecuritySummaries(Long accountId) {
        return getSession().getNamedQuery(Security.ACCOUNT_SECURITY_SUMMARY)
                .setParameter("accountId", accountId)
                .setResultTransformer(ACCOUNT_SUMMARY_TRANSFORMER)
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SecuritySummary> getSecuritySummariesByAccount() {
        return getSession().getNamedQuery(Security.SECURITY_SUMMARY_BY_ACCOUNT)
                .setResultTransformer(ACCOUNT_SUMMARIES_TRANSFORMER)
                .list();
    }
}