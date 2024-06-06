package io.github.jonestimd.finance.swing.transaction;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TransactionDetailTableModelTest {
    @Test
    public void columnAdapters() throws Exception {
        TransactionDetailTableModel model = new TransactionDetailTableModel();

        assertThat(model.getColumnCount()).isEqualTo(10);
        assertThat(model.getColumnIdentifier(0)).isSameAs(TransactionDetailColumnAdapter.TRANSACTION_DATE_ADAPTER);
        assertThat(model.getColumnIdentifier(1)).isSameAs(TransactionDetailColumnAdapter.TRANSACTION_ACCOUNT_ADAPTER);
        assertThat(model.getColumnIdentifier(2)).isSameAs(TransactionDetailColumnAdapter.TRANSACTION_PAYEE_ADAPTER);
        assertThat(model.getColumnIdentifier(3)).isSameAs(TransactionDetailColumnAdapter.GROUP_ADAPTER);
        assertThat(model.getColumnIdentifier(4)).isSameAs(ValidatedDetailColumnAdapter.TYPE_ADAPTER);
        assertThat(model.getColumnIdentifier(5)).isSameAs(TransactionDetailColumnAdapter.TRANSACTION_MEMO_ADAPTER);
        assertThat(model.getColumnIdentifier(6)).isSameAs(TransactionDetailColumnAdapter.MEMO_ADAPTER);
        assertThat(model.getColumnIdentifier(7)).isSameAs(TransactionDetailColumnAdapter.TRANSACTION_SECURITY_ADAPTER);
        assertThat(model.getColumnIdentifier(8)).isSameAs(ValidatedDetailColumnAdapter.SHARES_ADAPTER);
        assertThat(model.getColumnIdentifier(9)).isSameAs(ValidatedDetailColumnAdapter.AMOUNT_ADAPTER);
    }
}