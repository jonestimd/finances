package io.github.jonestimd.finance.domain.transaction;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class CategoryKeyTest {
    @Test
    public void equalsMatchesCodes() throws Exception {
        TransactionCategory p1 = new TransactionCategory("parent");
        TransactionCategory p2 = new TransactionCategory("parent");
        TransactionCategory c1 = new TransactionCategory(p1, "child");
        TransactionCategory c2 = new TransactionCategory(p2, "child");

        assertThat(c1.equals(c2)).isTrue();
    }
}