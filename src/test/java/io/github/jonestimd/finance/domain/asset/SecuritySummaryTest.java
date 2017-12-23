// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.domain.asset;

import java.math.BigDecimal;
import java.util.Date;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class SecuritySummaryTest {
    private final Security security1 = new SecurityBuilder().nextId().get();
    private final Security security2 = new SecurityBuilder().nextId().get();
    private final Account account1 = new AccountBuilder().nextId().get();
    private final Account account2 = new AccountBuilder().nextId().get();

    @Test
    public void isNotEmpty() throws Exception {
        assertThat(SecuritySummary.isNotEmpty(null)).isFalse();
        assertThat(SecuritySummary.isNotEmpty(new SecuritySummary(security1, 0, BigDecimal.ZERO))).isFalse();
        assertThat(SecuritySummary.isNotEmpty(new SecuritySummary(security1, 0, BigDecimal.ONE))).isTrue();
    }

    @Test
    public void isSameIdsWithoutAccounts() throws Exception {
        SecuritySummary summary1 = new SecuritySummary(security1, 0, null, null);
        SecuritySummary summary2 = new SecuritySummary(security2, 0, null, null);

        verifyEquality(summary1, summary2);
    }

    @Test
    public void isSameIdsWithAccounts() throws Exception {
        SecuritySummary summary1 = new SecuritySummary(security1, 0, null, account1);
        SecuritySummary summary2 = new SecuritySummary(security1, 0, null, account2);
        SecuritySummary summary3 = new SecuritySummary(security2, 0, null, account1);
        SecuritySummary summary4 = new SecuritySummary(security2, 0, null, account2);

        verifyEquality(summary1, summary2, summary3, summary4);
    }

    @Test
    public void updateReplacesNullValues() throws Exception {
        SecuritySummary summary = new SecuritySummary(null, 0, BigDecimal.ZERO);
        SecuritySummary summary2 = new SecuritySummary(null, 2, BigDecimal.TEN, new Date(), BigDecimal.ONE, new BigDecimal(20));

        summary.update(summary2);

        assertThat(summary.getTransactionCount()).isEqualTo(2);
        assertThat(summary.getShares()).isEqualTo(BigDecimal.TEN);
        assertThat(summary.getFirstAcquired()).isEqualTo(summary2.getFirstAcquired());
        assertThat(summary.getCostBasis()).isEqualTo(summary2.getCostBasis());
        assertThat(summary.getDividends()).isEqualTo(summary2.getDividends());
    }

    @Test
    public void updateAddsValues() throws Exception {
        SecuritySummary summary = new SecuritySummary(null, 2, BigDecimal.TEN, new Date(), BigDecimal.ONE, new BigDecimal(20));
        SecuritySummary summary2 = new SecuritySummary(null, 2, BigDecimal.TEN, new Date(), BigDecimal.ONE, new BigDecimal(20));

        summary.update(summary2);

        assertThat(summary.getTransactionCount()).isEqualTo(4);
        assertThat(summary.getShares()).isEqualTo(new BigDecimal(20));
        assertThat(summary.getCostBasis()).isEqualTo(new BigDecimal(2));
        assertThat(summary.getDividends()).isEqualTo(new BigDecimal(40));
    }

    @Test
    public void updateUpdatesFirstAcquired() throws Exception {
        SecuritySummary summary = new SecuritySummary(null, 2, BigDecimal.TEN, new Date(), BigDecimal.ONE, new BigDecimal(20));
        SecuritySummary summary2 = new SecuritySummary(null, 2, BigDecimal.TEN, DateUtils.addDays(new Date(), -1), BigDecimal.ONE, new BigDecimal(20));

        summary.update(summary2);

        assertThat(summary.getFirstAcquired()).isEqualTo(summary2.getFirstAcquired());
    }

    @Test
    public void updateRetainsFirstAcquired() throws Exception {
        SecuritySummary summary = new SecuritySummary(null, 2, BigDecimal.TEN, DateUtils.addDays(new Date(), -2), BigDecimal.ONE, new BigDecimal(20));
        SecuritySummary summary2 = new SecuritySummary(null, 2, BigDecimal.TEN, DateUtils.addDays(new Date(), -1), BigDecimal.ONE, new BigDecimal(20));

        summary.update(summary2);

        assertThat(summary.getFirstAcquired()).isEqualTo(summary.getFirstAcquired());
    }

    private void verifyEquality(SecuritySummary ...summaries) {
        for (SecuritySummary summary1 : summaries) {
            for (SecuritySummary summary2 : summaries) {
                if (summary1 != summary2) {
                    assertThat(summary1.isSameIds(summary2)).isEqualTo(summary1 == summary2);
                }
            }
        }
    }
}