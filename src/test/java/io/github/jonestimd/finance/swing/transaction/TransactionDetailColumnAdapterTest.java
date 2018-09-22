package io.github.jonestimd.finance.swing.transaction;

import java.util.Date;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import org.junit.Test;

import static io.github.jonestimd.finance.swing.transaction.TransactionDetailColumnAdapter.*;
import static org.assertj.core.api.Assertions.*;

public class TransactionDetailColumnAdapterTest {
    @Test
    public void transactionDate() throws Exception {
        Date date = new Date();
        TransactionDetail detail = new TransactionDetailBuilder().onTransaction(date).get();

        assertThat(TRANSACTION_DATE_ADAPTER.getValue(detail)).isSameAs(date);
    }

    @Test
    public void transactionPayee() throws Exception {
        Payee payee = new Payee("the payee");
        TransactionDetail detail = new TransactionDetailBuilder().get();
        new TransactionBuilder().payee(payee).details(detail).get();

        assertThat(TRANSACTION_PAYEE_ADAPTER.getValue(detail)).isSameAs(payee);
    }

    @Test
    public void transactionAccount() throws Exception {
        Account account = new Account();
        TransactionDetail detail = new TransactionDetailBuilder().get();
        new TransactionBuilder().account(account).details(detail).get();

        assertThat(TRANSACTION_ACCOUNT_ADAPTER.getValue(detail)).isSameAs(account);
    }

    @Test
    public void transactionSecurity() throws Exception {
        Security security = new Security();
        TransactionDetail detail = new TransactionDetailBuilder().get();
        new TransactionBuilder().security(security).details(detail).get();

        assertThat(TRANSACTION_SECURITY_ADAPTER.getValue(detail)).isSameAs(security);
    }

    @Test
    public void transactionMemo() throws Exception {
        TransactionDetail detail = new TransactionDetailBuilder().get();
        new TransactionBuilder().memo("the memo").details(detail).get();

        assertThat(TRANSACTION_MEMO_ADAPTER.getValue(detail)).isEqualTo("the memo");
    }
}