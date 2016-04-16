package io.github.jonestimd.finance.file.quicken.qif;

import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SecurityTransactionConverterTest extends QifTestFixture {
    private SecurityTransactionConverter converter;
    private AccountHolder accountHolder;
    private Account currentAccount;

    protected AccountType getAccountType() {
        return AccountType.BROKERAGE;
    }

    protected void initializeAccountHolder() throws Exception {
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
        converter = getQifContext().getSecurityTransactionConverter();
        currentAccount = TestDomainUtils.createAccount("account", AccountType.BROKERAGE);
        accountHolder = new AccountHolder();
        accountHolder.setAccount(currentAccount);
    }

    @Test
    public void getTypesIncludesInvestmentTypes() throws Exception {
        assertTrue(new SecurityTransactionConverter(null, null).getTypes().contains("Type:Invst"));
    }

    @Test
    public void unknownActionThrowsException() throws Exception {
        try {
            QifRecord record = new QifRecord(1L);
            record.setValue(QifField.SECURITY_ACTION, "unknown");
            initializeAccountHolder();

            converter.importRecord(accountHolder, record);

            fail("expected exception");
        } catch (QuickenException ex) {
            assertEquals("io.github.jonestimd.finance.file.quicken.unknownSecurityAction", ex.getMessageKey());
            assertArrayEquals(new Object[] {"unknown", 1L}, ex.getMessageArgs());
        }
    }

    @Test
    public void convertUsesHandlerMap() throws Exception {
        QifRecord record1 = createQifRecord("XIn", 100.0, null, null);
        QifRecord record2 = createQifRecord("Buy", 100.0, "security name", 10.0);
        Security security = addSecurity(record2);
        record1.setValue(QifField.TRANSFER_ACCOUNT, "[transfer account]");
        when(accountOperations.getAccount(null, "transfer account"))
            .thenReturn(TestDomainUtils.createAccount("transfer account", AccountType.BANK));
        initializeAccountHolder();

        converter.importRecord(accountHolder, record1);
        converter.importRecord(accountHolder, record2);

        ArgumentCaptor<List<Transaction>> saveCapture = captureSaveTransactions(2);
        assertSame(currentAccount, accountHolder.getAccount());
        assertEquals(2, saveCapture.getAllValues().size());
        assertNull(saveCapture.getAllValues().get(0).get(0).getSecurity());
        assertSame(security, saveCapture.getAllValues().get(1).get(0).getSecurity());
    }

    @SuppressWarnings("deprecation")
    private ArgumentCaptor<List<Transaction>> captureSaveTransactions(int count) {
        ArgumentCaptor<List<Transaction>> saveCapture = new ArgumentCaptor<List<Transaction>>();
        verify(transactionService, times(count)).saveTransactions(saveCapture.capture());
        return saveCapture;
    }

    private QifRecord createQifRecord(String action, Double amount, String security, Double shares) {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.SECURITY_ACTION, action);
        record.setValue(QifField.AMOUNT, amount.toString());
        if (security != null) {
            record.setValue(QifField.SECURITY, security);
        }
        if (shares != null) {
            record.setValue(QifField.SHARES, shares.toString());
        }
        return record;
    }

    private QifRecord createQifRecord(String action, double amount, String security, String memo, String category) {
        QifRecord record = createQifRecord(action, amount, security, null);
        record.setValue(QifField.MEMO, memo);
        record.setValue(QifField.CATEGORY, category);
        return record;
    }

    @Test
    public void convertMiscInc() throws Exception {
        QifRecord record = createQifRecord("MiscInc", 10.00, "security name", "memo", "category");
        Security security = addSecurity(record);
        TransactionCategory TransactionCategory = addTransactionCategory("description", true, "category");
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        ArgumentCaptor<List<Transaction>> saveCapture = captureSaveTransactions(1);
        assertSame(currentAccount, accountHolder.getAccount());
        List<Transaction> savedTransactions = saveCapture.getValue();
        assertEquals(1, savedTransactions.size());
        Transaction transaction = savedTransactions.get(0);
        assertSame(security, transaction.getSecurity());
        assertEquals("memo", transaction.getMemo());
        assertEquals(1, transaction.getDetails().size());
        TransactionDetail detail = transaction.getDetails().get(0);
        assertSame(TransactionCategory, detail.getCategory());
        assertEquals(10.0d, detail.getAmount().doubleValue(), 0d);
    }

    @Test
    public void convertMiscExp() throws Exception {
        QifRecord record = createQifRecord("MiscExp", 10.00, "security name", "memo", "category");
        Security security = addSecurity(record);
        TransactionCategory TransactionCategory = addTransactionCategory("description", true, "category");
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        ArgumentCaptor<List<Transaction>> saveCapture = captureSaveTransactions(1);
        assertSame(currentAccount, accountHolder.getAccount());
        List<Transaction> savedTransactions = saveCapture.getValue();
        assertEquals(1, savedTransactions.size());
        Transaction transaction = savedTransactions.get(0);
        assertSame(security, transaction.getSecurity());
        assertEquals("memo", transaction.getMemo());
        assertEquals(1, transaction.getDetails().size());
        TransactionDetail detail = transaction.getDetails().get(0);
        assertSame(TransactionCategory, detail.getCategory());
        assertEquals(-10.0d, detail.getAmount().doubleValue(), 0d);
    }
}