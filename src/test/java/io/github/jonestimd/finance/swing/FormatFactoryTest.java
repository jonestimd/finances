package io.github.jonestimd.finance.swing;

import java.math.BigDecimal;
import java.text.Format;
import java.text.NumberFormat;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class FormatFactoryTest {
    @Test
    public void currencyFormat() throws Exception {
        Format format = FormatFactory.currencyFormat();

        assertThat(format.format(BigDecimal.TEN)).isEqualTo("$10.00");
        assertThat(format.format(BigDecimal.TEN.negate())).isEqualTo("$-10.00");
    }

    @Test
    public void numberFormat() throws Exception {
        NumberFormat format = FormatFactory.numberFormat();

        assertThat(format.format(1.987654321111)).isEqualTo("1.987654321");
    }

    @Test
    public void formatByFunction() throws Exception {
        Format format = FormatFactory.format(Object::toString);

        assertThat(format.format(1234)).isEqualTo("1234");
        assertThat(format.format(null)).isEqualTo("");
    }
}