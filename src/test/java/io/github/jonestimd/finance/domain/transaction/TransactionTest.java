package io.github.jonestimd.finance.domain.transaction;

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
}
