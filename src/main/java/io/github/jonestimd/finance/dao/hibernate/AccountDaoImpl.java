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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import io.github.jonestimd.finance.dao.AccountDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.Company;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanConstructorResultTransformer;

public class AccountDaoImpl extends HibernateDao<Account, Long> implements AccountDao {
    public AccountDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory, Account.class);
    }

    public Account getAccount(Company company, String name) {
        Property companyProperty = getProperty(Account.COMPANY);
        Criterion companyCriterion = company == null ? companyProperty.isNull() : companyProperty.eq(company);
        return findUnique(companyCriterion, Restrictions.eq(Account.NAME, name));
    }

    @SuppressWarnings("unchecked")
    public List<AccountSummary> getAccountSummaries() {
        return getSession().getNamedQuery(Account.SUMMARY_QUERY)
            .setResultTransformer(new AliasToBeanConstructorResultTransformer(getAccountSummaryConstructor()))
            .list();
    }

    private Constructor<AccountSummary> getAccountSummaryConstructor() {
        try {
            return AccountSummary.class.getConstructor(Account.class, Long.class, BigDecimal.class);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void removeAccountsFromCompanies(final Collection<Company> companies) {
        Query query = getSession().getNamedQuery("account.removeFromCompany");
        query.setParameterList("companies", companies);
        query.executeUpdate();
    }
}