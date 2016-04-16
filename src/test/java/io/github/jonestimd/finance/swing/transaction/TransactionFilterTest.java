package io.github.jonestimd.finance.swing.transaction;

import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class TransactionFilterTest {
    @Test
    public void trueForEmptyFilterField() throws Exception {
        assertThat(new TransactionFilter("").test(null)).isTrue();
    }

    @Test
    public void matchesUnsavedTransaction() throws Exception {
        assertThat(new TransactionFilter("x").test(new Transaction())).isTrue();
    }

    @Test
    public void matchesTransactionMemo() throws Exception {
        TransactionFilter filter = new TransactionFilter("tx memo");

        assertThat(filter.test(new TransactionBuilder().nextId().memo("tx memo 1").get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().memo("tx MEMO").get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().memo("ty MEMO").get())).isFalse();
    }

    @Test
    public void matchesPayeeName() throws Exception {
        TransactionFilter filter = new TransactionFilter("payee");

        assertThat(filter.test(new TransactionBuilder().nextId().payee(new Payee("payee abc")).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().payee(new Payee("def PAYEE")).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().payee(new Payee("pay")).get())).isFalse();
    }

    @Test
    public void matchesTransactionAmount() throws Exception {
        TransactionFilter filter = new TransactionFilter("1.00");

        assertThat(filter.test(new TransactionBuilder().nextId().details(
                new TransactionDetail(null, new BigDecimal("10.20"), null, null),
                new TransactionDetail(null, new BigDecimal(".80"), null, null)).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().details(new TransactionDetail(null, BigDecimal.ZERO, null, null)).get())).isFalse();
    }

    @Test
    public void matchesDetailMemeo() throws Exception {
        TransactionFilter filter = new TransactionFilter("memo");

        assertThat(filter.test(new TransactionBuilder().nextId().details(new TransactionDetail(null, BigDecimal.ONE, "memo abc", null)).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().details(new TransactionDetail(null, BigDecimal.ONE, "DEF MEMO", null)).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().details(new TransactionDetail(null, BigDecimal.ONE, "abc def", null)).get())).isFalse();
    }

    @Test
    public void matchesCategory() throws Exception {
        TransactionFilter filter = new TransactionFilter("Tax");

        assertThat(filter.test(new TransactionBuilder().nextId().details(new TransactionDetail(new TransactionCategory("fed tax"), BigDecimal.ONE, null, null)).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().details(new TransactionDetail(new TransactionCategory("TAX state"), BigDecimal.ONE, null, null)).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().details(new TransactionDetail(new TransactionCategory("state"), BigDecimal.ONE, null, null)).get())).isFalse();
    }

    @Test
    public void matchesSecurityName() throws Exception {
        TransactionFilter filter = new TransactionFilter("stock");

        assertThat(filter.test(new TransactionBuilder().nextId().security(new Security("abc stock", SecurityType.STOCK)).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().security(new Security("STOCK XYZ", SecurityType.STOCK)).get())).isTrue();
        assertThat(filter.test(new TransactionBuilder().nextId().security(new Security("abc XYZ", SecurityType.STOCK)).get())).isFalse();
    }

    @Test
    public void matchesDetailAmount() throws Exception {
        TransactionFilter filter = new TransactionFilter("1.00");

        assertThat(filter.test(new TransactionBuilder().nextId().details(
                new TransactionDetail(null, new BigDecimal("1.00"), null, null),
                new TransactionDetail(null, new BigDecimal(".80"), null, null)).get())).isTrue();
    }
}