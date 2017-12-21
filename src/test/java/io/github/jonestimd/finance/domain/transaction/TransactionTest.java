package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TransactionTest {
    @Test
    public void setDateUpdatesTransfers() throws Exception {
        Transaction transaction = new TransactionBuilder()
            .details(new TransactionDetailBuilder().newTransfer())
            .details(new TransactionDetailBuilder().newTransfer()).get();

        Date date = new Date();
        transaction.setDate(date);

        assertThat(transaction.getDetails().get(0).getRelatedDetail().getTransaction().getDate()).isSameAs(date);
        assertThat(transaction.getDetails().get(1).getRelatedDetail().getTransaction().getDate()).isSameAs(date);
    }

    @Test
    public void isUnsavedAndEmpty() throws Exception {
        assertThat(new Transaction(-1L).isUnsavedAndEmpty()).isFalse();
        assertThat(new Transaction().isUnsavedAndEmpty()).isTrue();
        assertThat(new TransactionBuilder().details(new TransactionDetail()).get().isUnsavedAndEmpty()).isTrue();
        assertThat(new TransactionBuilder().details(new TransactionDetail(BigDecimal.ONE, null, null)).get().isUnsavedAndEmpty()).isFalse();
    }

    @Test
    public void isSavedorNonempty() throws Exception {
        assertThat(new Transaction(-1L).isSavedOrNonempty()).isTrue();
        assertThat(new Transaction().isSavedOrNonempty()).isFalse();
        assertThat(new TransactionBuilder().details(new TransactionDetail()).get().isSavedOrNonempty()).isFalse();
        assertThat(new TransactionBuilder().details(new TransactionDetail(BigDecimal.ONE, null, null)).get().isSavedOrNonempty()).isTrue();
    }
}
