package io.github.jonestimd.finance.file.quicken.handler;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.qif.CategoryParser;
import io.github.jonestimd.finance.file.quicken.qif.QifField;
import io.github.jonestimd.finance.file.quicken.qif.QifRecord;
import org.junit.Test;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransferHandlerTest extends HandlerTestFixture<TransferHandler> {
    private Map<String, Account> transferAccounts = new HashMap<String, Account>();
    private Map<String, TransactionGroup> transactionGroups = new HashMap<String, TransactionGroup>();

    protected AccountType getAccountType() {
        return AccountType.BROKERAGE;
    }

    @Test
    public void convertRecordThrowsExceptionForInvalidDate() throws Exception {
        String qifDate = "12/23/ab";
        QifRecord record = createTransferRecord(qifDate, "payee/description", "123.45", false, "memo", "account", "memo");
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());

        try {
            getHandler("XIn").convertRecord(account, record);
            fail();
        }
        catch (QuickenException ex) {
            assertEquals("io.github.jonestimd.finance.file.quicken.invalidDate", ex.getMessageKey());
            assertArrayEquals(new Object[]{qifDate, 1L}, ex.getMessageArgs());
        }
    }

    @Test
    public void convertXIn() throws Exception {
        String date = "12/8/98";
        String accountName = "transfer account";
        String groupName = "group";
        String memo = "memo";
        String description = "description";
        BigDecimal amount = new BigDecimal("123.45");
        QifRecord record = createTransferRecord(date, description, amount.toString(), true, memo, accountName, groupName);
        record.setValue(QifField.SECURITY_ACTION, "XIn");
        Payee payee = addPayee(record);
        Account transferAccount = addAccount(accountName, AccountType.BANK);
        TransactionGroup group = addTransactionGroup(record);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());

        List<? extends Transaction> transactions = getHandler("XIn").convertRecord(account, record);

        Transaction relatedTransaction = transactions.get(0);
        Transaction importTransaction = transactions.get(1);
        assertNull(relatedTransaction.getNumber());
        assertNull(importTransaction.getNumber());
        assertSame(payee, relatedTransaction.getPayee());
        assertSame(payee, importTransaction.getPayee());
        assertSame(transferAccount, relatedTransaction.getAccount());
        assertSame(account, importTransaction.getAccount());
        assertEquals(dateFormat.parse(date), relatedTransaction.getDate());
        assertEquals(dateFormat.parse(date), importTransaction.getDate());
        assertEquals(amount.negate(), relatedTransaction.getAmount());
        assertEquals(amount, importTransaction.getAmount());
        assertTrue(relatedTransaction.isCleared());
        assertTrue(importTransaction.isCleared());
        assertNull(relatedTransaction.getNumber());
        assertNull(importTransaction.getNumber());

        assertEquals(1, relatedTransaction.getDetails().size());
        assertEquals(1, importTransaction.getDetails().size());
        TransactionDetail relatedDetail = relatedTransaction.getDetails().get(0);
        TransactionDetail importDetail = importTransaction.getDetails().get(0);
        assertTrue(relatedDetail.isTransfer());
        assertTrue(importDetail.isTransfer());
        assertEquals(amount.negate(), relatedDetail.getAmount());
        assertEquals(amount, importDetail.getAmount());
        assertSame(group, relatedDetail.getGroup());
        assertSame(group, importDetail.getGroup());
        assertEquals(memo, relatedDetail.getMemo());
        assertEquals(memo, importDetail.getMemo());
        assertSame(relatedDetail.getRelatedDetail(), importDetail);
        assertSame(importDetail.getRelatedDetail(), relatedDetail);

        assertSame(relatedDetail, pendingTransferDetails.remove(transferAccount, account.getName(), importTransaction.getDate(), amount.negate(), group));
    }

    @Test
    public void convertXInFromSameAccount() throws Exception {
        String date = "12/8/98";
        String groupName = "group";
        String memo = "memo";
        String description = "description";
        BigDecimal amount = new BigDecimal("123.45");
        QifRecord record = createTransferRecord(date, description, amount.toString(), true, memo, account.getName(), groupName);
        Payee payee = addPayee(record);
        TransactionGroup group = addTransactionGroup(record);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());

        List<? extends Transaction> transactions = getHandler("XIn").convertRecord(account, record);

        assertEquals(1, transactions.size());
        Transaction transaction = transactions.get(0);
        assertNull(transaction.getNumber());
        assertSame(payee, transaction.getPayee());
        assertSame(account, transaction.getAccount());
        assertEquals(dateFormat.parse(date), transaction.getDate());
        assertEquals(amount, transaction.getAmount());
        assertTrue(transaction.isCleared());

        assertEquals(1, transaction.getDetails().size());
        TransactionDetail detail = transaction.getDetails().get(0);
        assertFalse(detail.isTransfer());
        assertNull(detail.getCategory());
        assertEquals(amount, detail.getAmount());
        assertSame(group, detail.getGroup());
        assertEquals(memo, detail.getMemo());
        assertNull(detail.getRelatedDetail());
    }

    @Test
    public void convertXOut() throws Exception {
        String date = "12/8/98";
        String accountName = "transfer account";
        String groupName = "group";
        String memo = "memo";
        String description = "description";
        BigDecimal amount = new BigDecimal("123.45");
        QifRecord record = createTransferRecord(date, description, amount.toString(), true, memo, accountName, groupName);
        record.setValue(QifField.SECURITY_ACTION, "XOut");
        Payee payee = addPayee(record);
        Account transferAccount = addAccount(accountName, AccountType.BANK);
        TransactionGroup group = addTransactionGroup(record);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());

        List<? extends Transaction> transactions = getHandler("XOut").convertRecord(account, record);

        Transaction relatedTransaction = transactions.get(0);
        Transaction importTransaction = transactions.get(1);
        assertNull(relatedTransaction.getNumber());
        assertNull(importTransaction.getNumber());
        assertSame(payee, relatedTransaction.getPayee());
        assertSame(payee, importTransaction.getPayee());
        assertSame(transferAccount, relatedTransaction.getAccount());
        assertSame(account, importTransaction.getAccount());
        assertEquals(dateFormat.parse(date), relatedTransaction.getDate());
        assertEquals(dateFormat.parse(date), importTransaction.getDate());
        assertEquals(amount, relatedTransaction.getAmount());
        assertEquals(amount.negate(), importTransaction.getAmount());
        assertTrue(relatedTransaction.isCleared());
        assertTrue(importTransaction.isCleared());
        assertNull(relatedTransaction.getNumber());
        assertNull(importTransaction.getNumber());

        assertEquals(1, relatedTransaction.getDetails().size());
        assertEquals(1, importTransaction.getDetails().size());
        TransactionDetail relatedDetail = relatedTransaction.getDetails().get(0);
        TransactionDetail importDetail = importTransaction.getDetails().get(0);
        assertTrue(relatedDetail.isTransfer());
        assertTrue(importDetail.isTransfer());
        assertEquals(amount, relatedDetail.getAmount());
        assertEquals(amount.negate(), importDetail.getAmount());
        assertSame(group, relatedDetail.getGroup());
        assertSame(group, importDetail.getGroup());
        assertEquals(memo, relatedDetail.getMemo());
        assertEquals(memo, importDetail.getMemo());
        assertSame(relatedDetail.getRelatedDetail(), importDetail);
        assertSame(importDetail.getRelatedDetail(), relatedDetail);

        assertSame(relatedDetail, pendingTransferDetails.remove(transferAccount, account.getName(), importTransaction.getDate(), amount, group));
    }

    @Test
    public void convertXOutSameAccount() throws Exception {
        String date = "12/8/98";
        String groupName = "group";
        String memo = "memo";
        String description = "description";
        BigDecimal amount = new BigDecimal("123.45");
        QifRecord record = createTransferRecord(date, description, amount.toString(), true, memo, account.getName(), groupName);
        Payee payee = addPayee(record);
        TransactionGroup group = addTransactionGroup(record);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());

        List<? extends Transaction> transactions = getHandler("XOut").convertRecord(account, record);

        assertEquals(1, transactions.size());
        Transaction transaction = transactions.get(0);
        assertNull(transaction.getNumber());
        assertSame(payee, transaction.getPayee());
        assertSame(account, transaction.getAccount());
        assertEquals(dateFormat.parse(date), transaction.getDate());
        assertEquals(amount.negate(), transaction.getAmount());
        assertTrue(transaction.isCleared());

        assertEquals(1, transaction.getDetails().size());
        TransactionDetail detail = transaction.getDetails().get(0);
        assertFalse(detail.isTransfer());
        assertNull(detail.getCategory());
        assertEquals(amount.negate(), detail.getAmount());
        assertSame(group, detail.getGroup());
        assertEquals(memo, detail.getMemo());
        assertNull(detail.getRelatedDetail());
    }

    private Account addAccount(String accountName, AccountType type) throws Exception {
        Account transferAccount = transferAccounts.get(accountName);
        if (transferAccount == null) {
            transferAccount = TestDomainUtils.createAccount(accountName, type);
            transferAccounts.put(accountName, transferAccount);
            when(accountOperations.getAccount(null, accountName)).thenReturn(transferAccount);
        }
        return transferAccount;
    }

    public void convertXInUpdatesOriginalTransfer() throws Exception {
        String date = "12/8/98";
        String accountName = "transfer account";
        String groupName = "group";
        String memo = "memo";
        BigDecimal amount = new BigDecimal("123.45");
        QifRecord record = createTransferRecord(date, "Payee/description", amount.toString(), true, memo, accountName, groupName);
        record.setValue(QifField.SECURITY_ACTION, "XIn");
        Payee payee = addPayee(record);
        TransactionGroup group = addTransactionGroup(record);
        TransactionDetail transferDetail = addPendingTransfer(group, record);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());

        List<? extends Transaction> transactions = getHandler("XIn").convertRecord(account, record);

        assertEquals(1, transactions.size());
        Transaction savedTransaction = transactions.get(0);
        assertSame(transferDetail.getTransaction(), savedTransaction);
        assertEquals(1, savedTransaction.getDetails().size());
        assertSame(transferDetail, savedTransaction.getDetails().get(0));
        assertEquals(null, savedTransaction.getNumber());
        assertSame(payee, savedTransaction.getPayee());
        assertTrue(savedTransaction.isCleared());
        assertEquals(memo, savedTransaction.getMemo());
    }

    private TransactionGroup addTransactionGroup(QifRecord record) throws Exception {
        String transferAccountName = record.getValue(TRANSFER_ACCOUNT);
        TransactionGroup group = null;
        String name = new CategoryParser(transferAccountName).getGroupName();
        if (name != null) {
            group = transactionGroups.get(name);
            if (group == null) {
                group = TestDomainUtils.createTransactionGroup(name);
                when(transactionGroupOperations.getTransactionGroup(name)).thenReturn(group);
            }
        }
        return group;
    }

    private QifRecord createTransferRecord(String date, String description, String amount, boolean cleared, String memo,
            String accountName, String group) {
        QifRecord record = createRecord(date, description, amount, memo, cleared);
        record.setValue(TRANSFER_ACCOUNT, '[' + accountName + "]/" + group);
        return record;
    }
}