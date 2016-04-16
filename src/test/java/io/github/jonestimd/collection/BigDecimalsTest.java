package io.github.jonestimd.collection;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class BigDecimalsTest {

    @Test
    public void sum() throws Exception {
        List<BigDecimal> items = Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(3L));

        assertThat(BigDecimal.valueOf(14L).compareTo(BigDecimals.sum(items))).isEqualTo(0);
        assertThat(BigDecimal.valueOf(14L).compareTo(BigDecimals.sum(items, Function.<BigDecimal>identity()))).isEqualTo(0);
    }
}
