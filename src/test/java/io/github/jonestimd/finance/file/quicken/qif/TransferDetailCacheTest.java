package io.github.jonestimd.finance.file.quicken.qif;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransferDetailCacheTest {
    private TransferDetailCache transferDetailCache = new TransferDetailCache();

    @Test
    public void testRemoveWithoutGroup() throws Exception {
        Account account1 = TestDomainUtils.createAccount("account1");
        Account account2 = TestDomainUtils.createAccount("account2");
        TransactionDetail detail1 = createTransfer(account1, account2, "123.45", null);
        transferDetailCache.add(detail1);
        TransactionDetail detail2 = createTransfer(account1, account2, "543.21", null);
        transferDetailCache.add(detail2);

        assertSame(detail1, transferDetailCache.remove(
                account1, account2.getName(), detail1.getTransaction().getDate(), detail1.getAmount(), null));
        assertSame(detail2, transferDetailCache.remove(
                account1, account2.getName(), detail2.getTransaction().getDate(), detail2.getAmount(), null));

        assertEquals(0, transferDetailCache.getPendingTransferDetails().size());
    }

    @Test
    public void testRemoveWithGroup() throws Exception {
        TransactionGroup group = TestDomainUtils.createTransactionGroup("group");
        Account account1 = TestDomainUtils.createAccount("account1");
        Account account2 = TestDomainUtils.createAccount("account2");
        TransactionDetail detail1 = createTransfer(account1, account2, "123.45", null);
        transferDetailCache.add(detail1);
        TransactionDetail detail2 = createTransfer(account1, account2, "123.45", group);
        transferDetailCache.add(detail2);

        assertSame(detail1, transferDetailCache.remove(
                account1, account2.getName(), detail1.getTransaction().getDate(), detail1.getAmount(), null));
        assertSame(detail2, transferDetailCache.remove(
                account1, account2.getName(), detail2.getTransaction().getDate(), detail2.getAmount(), group));

        assertEquals(0, transferDetailCache.getPendingTransferDetails().size());
    }

    @Test
    public void testAddDetailDoesNotReplaceDuplicate() throws Exception {
        Account account1 = TestDomainUtils.createAccount("account1");
        Account account2 = TestDomainUtils.createAccount("account2");
        TransactionDetail detail1 = createTransfer(account1, account2, "123.45", null);
        TransactionDetail detail2 = createTransfer(account1, account2, "123.45", null);

        transferDetailCache.add(detail1);
        transferDetailCache.add(detail2);

        assertEquals(2, transferDetailCache.getPendingTransferDetails().size());
    }

    @Test
    public void testGetPendingTransferDetails() throws Exception {
        Account account1 = TestDomainUtils.createAccount("account1");
        Account account2 = TestDomainUtils.createAccount("account2");
        TransactionDetail detail1 = createTransfer(account1, account2, "123.45", null);
        TransactionDetail detail2 = createTransfer(account1, account2, "123.45", null);
        transferDetailCache.add(detail1);
        transferDetailCache.add(detail2);

        List<TransactionDetail> pendingTransferDetails = transferDetailCache.getPendingTransferDetails();

        assertEquals(2, pendingTransferDetails.size());
        assertTrue(pendingTransferDetails.contains(detail1));
        assertTrue(pendingTransferDetails.contains(detail2));
        pendingTransferDetails.clear();
        assertEquals(2, transferDetailCache.getPendingTransferDetails().size());
    }

    private TransactionDetail createTransfer(Account account1, Account account2, String amount, TransactionGroup group) {
        TransactionDetail detail = new TransactionDetail(new BigDecimal(amount), null, group);
        detail.getRelatedDetail().setTransaction(createTransaction(account2));
        Transaction transaction = createTransaction(account1);
        transaction.addDetails(detail);
        return detail;
    }

    private Transaction createTransaction(Account account) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setDate(new Date());
        return transaction;
    }
}