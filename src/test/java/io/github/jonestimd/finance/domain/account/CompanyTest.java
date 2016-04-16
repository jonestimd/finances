package io.github.jonestimd.finance.domain.account;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompanyTest {

    @Test
    public void testEquals() throws Exception {
        assertFalse(new Company().equals(new Company()));

        Company company1 = TestDomainUtils.create(Company.class, 1L);
        company1.setName("Company1");
        Company company2 = TestDomainUtils.create(Company.class, 2L);
        company2.setName("company1");
        Company company3 = TestDomainUtils.create(Company.class, 3L);
        company3.setName("company3");

        assertTrue(company1.equals(company1));
        assertTrue(company1.equals(company2));
        assertFalse(company1.equals(company3));
    }
}
