package io.github.jonestimd.finance.swing.account;

import java.text.ParseException;

import io.github.jonestimd.finance.domain.account.Company;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CompanyFormatTest {
    @Test
    public void parseObjectSetsName() throws Exception {
        Object obj = new CompanyFormat().parseObject("company name");

        assertThat(obj).isInstanceOf(Company.class);
        Company company = (Company) obj;
        assertThat(company.getName()).isEqualTo("company name");
        assertThat(company.getId()).isNull();
    }

    @Test
    public void parseObjectTrimsInput() throws Exception {
        Object obj = new CompanyFormat().parseObject(" company name ");

        assertThat(obj).isInstanceOf(Company.class);
        Company company = (Company) obj;
        assertThat(company.getName()).isEqualTo("company name");
        assertThat(company.getId()).isNull();
    }

    @Test(expected = ParseException.class)
    public void parseNullThrowsException() throws Exception {
        new CompanyFormat().parseObject(null);
    }

    @Test(expected = ParseException.class)
    public void parseEmptyStringThrowsException() throws Exception {
        new CompanyFormat().parseObject("");
    }

    @Test(expected = ParseException.class)
    public void parseBlankStringThrowsException() throws Exception {
        new CompanyFormat().parseObject(" ");
    }
}
