package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class SecurityColumnAdapterTest {
    @Test
    public void getCostBasisReturnsNullForZeroShares() throws Exception {
        SecuritySummary summary = new SecuritySummary(null, 2, BigDecimal.ZERO, null, BigDecimal.TEN, BigDecimal.ONE);

        assertThat(SecurityColumnAdapter.COST_BASIS_ADAPTER.getValue(summary)).isNull();
    }

    @Test
    public void getCostBasisReturnsValueForPositiveShares() throws Exception {
        SecuritySummary summary = new SecuritySummary(null, 2, new BigDecimal("0.001"), null, BigDecimal.TEN, BigDecimal.ONE);

        assertThat(SecurityColumnAdapter.COST_BASIS_ADAPTER.getValue(summary)).isEqualTo(BigDecimal.TEN);
    }
}