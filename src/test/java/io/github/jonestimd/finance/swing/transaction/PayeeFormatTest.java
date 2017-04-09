package io.github.jonestimd.finance.swing.transaction;

import java.text.ParseException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class PayeeFormatTest {
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
