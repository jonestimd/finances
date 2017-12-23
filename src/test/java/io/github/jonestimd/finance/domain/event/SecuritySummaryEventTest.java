package io.github.jonestimd.finance.domain.event;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class SecuritySummaryEventTest {

    private final Security security = TestDomainUtils.setId(new Security());

    @Test
    public void getTotals() throws Exception {
        SecuritySummaryEvent event = new SecuritySummaryEvent("test", EventType.REPLACED,
                Arrays.asList(newSecuritySummary(2, 10, 5, 9), newSecuritySummary(3, 15, 8, 11)));

        Map<Long, SecuritySummary> totals = SecuritySummaryEvent.getTotals(event);

        assertThat(totals).hasSize(1);
        SecuritySummary summary = totals.get(security.getId());
        assertThat(summary.getTransactionCount()).isEqualTo(5);
        assertThat(summary.getShares()).isEqualTo(new BigDecimal(25));
        assertThat(summary.getCostBasis()).isEqualTo(new BigDecimal(13));
        assertThat(summary.getDividends()).isEqualTo(new BigDecimal(20));
    }

    private SecuritySummary newSecuritySummary(int transactionCount, int shares, int costBasis, int dividends) {
        return new SecuritySummary(security, transactionCount, new BigDecimal(shares), new Date(), new BigDecimal(costBasis), new BigDecimal(dividends));
    }
}