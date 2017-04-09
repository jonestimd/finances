package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class SecuritySummaryFormatTest {
    private final SecuritySummaryFormat format = new SecuritySummaryFormat();

    @Test
    public void formatNull() throws Exception {
        assertThat(format.format(null)).isEqualTo("");
    }

    @Test
    public void formatSecuritySummary() throws Exception {
        assertThat(format.format(new SecuritySummary(new Security("Security name", SecurityType.STOCK), 0L, BigDecimal.ZERO))).isEqualTo("Security name");
    }
}