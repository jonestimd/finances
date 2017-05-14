package io.github.jonestimd.finance.swing.transaction;

import java.text.ParseException;

import io.github.jonestimd.finance.domain.transaction.Payee;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

public class PayeeFormatTest {
    @Test
    public void formatIgnoresNull() throws Exception {
        assertThat(new PayeeFormat().format(null)).isEqualTo("");
    }

    @Test
    public void formatReturnsName() throws Exception {
        assertThat(new PayeeFormat().format(new Payee("name"))).isEqualTo("name");
    }

    @Test
    public void parseNullThrowsException() throws Exception {
        try {
            new PayeeFormat().parseObject(null);
            fail("expected exception");
        } catch (ParseException ex) {
            assertThat(ex.getMessage()).isEqualTo("Format.parseObject(String) failed");
        }
    }

    @Test
    public void parseEmptyStringThrowsException() throws Exception {
        try {
            new PayeeFormat().parseObject("");
            fail("expected exception");
        } catch (ParseException ex) {
            assertThat(ex.getMessage()).isEqualTo("Format.parseObject(String) failed");
        }
    }
    @Test

    public void parseEmptyWhiteSpaceThrowsException() throws Exception {
        try {
            new PayeeFormat().parseObject(" ");
            fail("expected exception");
        } catch (ParseException ex) {
            assertThat(ex.getMessage()).isEqualTo("Format.parseObject(String) failed");
        }
    }

    @Test
    public void parseTrimsWhiteSpace() throws Exception {
        assertThat((new PayeeFormat().parseObject(" name ")).getName()).isEqualTo("name");
    }
}
