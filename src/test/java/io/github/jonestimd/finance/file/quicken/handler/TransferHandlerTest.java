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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TransferHandlerTest extends HandlerTestFixture<TransferHandler> {
    private Map<String, Account> transferAccounts = new HashMap<>();
    private Map<String, TransactionGroup> transactionGroups = new HashMap<>();

    protected AccountType getAccountType() {
        return AccountType.BROKERAGE;
    }

    @Test
    public void convertRecordThrowsExceptionForInvalidDate() throws Exception {
        String qifDate = "12/23/ab";
        QifRecord record = createTransferRecord(qifDate, "payee/description", "123.45", false, "memo", "account", "memo");
        when(payeeOperations.getAllPayees()).thenReturn(Collections.emptyList());

        try {
            getHandler("XIn").convertRecord(account, record);
            fail("expected an exception");
        }
        catch (QuickenException ex) {
            assertThat(ex.getMessageKey()).isEqualTo("import.qif.invalidDate");
            assertThat(ex.getMessageArgs()).isEqualTo(new Object[]{qifDate, 1L});
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
        when(payeeOperations.getAllPayees()).thenReturn(Collections.emptyList());

        List<Transaction> transactions = getHandler("XIn").convertRecord(account, record);

        Transaction relatedTransaction = transactions.get(0);
        Transaction importTransaction = transactions.get(1);
        assertThat(relatedTransaction.getNumber()).isNull();
        assertThat(importTransaction.getNumber()).isNull();
        assertThat(relatedTransaction.getPayee()).isSameAs(payee);
        assertThat(importTransaction.getPayee()).isSameAs(payee);
        assertThat(relatedTransaction.getAccount()).isSameAs(transferAccount);
        assertThat(importTransaction.getAccount()).isSameAs(account);
        assertThat(relatedTransaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(importTransaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(relatedTransaction.getAmount()).isEqualTo(amount.negate());
        assertThat(importTransaction.getAmount()).isEqualTo(amount);
        assertThat(relatedTransaction.isCleared()).isTrue();
        assertThat(importTransaction.isCleared()).isTrue();
        assertThat(relatedTransaction.getNumber()).isNull();
        assertThat(importTransaction.getNumber()).isNull();

        assertThat(relatedTransaction.getDetails()).hasSize(1);
        assertThat(importTransaction.getDetails()).hasSize(1);
        TransactionDetail relatedDetail = relatedTransaction.getDetails().get(0);
        TransactionDetail importDetail = importTransaction.getDetails().get(0);
        assertThat(relatedDetail.isTransfer()).isTrue();
        assertThat(importDetail.isTransfer()).isTrue();
        assertThat(relatedDetail.getAmount()).isEqualTo(amount.negate());
        assertThat(importDetail.getAmount()).isEqualTo(amount);
        assertThat(relatedDetail.getGroup()).isSameAs(group);
        assertThat(importDetail.getGroup()).isSameAs(group);
        assertThat(relatedDetail.getMemo()).isEqualTo(memo);
        assertThat(importDetail.getMemo()).isEqualTo(memo);
        assertThat(importDetail).isSameAs(relatedDetail.getRelatedDetail());
        assertThat(relatedDetail).isSameAs(importDetail.getRelatedDetail());

        assertThat(pendingTransferDetails.remove(transferAccount, account.getName(), importTransaction.getDate(), amount.negate(), group)).isSameAs(relatedDetail);
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
        when(payeeOperations.getAllPayees()).thenReturn(Collections.emptyList());

        List<Transaction> transactions = getHandler("XIn").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getNumber()).isNull();
        assertThat(transaction.getPayee()).isSameAs(payee);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction.getAmount()).isEqualTo(amount);
        assertThat(transaction.isCleared()).isTrue();

        assertThat(transaction.getDetails()).hasSize(1);
        TransactionDetail detail = transaction.getDetails().get(0);
        assertThat(detail.isTransfer()).isFalse();
        assertThat(detail.getCategory()).isNull();
        assertThat(detail.getAmount()).isEqualTo(amount);
        assertThat(detail.getGroup()).isSameAs(group);
        assertThat(detail.getMemo()).isEqualTo(memo);
        assertThat(detail.getRelatedDetail()).isNull();
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
        when(payeeOperations.getAllPayees()).thenReturn(Collections.emptyList());

        List<Transaction> transactions = getHandler("XOut").convertRecord(account, record);

        Transaction relatedTransaction = transactions.get(0);
        Transaction importTransaction = transactions.get(1);
        assertThat(relatedTransaction.getNumber()).isNull();
        assertThat(importTransaction.getNumber()).isNull();
        assertThat(relatedTransaction.getPayee()).isSameAs(payee);
        assertThat(importTransaction.getPayee()).isSameAs(payee);
        assertThat(relatedTransaction.getAccount()).isSameAs(transferAccount);
        assertThat(importTransaction.getAccount()).isSameAs(account);
        assertThat(relatedTransaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(importTransaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(relatedTransaction.getAmount()).isEqualTo(amount);
        assertThat(importTransaction.getAmount()).isEqualTo(amount.negate());
        assertThat(relatedTransaction.isCleared()).isTrue();
        assertThat(importTransaction.isCleared()).isTrue();
        assertThat(relatedTransaction.getNumber()).isNull();
        assertThat(importTransaction.getNumber()).isNull();

        assertThat(relatedTransaction.getDetails()).hasSize(1);
        assertThat(importTransaction.getDetails()).hasSize(1);
        TransactionDetail relatedDetail = relatedTransaction.getDetails().get(0);
        TransactionDetail importDetail = importTransaction.getDetails().get(0);
        assertThat(relatedDetail.isTransfer()).isTrue();
        assertThat(importDetail.isTransfer()).isTrue();
        assertThat(relatedDetail.getAmount()).isEqualTo(amount);
        assertThat(importDetail.getAmount()).isEqualTo(amount.negate());
        assertThat(relatedDetail.getGroup()).isSameAs(group);
        assertThat(importDetail.getGroup()).isSameAs(group);
        assertThat(relatedDetail.getMemo()).isEqualTo(memo);
        assertThat(importDetail.getMemo()).isEqualTo(memo);
        assertThat(importDetail).isSameAs(relatedDetail.getRelatedDetail());
        assertThat(relatedDetail).isSameAs(importDetail.getRelatedDetail());

        assertThat(pendingTransferDetails.remove(transferAccount, account.getName(), importTransaction.getDate(), amount, group)).isSameAs(relatedDetail);
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
        when(payeeOperations.getAllPayees()).thenReturn(Collections.emptyList());

        List<Transaction> transactions = getHandler("XOut").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getNumber()).isNull();
        assertThat(transaction.getPayee()).isSameAs(payee);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction.getAmount()).isEqualTo(amount.negate());
        assertThat(transaction.isCleared()).isTrue();

        assertThat(transaction.getDetails()).hasSize(1);
        TransactionDetail detail = transaction.getDetails().get(0);
        assertThat(detail.isTransfer()).isFalse();
        assertThat(detail.getCategory()).isNull();
        assertThat(detail.getAmount()).isEqualTo(amount.negate());
        assertThat(detail.getGroup()).isSameAs(group);
        assertThat(detail.getMemo()).isEqualTo(memo);
        assertThat(detail.getRelatedDetail()).isNull();
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

    @Test
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
        when(payeeOperations.getAllPayees()).thenReturn(Collections.emptyList());

        List<Transaction> transactions = getHandler("XIn").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction savedTransaction = transactions.get(0);
        assertThat(savedTransaction).isSameAs(transferDetail.getTransaction());
        assertThat(savedTransaction.getDetails()).hasSize(1);
        assertThat(savedTransaction.getDetails().get(0)).isSameAs(transferDetail);
        assertThat(savedTransaction.getNumber()).isEqualTo(null);
        assertThat(savedTransaction.getPayee()).isSameAs(payee);
        assertThat(savedTransaction.isCleared()).isTrue();
        assertThat(savedTransaction.getMemo()).isEqualTo(memo);
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