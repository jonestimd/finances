package io.github.jonestimd.finance.dao.hibernate;

import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.dao.CompanyDao;
import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.domain.account.Company;
import org.hibernate.Hibernate;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CompanyDaoImplTest extends HsqlTestFixture {
    private CompanyDao companyDao;

    protected List<QueryBatch> getInsertQueries() {
        return Collections.singletonList(ACCOUNT_BATCH);
    }

    @Before
    public void setUpDao() throws Exception {
        companyDao = daoContext.getCompanyDao();
    }

    @Test
    public void testAccountsIsLazy() throws Exception {
        Company company = companyDao.get(1000L);

        assertThat(Hibernate.isInitialized(company.getAccounts())).isFalse();
    }
}