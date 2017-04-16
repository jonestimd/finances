package io.github.jonestimd.finance.file.quicken.qif;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
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
        when(payeeOperations.getAllPayees()).thenReturn(Collections.emptyList());
        converter = getQifContext().getMoneyTransactionConverter();
        accountHolder = new AccountHolder();
        accountHolder.setAccount(account);
    }

    @Test
    public void getTypesIncludesMoneyTypes() throws Exception {
        initializeAccountHolder();
        assertThat(converter.getTypes().contains("Type:Bank")).isTrue();
        assertThat(converter.getTypes().contains("Type:Cash")).isTrue();
        assertThat(converter.getTypes().contains("Type:CCard")).isTrue();
        assertThat(converter.getTypes().contains("Type:Oth L")).isTrue();
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

        assertThat(accountHolder.getAccount()).isSameAs(account);
        verify(transactionService).saveTransaction(saveCapture.capture());
        Transaction transaction = saveCapture.getValue();
        assertThat(transaction.getPayee()).isSameAs(payee);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction.getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getNumber()).isEqualTo(checkNumber);

        List<TransactionDetail> details = transaction.getDetails();
        assertThat(details).hasSize(1);
        TransactionDetail detail = details.get(0);
        assertThat(detail.isTransfer()).isFalse();
        assertThat(detail.getAmount()).isEqualTo(new BigDecimal(amount));
        assertThat(detail.getCategory()).isSameAs(TransactionCategory);
        assertThat(detail.getGroup()).isSameAs(groups[0]);
        assertThat(detail.getMemo()).isEqualTo(memo);
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
        assertThat(transaction.getDate()).isEqualTo(date);
        assertThat(transaction.isCleared()).isFalse();
    }

    @Test
    public void importRecordThrowsExceptionForInvalidDate() throws Exception {
        String qifDate = "12/23/ab";
        QifRecord record = createRecord(qifDate, "123.45", false, "Payee", "category:subcategory", "memo");
        addPayee(record);
        initializeAccountHolder();

        try {
            converter.importRecord(accountHolder, record);
            fail("expected an exception");
        }
        catch (QuickenException ex) {
            assertThat(ex.getMessageKey()).isEqualTo("import.qif.invalidDate");
            assertThat(ex.getMessageArgs()).isEqualTo(new Object[]{qifDate, 1L});
        }
    }

    private QifRecord createSplit(String date, String[] amounts, String payeeName, String[] categories, String[] memos) {
        QifRecord record = createRecord(date, amounts[0], true, payeeName, categories[0] , memos[0]);
        for (int i=1; i<categories.length; i++) {
            record.setValue(SPLIT_CATEGORY, categories[i]);
            if (memos[i] != null) record.setValue(SPLIT_MEMO, memos[i]);
            record.setValue(SPLIT_AMOUNT, amounts[i]);
        }
        return record;
    }

    private List<String> combineValues(QifRecord record, QifField mainCode, QifField splitCode) {
        List<String> values = new ArrayList<>();
        values.add(record.getValue(mainCode));
        if (record.hasValue(splitCode)) {
            values.addAll(record.getValues(splitCode));
        }
        return values;
    }

    private List<TransactionCategory> addTransactionCategorys(QifRecord record) throws Exception {
        List<String> categories = record.isSplit() ? record.getValues(SPLIT_CATEGORY) : singletonList(record.getValue(CATEGORY));
        List<TransactionCategory> types = new ArrayList<>();
        for (String category : categories) {
            CategoryParser parser = new CategoryParser(category);
            if (!parser.isTransfer()) {
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
        List<TransactionDetail> transferDetails = new ArrayList<>();
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
        QifRecord record = createSplit(date, amounts, "Payee", categories , memos);
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionCategory> types = addTransactionCategorys(record);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService).saveTransaction(saveCapture.capture());
        Transaction transaction = saveCapture.getValue();
        assertThat(transaction.getPayee()).isSameAs(payee);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction.getAmount()).isEqualTo(new BigDecimal(amounts[0]));
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getMemo()).isEqualTo(memos[0]);

        List<TransactionDetail> details = transaction.getDetails();
        assertThat(details.size()).isEqualTo(types.size());
        for (int i=0; i<details.size(); i++) {
            TransactionDetail detail = details.get(i);
            assertThat(detail.isTransfer()).isFalse();
            assertThat(detail.getAmount()).isEqualTo(new BigDecimal(amounts[i+1]));
            assertThat(detail.getCategory()).isSameAs(types.get(i));
            assertThat(detail.getGroup()).isSameAs(groups[i+1]);
            assertThat(detail.getMemo()).isEqualTo(memos[i+1]);
        }
    }

    @Test
    public void importRecordVerifiesSplitTotal() throws Exception {
        String date = "12/8/98";
        String payeeName = "Payee";
        String[] categories = { "category:subcategory", "cat1/group", "cat2" };
        String[] memos = { "main memo", "memo1", "memo2" };
        String[] amounts = { "123.45", "12.00", "3.45" };
        QifRecord record = createSplit(date, amounts, payeeName, categories , memos);
        addPayee(record);
        addTransactionCategorys(record);
        addTransactionGroups(record);
        initializeAccountHolder();

        try {
            converter.importRecord(accountHolder, record);
            fail("expected an exception");
        }
        catch (QuickenException ex) {
            verify(transactionService).saveTransaction(any(Transaction.class));
            assertThat(ex.getMessageKey()).isEqualTo("import.qif.invalidSplit");
            assertThat(ex.getMessageArgs()).isEqualTo(new Object[] {new BigDecimal(amounts[0]), new BigDecimal("15.45"), 1L});
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

        assertThat(accountHolder.getAccount()).isSameAs(account);
        verify(transactionService, times(2)).saveTransaction(saveCapture.capture());
        verify(transactionService, times(2)).saveDetail(any(TransactionDetail.class));
        assertThat(saveCapture.getAllValues()).hasSize(2);
        Transaction transaction1 = saveCapture.getAllValues().get(0);
        Transaction transaction2 = saveCapture.getAllValues().get(1);
        assertThat(transaction1.getPayee()).isSameAs(payee);
        assertThat(transaction2.getPayee()).isSameAs(payee);
        assertThat(transaction1.getAccount()).isSameAs(account);
        assertThat(transaction2.getAccount()).isSameAs(transferAccount);
        assertThat(transaction1.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction2.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction1.getAmount().abs()).isEqualTo(new BigDecimal(amount));
        assertThat(transaction2.getAmount().negate()).isEqualTo(transaction1.getAmount());
        assertThat(transaction1.isCleared()).isTrue();
        assertThat(transaction2.isCleared()).isTrue();

        assertThat(transaction1.getDetails()).hasSize(1);
        assertThat(transaction2.getDetails()).hasSize(1);
        TransactionDetail detail1 = transaction1.getDetails().get(0);
        TransactionDetail detail2 = transaction2.getDetails().get(0);
        assertThat(detail1.isTransfer()).isTrue();
        assertThat(detail2.isTransfer()).isTrue();
        assertThat(detail1.getCategory()).isNull();
        assertThat(detail2.getCategory()).isNull();
        assertThat(detail1.getAmount().abs()).isEqualTo(new BigDecimal(amount));
        assertThat(detail2.getAmount().negate()).isEqualTo(detail1.getAmount());
        assertThat(detail1.getGroup()).isSameAs(group);
        assertThat(detail2.getGroup()).isSameAs(group);
        assertThat(detail1.getMemo()).isEqualTo(memo);
        assertThat(detail2.getMemo()).isEqualTo(memo);
        assertThat(detail2).isSameAs(detail1.getRelatedDetail());
        assertThat(detail1).isSameAs(detail2.getRelatedDetail());
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
        assertThat(saveCapture.getAllValues()).hasSize(1);
        Transaction updatedTransaction = transferDetail.getTransaction();
        assertThat(updatedTransaction).isSameAs(transferDetail.getTransaction());
        assertThat(updatedTransaction.getDetails()).hasSize(1);
        assertThat(updatedTransaction.getDetails().get(0)).isSameAs(transferDetail);
        assertThat(updatedTransaction.getNumber()).isEqualTo("999");
        assertThat(updatedTransaction.getPayee()).isSameAs(payee);
        assertThat(updatedTransaction.isCleared()).isTrue();
        assertThat(updatedTransaction.getMemo()).isEqualTo(memo);
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
        assertThat(saveCapture.getAllValues()).hasSize(1);
        Transaction updatedTransaction = saveCapture.getAllValues().get(0);
        assertThat(transferDetail1.getTransaction()).isSameAs(updatedTransaction);
        assertThat(updatedTransaction.getDetails()).hasSize(2);
        assertThat(updatedTransaction.getDetails().get(0)).isSameAs(transferDetail1);
        assertThat(updatedTransaction.getDetails().get(1)).isSameAs(transferDetail2);
        assertThat(updatedTransaction.getAccount()).isSameAs(account);
        assertThat(updatedTransaction.getNumber()).isEqualTo("999");
        assertThat(updatedTransaction.getPayee()).isSameAs(payee);
        assertThat(updatedTransaction.isCleared()).isTrue();
        assertThat(updatedTransaction.getMemo()).isEqualTo(memo);
    }

    @Test
    public void importRecordMergesExistingTransfersForSplit() throws Exception {
        String date = "12/8/98";
        String[] categories = { null, "[account1]/group1", "[account2]" };
        String[] memos = { "main memo", "memo1", null };
        String[] amounts = { "123.45", "120.00", "3.45" };
        QifRecord record = createSplit(date, amounts, "Payee", categories , memos);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionDetail> transferDetails = addTransfers(groups, record);
        List<Transaction> expectedDeletes = getTransactions(transferDetails);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        for (Transaction transaction : expectedDeletes) {
            assertThat(transaction.getDetails().isEmpty()).isTrue();
        }

        verify(transactionService).saveTransaction(saveCapture.capture());
        for (Transaction transaction : expectedDeletes) {
            verify(transactionService).deleteTransaction(transaction);
        }
        assertThat(saveCapture.getAllValues()).hasSize(1);
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertThat(savedTransaction.getNumber()).isEqualTo("999");
        assertThat(savedTransaction.getPayee()).isSameAs(payee);
        assertThat(savedTransaction.isCleared()).isTrue();
        assertThat(savedTransaction.getDetails()).hasSize(2);
        assertThat(savedTransaction.getDetails().contains(transferDetails.get(0))).isTrue();
        assertThat(savedTransaction.getDetails().contains(transferDetails.get(1))).isTrue();
        assertThat(savedTransaction.getMemo()).isEqualTo(memos[0]);
    }

    @Test
    public void importRecordMatchesCombinedSplitTransfer() throws Exception {
        String date = "12/8/98";
        String[] categories = { null, "[account1]", "[account1]", "[account1]/group" };
        String[] memos = { "main memo", "memo1", null, null };
        String[] amounts = { "124.00", "120.00", "3.45", "0.55" };
        QifRecord record = createSplit(date, amounts, "Payee", categories , memos);
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
            assertThat(transaction.getDetails().isEmpty()).isTrue();
        }
        verify(transactionService).saveTransaction(saveCapture.capture());
        verify(transactionService).deleteTransaction(expectedDeletes.get(0));
        verify(transactionService, times(2)).saveDetail(any(TransactionDetail.class));
        verify(transactionService).deleteTransaction(expectedDeletes.get(1));
        assertThat(saveCapture.getAllValues()).hasSize(1);
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertThat(savedTransaction.getNumber()).isEqualTo("999");
        assertThat(savedTransaction.getPayee()).isSameAs(payee);
        assertThat(savedTransaction.getMemo()).isEqualTo(memos[0]);
        assertThat(savedTransaction.isCleared()).isTrue();
        assertThat(savedTransaction.getDetails()).hasSize(3);
        assertThat(savedTransaction.getDetails().get(0)).isSameAs(transferDetails.get(0));
        assertThat(transferDetails.get(0).getAmount().toString()).isEqualTo("120.00");
        assertThat(savedTransaction.getDetails().get(1).getAmount().toString()).isEqualTo("3.45");
        assertThat(savedTransaction.getDetails().get(2).getAmount().toString()).isEqualTo("0.55");
        assertThat(firstTransaction.getDetails()).hasSize(2);
        assertThat(firstTransaction.getDetails().get(0).getAmount().toString()).isEqualTo("-120.00");
        assertThat(firstTransaction.getDetails().get(1).getAmount().toString()).isEqualTo("-3.45");
        assertThat(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction()).isSameAs(firstTransaction);
        assertThat(savedTransaction.getDetails().get(1).getRelatedDetail().getTransaction())
                .isSameAs(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction());
        assertThat(savedTransaction.getDetails().get(2).getRelatedDetail().getTransaction())
                .isNotSameAs(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction());
    }

    @Test
    public void importRecordMatchesCombinedSplitTransfer2() throws Exception {
        String date = "12/8/98";
        String[] categories = { null, "[account1]", "[account1]", "[account1]" };
        String[] memos = { "main memo", "memo1", null, null };
        String[] amounts = { "123.45", "3.45", "123.45", "-3.45" };
        QifRecord record = createSplit(date, amounts, "Payee", categories , memos);
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
            assertThat(transaction.getDetails().isEmpty()).isTrue();
        }
        verify(transactionService).saveTransaction(saveCapture.capture());
        for (Transaction transaction : expectedDeletes) {
            verify(transactionService).deleteTransaction(transaction);
        }
        verify(transactionService, times(4)).saveDetail(any(TransactionDetail.class));
        assertThat(saveCapture.getAllValues()).hasSize(1);
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertThat(savedTransaction.getNumber()).isEqualTo("999");
        assertThat(savedTransaction.getPayee()).isSameAs(payee);
        assertThat(savedTransaction.getMemo()).isEqualTo(memos[0]);
        assertThat(savedTransaction.isCleared()).isTrue();
        assertThat(savedTransaction.getDetails()).hasSize(3);
        assertThat(savedTransaction.getDetails().get(0)).isSameAs(transferDetails.get(0));
        assertThat(transferDetails.get(0).getAmount().toString()).isEqualTo("3.45");
        assertThat(savedTransaction.getDetails().get(1).getAmount().toString()).isEqualTo("123.45");
        assertThat(savedTransaction.getDetails().get(2).getAmount().toString()).isEqualTo("-3.45");
        assertThat(firstTransaction.getDetails()).hasSize(3);
        assertThat(firstTransaction.getDetails().get(0).getAmount().toString()).isEqualTo("-3.45");
        assertThat(firstTransaction.getDetails().get(1).getAmount().toString()).isEqualTo("-123.45");
        assertThat(firstTransaction.getDetails().get(2).getAmount().toString()).isEqualTo("3.45");
        assertThat(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction()).isSameAs(firstTransaction);
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

        assertThat(accountHolder.getAccount()).isSameAs(account);
        verify(transactionService).saveTransaction(saveCapture.capture());
        assertThat(saveCapture.getAllValues()).hasSize(1);
        Transaction transaction1 = saveCapture.getAllValues().get(0);
        assertThat(transaction1.getPayee()).isSameAs(payee);
        assertThat(transaction1.getAccount()).isSameAs(account);
        assertThat(transaction1.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction1.getAmount().abs()).isEqualTo(new BigDecimal(amount));
        assertThat(transaction1.isCleared()).isTrue();

        assertThat(transaction1.getDetails()).hasSize(1);
        TransactionDetail detail1 = transaction1.getDetails().get(0);
        assertThat(detail1.isTransfer()).isFalse();
        assertThat(detail1.getCategory()).isNull();
        assertThat(detail1.getAmount().abs()).isEqualTo(new BigDecimal(amount));
        assertThat(detail1.getGroup()).isSameAs(group);
        assertThat(detail1.getMemo()).isEqualTo(memo);
        assertThat(detail1.getRelatedDetail()).isNull();
    }

    @Test
    public void importRecordCreatesCombinedPendingTransfer() throws Exception {
        Account account1 = TestDomainUtils.createAccount("account1");
        String date = "12/8/98";
        String[] categories = { null, "[account1]", "[account1]", "[account1]" };
        String[] memos = { "main memo", "memo1", null, null };
        String[] amounts = { "123.45", "3.45", "123.45", "-3.45" };
        QifRecord record = createSplit(date, amounts, "Payee", categories , memos);
        record.setValue(NUMBER, "999");
        Payee payee = addPayee(record);
        when(accountOperations.getAccount(null, "account1")).thenReturn(account1);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService, times(2)).saveTransaction(saveCapture.capture());
        verify(transactionService, times(6)).saveDetail(any(TransactionDetail.class));
        assertThat(saveCapture.getAllValues()).hasSize(2);
        Transaction savedTransaction = saveCapture.getAllValues().get(0);
        assertThat(savedTransaction.getNumber()).isEqualTo("999");
        assertThat(savedTransaction.getPayee()).isSameAs(payee);
        assertThat(savedTransaction.isCleared()).isTrue();
        assertThat(savedTransaction.getMemo()).isEqualTo(memos[0]);
        assertThat(savedTransaction.getDetails()).hasSize(3);
        assertThat(savedTransaction.getDetails().get(0).getAmount().toString()).isEqualTo("3.45");
        assertThat(savedTransaction.getDetails().get(1).getAmount().toString()).isEqualTo("123.45");
        assertThat(savedTransaction.getDetails().get(2).getAmount().toString()).isEqualTo("-3.45");
        assertThat(savedTransaction.getDetails().get(1).getRelatedDetail().getTransaction())
                .isSameAs(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction());
        assertThat(savedTransaction.getDetails().get(2).getRelatedDetail().getTransaction())
                .isSameAs(savedTransaction.getDetails().get(0).getRelatedDetail().getTransaction());
        assertThat(pendingTransferDetails.getPendingTransferDetails()).hasSize(1);
        assertThat(pendingTransferDetails.remove(account1, account.getName(), savedTransaction.getDate(), new BigDecimal("-123.45"), null))
                .isSameAs(savedTransaction.getDetails().get(0).getRelatedDetail());
    }

    private List<Transaction> getTransactions(List<? extends TransactionDetail> details) {
        List<Transaction> transactions = new ArrayList<>();
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
        QifRecord record = createSplit(date, amounts, "Payee", categories , memos);
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
            assertThat(transaction.getPayee()).isSameAs(payee);
            assertThat(transaction.getAccount()).isSameAs(accounts[i]);
            assertThat(transaction.getDate()).isEqualTo(dateFormat.parse(date));
            assertThat(transaction.getAmount()).isEqualTo(amount);
            assertThat(transaction.isCleared()).isTrue();
            assertThat(transaction.getMemo()).isEqualTo(memos[i]);
        }

        assertThat(saveCapture.getAllValues()).hasSize(3);
        Transaction transaction = findSavedTransaction(new BigDecimal(amounts[0]));
        List<TransactionDetail> details = transaction.getDetails();
        for (int i=0; i<details.size(); i++) {
            TransactionDetail detail = details.get(i);
            assertThat(detail.isTransfer()).isTrue();
            assertThat(detail.getAmount()).isEqualTo(new BigDecimal(amounts[i+1]));
            assertThat(detail.getGroup()).isSameAs(groups[i+1]);
            assertThat(detail.getMemo()).isEqualTo(memos[i+1]);
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
        QifRecord record = createSplit(date, amounts, "Payee", categories , memos);
        Payee payee = addPayee(record);
        TransactionGroup[] groups = addTransactionGroups(record);
        List<TransactionCategory> types = addTransactionCategorys(record);
        initializeAccountHolder();

        converter.importRecord(accountHolder, record);

        verify(transactionService).saveTransaction(saveCapture.capture());
        Transaction transaction = saveCapture.getValue();
        assertThat(transaction.getPayee()).isSameAs(payee);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(dateFormat.parse(date));
        assertThat(transaction.getAmount()).isEqualTo(new BigDecimal(amounts[0]));
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getMemo()).isEqualTo(memos[0]);

        List<TransactionDetail> details = transaction.getDetails();
        assertThat(details.size()).isEqualTo(types.size());
        for (int i=0; i<details.size(); i++) {
            TransactionDetail detail = details.get(i);
            assertThat(detail.isTransfer()).isFalse();
            assertThat(detail.getAmount()).isEqualTo(new BigDecimal(amounts[i+1]));
            assertThat(detail.getCategory()).isSameAs(types.get(i));
            assertThat(detail.getGroup()).isSameAs(groups[i+1]);
            assertThat(detail.getMemo()).isEqualTo(memos[i+1]);
        }
    }
}