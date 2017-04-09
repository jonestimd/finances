package io.github.jonestimd.finance.domain.account;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CompanyTest {

    @Test
    public void testEquals() throws Exception {
        assertThat(new Company().equals(new Company())).isFalse();

        Company company1 = TestDomainUtils.create(Company.class, 1L);
        company1.setName("Company1");
        Company company2 = TestDomainUtils.create(Company.class, 2L);
        company2.setName("company1");
        Company company3 = TestDomainUtils.create(Company.class, 3L);
        company3.setName("company3");

        assertThat(company1.equals(company1)).isTrue();
        assertThat(company1.equals(company2)).isTrue();
        assertThat(company1.equals(company3)).isFalse();
    }
}
