package io.github.jonestimd.finance.file.quicken.qif;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MoneyTransactionConverterTest extends QifTestFixture {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
    private MoneyTransactionConverter converter;
    private AccountHolder accountHolder;
    private ArgumentCaptor<Transaction> saveCapture = ArgumentCaptor.forClass(Transaction.class);

    protected AccountType getAccountType() {
        return AccountType.BANK;
    }

    protected void initializeAccountHolder() throws Exception {
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
        converter = getQifContext().getMoneyTransactionConverter();
        accountHolder = new AccountHolder();
        accountHolder.setAccount(account);
    }

    @Test
    public void getTypesIncludesMoneyTypes() throws Exception {
        initializeAccountHolder();
        assertTrue(converter.getTypes().contains("Type:Bank"));
        assertTrue(converter.getTypes().contains("Type:Cash"));
        assertTrue(converter.getTypes().contains("Type:CCard"));
        assertTrue(converter.getTypes().contains("Type:Oth L"));
    }

    private QifRecord createRecord(String date, String amount, boolean cleared, String payeeName, String category, String memo) {
        QifRecord record = new QifRecord(1L);
        record.setValue(DATE, date);
        record.setValue(AMOUNT, amount);
        if (cleared) {
            record.setValue(CLEARED, "X");
        }
        record.setValue(PAYEE, payeeName);
        if (category != null) record.setValue(CATEGORY, category);
        record.setValue(MEMO, memo);
        return record;
    }

    @Test
    public void importRecordParsesCategoryAndClass() throws Exception {
        String date = "12/8/98";
        String checkNumber = "321";
        String amount = "123.45";
        String category = "category:subcategory/group";
        String memo = "memo";
        QifRecord record = createRecord(date, amount, true, "Payee", category, memo);
        record.setValue(NUMBER, checkNumber);
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        TransactionCategory TransactionCategory = addTransactionCategorys(record).get(0);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        assertSame(account, accountHolder.getAccount());
        verify(transactionService).saveTransaction(saveCapture.capture());
        Transaction transaction = saveCapture.getValue();
        assertSame(payee, transaction.getPayee());
        assertSame(account, transaction.getAccount());
        assertEquals(dateFormat.parse(date), transaction.getDate());
        assertEquals(new BigDecimal(amount), transaction.getAmount());
        assertTrue(transaction.isCleared());
        assertEquals(checkNumber, transaction.getNumber());

        List<TransactionDetail> details = transaction.getDetails();
        assertEquals(1, details.size());
        TransactionDetail detail = details.get(0);
        assertFalse(detail.isTransfer());
        assertEquals(new BigDecimal(amount), detail.getAmount());
        assertSame(TransactionCategory, detail.getCategory());
        assertSame(groups[0], detail.getGroup());
        assertEquals(memo, detail.getMemo());
    }

    @Test
    public void importRecordParsesQuickenDate() throws Exception {
        Date date = new SimpleDateFormat("MM/dd/yyyy").parse("12/8/2001");
        String qifDate = new SimpleDateFormat("MM/dd").format(date) + "' 1";
        QifRecord record = createRecord(qifDate, "123.45", false, "Payee", "category:subcategory", "memo");
        addPayee(record);
        addTransactionCategorys(record);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService).saveTransaction(saveCapture.capture());
        Transaction transaction = saveCapture.getValue();
        assertEquals(date, transaction.getDate());
        assertFalse(transaction.isCleared());
    }

    @Test
    public void importRecordThrowsExceptionForInvalidDate() throws Exception {
        String qifDate = "12/23/ab";
        QifRecord record = createRecord(qifDate, "123.45", false, "Payee", "category:subcategory", "memo");
        addPayee(record);
        initializeAccountHolder();

        try {
            converter.importRecord(accountHolder, record);
            fail();
        }
        catch (QuickenException ex) {
            assertEquals("io.github.jonestimd.finance.file.quicken.invalidDate", ex.getMessageKey());
            assertArrayEquals(new Object[]{qifDate, 1L}, ex.getMessageArgs());
        }
    }

    private QifRecord createSplit(String date, String[] amounts, boolean cleared, String payeeName,
            String[] categories, String[] memos) {
        QifRecord record = createRecord(date, amounts[0], true, payeeName, categories[0] , memos[0]);
        for (int i=1; i<categories.length; i++) {
            record.setValue(SPLIT_CATEGORY, categories[i]);
            if (memos[i] != null) record.setValue(SPLIT_MEMO, memos[i]);
            record.setValue(SPLIT_AMOUNT, amounts[i]);
        }
        return record;
    }

    private List<String> combineValues(QifRecord record, QifField mainCode, QifField splitCode) {
        List<String> values = new ArrayList<String>();
        values.add(record.getValue(mainCode));
        if (record.hasValue(splitCode)) {
            values.addAll(record.getValues(splitCode));
        }
        return values;
    }

    private List<TransactionCategory> addTransactionCategorys(QifRecord record) throws Exception {
        List<String> categories = record.isSplit() ? record.getValues(SPLIT_CATEGORY) : Arrays.asList(record.getValue(CATEGORY));
        List<TransactionCategory> types = new ArrayList<TransactionCategory>();
        for (int i=0; i<categories.size(); i++) {
            CategoryParser parser = new CategoryParser(categories.get(i));
            if (! parser.isTransfer()) {
                if (parser.getCategoryNames() == null) {
                    types.add(null);
                }
                else {
                    TransactionCategory type = new TransactionCategory();
                    when(TransactionCategoryOperations.getTransactionCategory(parser.getCategoryNames())).thenReturn(type);
                    types.add(type);
                }
            }
        }
        return types;
    }

    private List<TransactionDetail> addTransfers(TransactionGroup groups[], QifRecord record) throws Exception {
        List<String> categories = combineValues(record, CATEGORY, SPLIT_CATEGORY);
        List<TransactionDetail> transferDetails = new ArrayList<TransactionDetail>();
        for (int i=1; i<categories.size(); i++) {
            CategoryParser parser = new CategoryParser(categories.get(i));
            if (parser.isTransfer()) {
                transferDetails.add(addPendingTransfer(groups[i], record, i-1));
            }
        }
        return transferDetails;
    }

    private TransactionGroup[] addTransactionGroups(QifRecord record) throws Exception {
        List<String> categories = combineValues(record, CATEGORY, SPLIT_CATEGORY);
        TransactionGroup[] groups = new TransactionGroup[categories.size()];
        for (int i=0; i<categories.size(); i++) {
            String name = new CategoryParser(categories.get(i)).getGroupName();
            if (name != null) {
                groups[i] = TestDomainUtils.createTransactionGroup(name);
                when(transactionGroupOperations.getTransactionGroup(name)).thenReturn(groups[i]);
            }
        }
        return groups;
    }

    @Test
    public void importRecordCreatesDetailsForSplitTransaction() throws Exception {
        String date = "12/8/98";
        String[] categories = { "category:subcategory", "cat1/group", "cat2" };
        String[] memos = { "main memo", "memo1", "memo2" };
        String[] amounts = { "123.45", "120.00", "3.45" };
        QifRecord record = createSplit(date, amounts, true, "Payee", categories , memos);
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionCategory> types = addTransactionCategorys(record);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService).saveTransaction(saveCapture.capture());
        Transaction transaction = saveCapture.getValue();
        assertSame(payee, transaction.getPayee());
        assertSame(account, transaction.getAccount());
        assertEquals(dateFormat.parse(date), transaction.getDate());
        assertEquals(new BigDecimal(amounts[0]), transaction.getAmount());
        assertTrue(transaction.isCleared());
        assertEquals(memos[0], transaction.getMemo());

        List<TransactionDetail> details = transaction.getDetails();
        assertEquals(types.size(), details.size());
        for (int i=0; i<details.size(); i++) {
            TransactionDetail detail = details.get(i);
            assertFalse(detail.isTransfer());
            assertEquals(new BigDecimal(amounts[i+1]), detail.getAmount());
            assertSame(types.get(i), detail.getCategory());
            assertSame(groups[i+1], detail.getGroup());
            assertEquals(memos[i+1], detail.getMemo());
        }
    }

    @Test
    public void importRecordVerifiesSplitTotal() throws Exception {
        String date = "12/8/98";
        String payeeName = "Payee";
        String[] categories = { "category:subcategory", "cat1/group", "cat2" };
        String[] memos = { "main memo", "memo1", "memo2" };
        String[] amounts = { "123.45", "12.00", "3.45" };
        QifRecord record = createSplit(date, amounts, true, payeeName, categories , memos);
        addPayee(record);
        addTransactionCategorys(record);
        addTransactionGroups(record);
        initializeAccountHolder();

        try {
            converter.importRecord(accountHolder, record);
            fail();
        }
        catch (QuickenException ex) {
            verify(transactionService).saveTransaction(any(Transaction.class));
            assertEquals("io.github.jonestimd.finance.file.quicken.invalidSplit", ex.getMessageKey());
            assertArrayEquals(new Object[] {new BigDecimal(amounts[0]), new BigDecimal("15.45"), 1L}, ex.getMessageArgs());
        }
    }

    @Test
    public void importRecordCreatesTransferDetail() throws Exception {
        String date = "12/8/98";
        String category = "[transfer account]/group";
        String memo = "memo";
        String amount = "123.45";
        QifRecord record = createRecord(date, amount, true, "Payee", category, memo);
        Payee payee = addPayee(record);
        Account transferAccount = TestDomainUtils.createAccount("transfer account");
        when(accountOperations.getAccount(null, "transfer account")).thenReturn(transferAccount);
        TransactionGroup group = addTransactionGroups(record)[0];
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        assertSame(account, accountHolder.getAccount());
        verify(transactionService, times(2)).saveTransaction(saveCapture.capture());
        verify(transactionService, times(2)).saveDetail(any(TransactionDetail.class));
        assertEquals(2, saveCapture.getAllValues().size());
        Transaction transaction1 = saveCapture.getAllValues().get(0);
        Transaction transaction2 = saveCapture.getAllValues().get(1);
        assertSame(payee, transaction1.getPayee());
        assertSame(payee, transaction2.getPayee());
        assertSame(account, transaction1.getAccount());
        assertSame(transferAccount, transaction2.getAccount());
        assertEquals(dateFormat.parse(date), transaction1.getDate());
        assertEquals(dateFormat.parse(date), transaction2.getDate());
        assertEquals(new BigDecimal(amount), transaction1.getAmount().abs());
        assertEquals(transaction1.getAmount(), transaction2.getAmount().negate());
        assertTrue(transaction1.isCleared());
        assertTrue(transaction2.isCleared());

        assertEquals(1, transaction1.getDetails().size());
        assertEquals(1, transaction2.getDetails().size());
        TransactionDetail detail1 = transaction1.getDetails().get(0);
        TransactionDetail detail2 = transaction2.getDetails().get(0);
        assertTrue(detail1.isTransfer());
        assertTrue(detail2.isTransfer());
        assertNull(detail1.getCategory());
        assertNull(detail2.getCategory());
        assertEquals(new BigDecimal(amount), detail1.getAmount().abs());
        assertEquals(detail1.getAmount(), detail2.getAmount().negate());
        assertSame(group, detail1.getGroup());
        assertSame(group, detail2.getGroup());
        assertEquals(memo, detail1.getMemo());
        assertEquals(memo, detail2.getMemo());
        assertSame(detail1.getRelatedDetail(), detail2);
        assertSame(detail2.getRelatedDetail(), detail1);
    }

    @Test
    public void importRecordUpdatesTransfer() throws Exception {
        String date = "12/8/98";
        String category = "[account1]/group";
        String memo = "memo";
        String amount = "123.45";
        QifRecord record = createRecord(date, amount, true, "Payee", category, memo);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        TransactionDetail transferDetail = addPendingTransfer(groups[0], record);
        Transaction originalTransaction = transferDetail.getTransaction();
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService).saveTransaction(saveCapture.capture());
        verify(transactionService).deleteTransaction(originalTransaction);
        assertEquals(1, saveCapture.getAllValues().size());
        Transaction updatedTransaction = transferDetail.getTransaction();
        assertSame(transferDetail.getTransaction(), updatedTransaction);
        assertEquals(1, updatedTransaction.getDetails().size());
        assertSame(transferDetail, updatedTransaction.getDetails().get(0));
        assertEquals("999", updatedTransaction.getNumber());
        assertSame(payee, updatedTransaction.getPayee());
        assertTrue(updatedTransaction.isCleared());
        assertEquals(memo, updatedTransaction.getMemo());
    }

    @Test
    public void importRecordUpdatesSplitTransfer() throws Exception {
        String date = "12/8/98";
        String category = "[account1]";
        String memo = "memo";
        String amount = "123.45";
        QifRecord record = createRecord(date, amount, true, "Payee", category, memo);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        TransactionDetail transferDetail1 = addPendingTransfer(null, record);
        transferDetail1.setAmount(new BigDecimal("100.00"));
        Transaction originalTransaction = transferDetail1.getTransaction();
        TransactionDetail transferDetail2 = new TransactionDetail(new BigDecimal("23.45"), null, null);
        originalTransaction.addDetails(transferDetail2);
        transferDetail1.getRelatedDetail().getTransaction().addDetails(transferDetail2.getRelatedDetail());
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService).saveTransaction(saveCapture.capture());
        verify(transactionService).deleteTransaction(originalTransaction);
        assertEquals(1, saveCapture.getAllValues().size());
        Transaction updatedTransaction = saveCapture.getAllValues().get(0);
        assertSame(updatedTransaction, transferDetail1.getTransaction());
        assertEquals(2, updatedTransaction.getDetails().size());
        assertSame(transferDetail1, updatedTransaction.getDetails().get(0));
        assertSame(transferDetail2, updatedTransaction.getDetails().get(1));
        assertSame(account, updatedTransaction.getAccount());
        assertEquals("999", updatedTransaction.getNumber());
        assertSame(payee, updatedTransaction.getPayee());
        assertTrue(updatedTransaction.isCleared());
        assertEquals(memo, updatedTransaction.getMemo());
    }

    @Test
    public void importRecordMergesExistingTransfersForSplit() throws Exception {
        String date = "12/8/98";
        String[] categories = { null, "[account1]/group1", "[account2]" };
        String[] memos = { "main memo", "memo1", null };
        String[] amounts = { "123.45", "120.00", "3.45" };
        QifRecord record = createSplit(date, amounts, true, "Payee", categories , memos);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionDetail> transferDetails = addTransfers(groups, record);
        List<Transaction> expectedDeletes = getTransactions(transferDetails);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        for (Transaction transaction : expectedDeletes) {
            assertTrue(transaction.getDetails().isEmpty());
        }

        verify(transactionService).saveTransaction(saveCapture.capture());
        for (Transaction transaction : expectedDeletes) {
            verify(transactionService).deleteTransaction(transaction);
        }
        assertEquals(1, saveCapture.getAllValues().size());
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertEquals("999", savedTransaction.getNumber());
        assertSame(payee, savedTransaction.getPayee());
        assertTrue(savedTransaction.isCleared());
        assertEquals(2, savedTransaction.getDetails().size());
        assertTrue(savedTransaction.getDetails().contains(transferDetails.get(0)));
        assertTrue(savedTransaction.getDetails().contains(transferDetails.get(1)));
        assertEquals(memos[0], savedTransaction.getMemo());
    }

    @Test
    public void importRecordMatchesCombinedSplitTransfer() throws Exception {
        String date = "12/8/98";
        String[] categories = { null, "[account1]", "[account1]", "[account1]/group" };
        String[] memos = { "main memo", "memo1", null, null };
        String[] amounts = { "124.00", "120.00", "3.45", "0.55" };
        QifRecord record = createSplit(date, amounts, true, "Payee", categories , memos);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionDetail> transferDetails = addTransfers(groups, record);
        when(accountOperations.getAccount(null, "account1"))
                .thenReturn(transferDetails.get(0).getRelatedDetail().getTransaction().getAccount());
        Transaction firstTransaction = mergeTransfers(transferDetails.subList(0, 2)).getRelatedDetail().getTransaction();
        List<Transaction> expectedDeletes = getTransactions(transferDetails);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        for (Transaction transaction : expectedDeletes) {
            assertTrue(transaction.getDetails().isEmpty());
        }
        verify(transactionService).saveTransaction(saveCapture.capture());
        verify(transactionService).deleteTransaction(expectedDeletes.get(0));
        verify(transactionService, times(2)).saveDetail(any(TransactionDetail.class));
        verify(transactionService).deleteTransaction(expectedDeletes.get(1));
        assertEquals(1, saveCapture.getAllValues().size());
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertEquals("999", savedTransaction.getNumber());
        assertSame(payee, savedTransaction.getPayee());
        assertEquals(memos[0], savedTransaction.getMemo());
        assertTrue(savedTransaction.isCleared());
        assertEquals(3, savedTransaction.getDetails().size());
        assertSame(transferDetails.get(0), savedTransaction.getDetails().get(0));
        assertEquals("120.00", transferDetails.get(0).getAmount().toString());
        assertEquals("3.45", savedTransaction.getDetails().get(1).getAmount().toString());
        assertEquals("0.55", savedTransaction.getDetails().get(2).getAmount().toString());
        assertEquals(2, firstTransaction.getDetails().size());
        assertEquals("-120.00", firstTransaction.getDetails().get(0).getAmount().toString());
        assertEquals("-3.45", firstTransaction.getDetails().get(1).getAmount().toString());
        assertSame(firstTransaction, savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction());
        assertSame(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction(),
                   savedTransaction.getDetails().get(1).getRelatedDetail().getTransaction());
        assertNotSame(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction(),
                   savedTransaction.getDetails().get(2).getRelatedDetail().getTransaction());
    }

    @Test
    public void importRecordMatchesCombinedSplitTransfer2() throws Exception {
        String date = "12/8/98";
        String[] categories = { null, "[account1]", "[account1]", "[account1]" };
        String[] memos = { "main memo", "memo1", null, null };
        String[] amounts = { "123.45", "3.45", "123.45", "-3.45" };
        QifRecord record = createSplit(date, amounts, true, "Payee", categories , memos);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionDetail> transferDetails = addTransfers(groups, record);
        when(accountOperations.getAccount(null, "account1"))
                .thenReturn(transferDetails.get(0).getRelatedDetail().getTransaction().getAccount());
        Transaction firstTransaction = mergeTransfers(transferDetails).getRelatedDetail().getTransaction();
        List<Transaction> expectedDeletes = getTransactions(transferDetails);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        for (Transaction transaction : expectedDeletes) {
            assertTrue(transaction.getDetails().isEmpty());
        }
        verify(transactionService).saveTransaction(saveCapture.capture());
        for (Transaction transaction : expectedDeletes) {
            verify(transactionService).deleteTransaction(transaction);
        }
        verify(transactionService, times(4)).saveDetail(any(TransactionDetail.class));
        assertEquals(1, saveCapture.getAllValues().size());
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertEquals("999", savedTransaction.getNumber());
        assertSame(payee, savedTransaction.getPayee());
        assertEquals(memos[0], savedTransaction.getMemo());
        assertTrue(savedTransaction.isCleared());
        assertEquals(3, savedTransaction.getDetails().size());
        assertSame(transferDetails.get(0), savedTransaction.getDetails().get(0));
        assertEquals("3.45", transferDetails.get(0).getAmount().toString());
        assertEquals("123.45", savedTransaction.getDetails().get(1).getAmount().toString());
        assertEquals("-3.45", savedTransaction.getDetails().get(2).getAmount().toString());
        assertEquals(3, firstTransaction.getDetails().size());
        assertEquals("-3.45", firstTransaction.getDetails().get(0).getAmount().toString());
        assertEquals("-123.45", firstTransaction.getDetails().get(1).getAmount().toString());
        assertEquals("3.45", firstTransaction.getDetails().get(2).getAmount().toString());
        assertSame(firstTransaction, savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction());
    }

    @Test
    public void importRecordFixesTransferToFromSameAccount() throws Exception {
        String date = "12/8/98";
        String category = "[account]/group";
        String memo = "memo";
        String amount = "123.45";
        QifRecord record = createRecord(date, amount, true, "Opening Balance", category, memo);
        Payee payee = addPayee(record);
        TransactionGroup group = addTransactionGroups(record)[0];
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        assertSame(account, accountHolder.getAccount());
        verify(transactionService).saveTransaction(saveCapture.capture());
        assertEquals(1, saveCapture.getAllValues().size());
        Transaction transaction1 = saveCapture.getAllValues().get(0);
        assertSame(payee, transaction1.getPayee());
        assertSame(account, transaction1.getAccount());
        assertEquals(dateFormat.parse(date), transaction1.getDate());
        assertEquals(new BigDecimal(amount), transaction1.getAmount().abs());
        assertTrue(transaction1.isCleared());

        assertEquals(1, transaction1.getDetails().size());
        TransactionDetail detail1 = transaction1.getDetails().get(0);
        assertFalse(detail1.isTransfer());
        assertNull(detail1.getCategory());
        assertEquals(new BigDecimal(amount), detail1.getAmount().abs());
        assertSame(group, detail1.getGroup());
        assertEquals(memo, detail1.getMemo());
        assertNull(detail1.getRelatedDetail());
    }

    @Test
    public void importRecordCreatesCombinedPendingTransfer() throws Exception {
        Account account1 = TestDomainUtils.createAccount("account1");
        String date = "12/8/98";
        String[] categories = { null, "[account1]", "[account1]", "[account1]" };
        String[] memos = { "main memo", "memo1", null, null };
        String[] amounts = { "123.45", "3.45", "123.45", "-3.45" };
        QifRecord record = createSplit(date, amounts, true, "Payee", categories , memos);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        when(accountOperations.getAccount(null, "account1")).thenReturn(account1);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService, times(2)).saveTransaction(saveCapture.capture());
        verify(transactionService, times(6)).saveDetail(any(TransactionDetail.class));
        assertEquals(2, saveCapture.getAllValues().size());
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertEquals("999", savedTransaction.getNumber());
        assertSame(payee, savedTransaction.getPayee());
        assertTrue(savedTransaction.isCleared());
        assertEquals(memos[0], savedTransaction.getMemo());
        assertEquals(3, savedTransaction.getDetails().size());
        assertEquals("3.45", savedTransaction.getDetails().get(0).getAmount().toString());
        assertEquals("123.45", savedTransaction.getDetails().get(1).getAmount().toString());
        assertEquals("-3.45", savedTransaction.getDetails().get(2).getAmount().toString());
        assertSame(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction(),
                   savedTransaction.getDetails().get(1).getRelatedDetail().getTransaction());
        assertSame(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction(),
                   savedTransaction.getDetails().get(2).getRelatedDetail().getTransaction());
        assertEquals(1, pendingTransferDetails.getPendingTransferDetails().size());
        assertSame(savedTransaction.getDetails().get(0).getRelatedDetail(), pendingTransferDetails.remove(
                account1, account.getName(), savedTransaction.getDate(), new BigDecimal("-123.45"), null));
    }

    private List<Transaction> getTransactions(List<? extends TransactionDetail> details) {
        List<Transaction> transactions = new ArrayList<Transaction>();
        for (TransactionDetail detail : details) {
            transactions.add(detail.getTransaction());
        }
        return transactions;
    }

    @Test
    public void importRecordCreatesSplitTransactionWithTransfers() throws Exception {
        String date = "12/8/98";
        String[] categories = { null, "[account1]/group1", "[account2]" };
        String[] memos = { "main memo", "memo1", null };
        String[] amounts = { "123.45", "120.00", "3.45" };
        QifRecord record = createSplit(date, amounts, true, "Payee", categories , memos);
        Payee payee = addPayee(record);
        Account accounts[] = { account, TestDomainUtils.createAccount("account1"), TestDomainUtils.createAccount("account2") };
        when(accountOperations.getAccount(null, "account1")).thenReturn(accounts[1]);
        when(accountOperations.getAccount(null, "account2")).thenReturn(accounts[2]);
        TransactionGroup[] groups = addTransactionGroups(record);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService, times(3)).saveTransaction(saveCapture.capture());
        verify(transactionService, times(4)).saveDetail(any(TransactionDetail.class));
        for (int i=0; i<amounts.length; i++) {
            BigDecimal amount = new BigDecimal(amounts[i]);
            amount = i == 0 ? amount : amount.negate();
            Transaction transaction = findSavedTransaction(amount);
            assertSame(payee, transaction.getPayee());
            assertSame(accounts[i], transaction.getAccount());
            assertEquals(dateFormat.parse(date), transaction.getDate());
            assertEquals(amount, transaction.getAmount());
            assertTrue(transaction.isCleared());
            assertEquals(memos[i], transaction.getMemo());
        }

        assertEquals(3, saveCapture.getAllValues().size());
        Transaction transaction = findSavedTransaction(new BigDecimal(amounts[0]));
        List<TransactionDetail> details = transaction.getDetails();
        for (int i=0; i<details.size(); i++) {
            TransactionDetail detail = details.get(i);
            assertTrue(detail.isTransfer());
            assertEquals(new BigDecimal(amounts[i+1]), detail.getAmount());
            assertSame(groups[i+1], detail.getGroup());
            assertEquals(memos[i+1], detail.getMemo());
        }
    }

    private Transaction findSavedTransaction(BigDecimal amount) {
        for (int i=0; i<saveCapture.getAllValues().size(); i++) {
            Transaction transaction = saveCapture.getAllValues().get(i);
            if (transaction.getAmount().equals(amount)) {
                return transaction;
            }
        }
        return null;
    }

    @Test
    public void importSplitWithoutCategory() throws Exception {
        String date = "12/8/98";
        String[] categories = { "category:subcategory", null, "cat2" };
        String[] memos = { "main memo", "memo1", "memo2" };
        String[] amounts = { "123.45", "120.00", "3.45" };
        QifRecord record = createSplit(date, amounts, true, "Payee", categories , memos);
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionCategory> types = addTransactionCategorys(record);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService).saveTransaction(saveCapture.capture());
        Transaction transaction = saveCapture.getValue();
        assertSame(payee, transaction.getPayee());
        assertSame(account, transaction.getAccount());
        assertEquals(dateFormat.parse(date), transaction.getDate());
        assertEquals(new BigDecimal(amounts[0]), transaction.getAmount());
        assertTrue(transaction.isCleared());
        assertEquals(memos[0], transaction.getMemo());

        List<TransactionDetail> details = transaction.getDetails();
        assertEquals(types.size(), details.size());
        for (int i=0; i<details.size(); i++) {
            TransactionDetail detail = details.get(i);
            assertFalse(detail.isTransfer());
            assertEquals(new BigDecimal(amounts[i+1]), detail.getAmount());
            assertSame(types.get(i), detail.getCategory());
            assertSame(groups[i+1], detail.getGroup());
            assertEquals(memos[i+1], detail.getMemo());
        }
    }
}