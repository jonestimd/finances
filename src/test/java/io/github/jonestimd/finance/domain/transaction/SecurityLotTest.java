package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.asset.SplitRatio;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class SecurityLotTest {
    private final Date splitDate = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -1);

    @Test
    public void getPurchasePriceAdjustsForSplits() throws Exception {
        Security security = new Security("stock", SecurityType.STOCK);
        security.setSplits(Lists.newArrayList(new StockSplit(security, splitDate, new SplitRatio(BigDecimal.ONE, BigDecimal.valueOf(2L)))));
        TransactionDetail purchase = new TransactionDetailBuilder().amount(BigDecimal.TEN.negate())
                .shares(BigDecimal.ONE).get();
        new TransactionBuilder().date(DateUtils.addDays(splitDate, -1))
                .account(new Account(new Currency()))
                .security(security)
                .details(purchase).get();
        TransactionDetail sale = new TransactionDetailBuilder().amount(BigDecimal.TEN)
                .shares(BigDecimal.ONE)
                .onTransaction(DateUtils.addDays(splitDate, 1)).get();
        SecurityLot lot = new SecurityLot(purchase, sale, BigDecimal.ONE);

        assertThat(lot.getPurchasePrice()).isEqualTo(BigDecimal.valueOf(5L));
    }
}