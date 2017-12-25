package io.github.jonestimd.collection;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class BigDecimalsTest {
    @Test
    public void sum() throws Exception {
        List<BigDecimal> items = Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(3L));

        assertThat(BigDecimal.valueOf(14L).compareTo(BigDecimals.sum(items))).isEqualTo(0);
        assertThat(BigDecimal.valueOf(14L).compareTo(BigDecimals.sum(items, Function.<BigDecimal>identity()))).isEqualTo(0);
    }

    @Test
    public void isEqualReturnsFalseForNulls() throws Exception {
        assertThat(BigDecimals.isEqual(null, null)).isFalse();
        assertThat(BigDecimals.isEqual(null, BigDecimal.ONE)).isFalse();
        assertThat(BigDecimals.isEqual(BigDecimal.ONE, null)).isFalse();
    }

    @Test
    public void isEqualComparesNonNullValues() throws Exception {
        assertThat(BigDecimals.isEqual(BigDecimal.ONE, new BigDecimal(1))).isTrue();
        assertThat(BigDecimals.isEqual(BigDecimal.ONE, new BigDecimal(1.0d))).isTrue();
        assertThat(BigDecimals.isEqual(BigDecimal.ONE, new BigDecimal(1.1d))).isFalse();
    }
}
