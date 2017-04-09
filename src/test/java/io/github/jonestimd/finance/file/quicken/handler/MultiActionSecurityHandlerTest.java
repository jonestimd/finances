package io.github.jonestimd.finance.file.quicken.handler;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.file.quicken.qif.QifRecord;
import org.junit.Test;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

//TODO test qif-context by using SecurityTransactionConverter
public class MultiActionSecurityHandlerTest extends HandlerTestFixture<MultiActionSecurityHandler> {
    private static final String SECURITY_NAME = "Security Name";

    private Security security;

    protected AccountType getAccountType() {
        return AccountType.BROKERAGE;
    }

    protected void initializeMocks() throws Exception {
        security = addSecurity(SECURITY_NAME);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
    }

    private void checkDetail(TransactionDetail detail, TransactionCategory type, double amount, String memo) {
        assertThat(detail.getCategory()).isSameAs(type);
        assertThat(detail.getAmount().doubleValue()).isCloseTo(amount, within(0d));
        assertThat(detail.getMemo()).isEqualTo(memo);
    }

    @Test
    public void convertBuyRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "103.00", "memo", true);
        record.setValue(SECURITY_ACTION, "Buy");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(PRICE, "10.001");
        record.setValue(SHARES, "10");
        record.setValue(COMMISSION, "3.00");
        initializeMocks();

        List<Transaction> transactions = getHandler("Buy").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getDetails().get(0).getAssetQuantity()).isEqualTo(BigDecimal.TEN);

        assertThat(transaction.getDetails()).hasSize(2);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), buyType, -100, null);
        checkDetail(transaction.getDetails().get(1), commissionType, -3, null);
    }

    @Test
    public void convertSellRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "103.00", "memo", true);
        record.setValue(SECURITY_ACTION, "Sell");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(PRICE, "10.001");
        record.setValue(SHARES, "10");
        record.setValue(COMMISSION, "3.00");
        initializeMocks();

        List<Transaction> transactions = getHandler("Sell").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getDetails().get(0).getAssetQuantity()).isEqualTo(BigDecimal.TEN.negate());

        assertThat(transaction.getDetails()).hasSize(2);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), sellType, 106d, null);
        checkDetail(transaction.getDetails().get(1), commissionType, -3d, null);
    }

    @Test
    public void convertDividendRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "123.00", "memo", true);
        record.setValue(SECURITY_ACTION, "Div");
        record.setValue(SECURITY, SECURITY_NAME);
        initializeMocks();

        List<Transaction> transactions = getHandler("Div").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getDetails().get(0).getAssetQuantity()).isNull();

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), divType, 123d, null);
    }

    @Test
    public void convertInterestIncomeRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "12.30", "memo", true);
        record.setValue(SECURITY_ACTION, "IntInc");
        record.setValue(SECURITY, SECURITY_NAME);
        initializeMocks();

        List<Transaction> transactions = getHandler("IntInc").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), interestType, 12.30d, null);
    }

    @Test
    public void convertMiscIncRecordNoCategory() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "12.30", "memo", true);
        record.setValue(SECURITY_ACTION, "MiscInc");
        record.setValue(SECURITY, SECURITY_NAME);
        TransactionCategory type = new TransactionCategory();
        when(TransactionCategoryOperations.getOrCreateTransactionCategory(null, true, "Misc Income")).thenReturn(type);
        initializeMocks();

        List<Transaction> transactions = getHandler("MiscInc").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getDetails().get(0).getAssetQuantity()).isNull();
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), type, 12.30d, "memo");
        assertThat(transaction.getDetails().get(0).getGroup()).isNull();
    }

    @Test
    public void convertMiscIncRecordGroupNoCategory() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "12.30", "memo", true);
        record.setValue(SECURITY_ACTION, "MiscInc");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(CATEGORY, "/group");
        TransactionCategory type = new TransactionCategory();
        when(TransactionCategoryOperations.getOrCreateTransactionCategory(null, true, "Misc Income")).thenReturn(type);
        TransactionGroup group = new TransactionGroup();
        when(transactionGroupOperations.getTransactionGroup("group")).thenReturn(group);
        initializeMocks();

        List<Transaction> transactions = getHandler("MiscInc").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), type, 12.30d, "memo");
        assertThat(transaction.getDetails().get(0).getGroup()).isSameAs(group);
    }

    @Test
    public void convertMiscIncRecordCategoryAndGroup() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "12.30", "memo", true);
        record.setValue(SECURITY_ACTION, "MiscInc");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(CATEGORY, "category/group");
        TransactionCategory type = new TransactionCategory();
        when(TransactionCategoryOperations.getTransactionCategory("category")).thenReturn(type);
        TransactionGroup group = new TransactionGroup();
        when(transactionGroupOperations.getTransactionGroup("group")).thenReturn(group);
        initializeMocks();

        List<Transaction> transactions = getHandler("MiscInc").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), type, 12.30d, "memo");
        assertThat(transaction.getDetails().get(0).getGroup()).isSameAs(group);
    }

    @Test
    public void convertMiscExpRecordNoCategory() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "12.30", "memo", true);
        record.setValue(SECURITY_ACTION, "MiscExp");
        record.setValue(SECURITY, SECURITY_NAME);
        TransactionCategory type = new TransactionCategory();
        when(TransactionCategoryOperations.getOrCreateTransactionCategory(null, false, "Misc Expense")).thenReturn(type);
        initializeMocks();

        List<Transaction> transactions = getHandler("MiscExp").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), type, -12.30d, "memo");
    }

    @Test
    public void convertShrsInRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "103.00", "memo", true);
        record.setValue(SECURITY_ACTION, "ShrsIn");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(SHARES, "10");
        initializeMocks();

        List<Transaction> transactions = getHandler("ShrsIn").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getDetails().get(0).getAssetQuantity()).isEqualTo(BigDecimal.TEN);

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), shrsInType, -103d, null);
    }

    @Test
    public void convertShrsOutRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "103.00", "memo", true);
        record.setValue(SECURITY_ACTION, "ShrsOut");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(SHARES, "10");
        initializeMocks();

        List<Transaction> transactions = getHandler("ShrsOut").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getDate()).isEqualTo(record.getDate(DATE));
        assertThat(transaction.getMemo()).isEqualTo("memo");
        assertThat(transaction.isCleared()).isTrue();
        assertThat(transaction.getDetails().get(0).getAssetQuantity()).isEqualTo(BigDecimal.TEN.negate());

        assertThat(transaction.getDetails()).hasSize(1);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), shrsOutType, 103d, null);
    }

    @Test
    public void convertReinvDivRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "103.00", "memo", true);
        record.setValue(SECURITY_ACTION, "ReinvDiv");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(PRICE, "10.001");
        record.setValue(SHARES, "10");
        record.setValue(COMMISSION, "3.00");
        initializeMocks();

        List<Transaction> transactions = getHandler("ReinvDiv").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getDetails().get(1).getAssetQuantity()).isEqualTo(BigDecimal.TEN);

        assertThat(transaction.getDetails()).hasSize(3);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), divType, 103d, null);
        checkDetail(transaction.getDetails().get(1), reinvestType, -100d, null);
        checkDetail(transaction.getDetails().get(2), commissionType, -3d, null);
    }

    @Test
    public void convertReinvIntRecord() throws Exception {
        QifRecord record = createRecord("12/8/98", null, "103.00", "memo", true);
        record.setValue(SECURITY_ACTION, "ReinvInt");
        record.setValue(SECURITY, SECURITY_NAME);
        record.setValue(PRICE, "10.001");
        record.setValue(SHARES, "10");
        record.setValue(COMMISSION, "3.00");
        initializeMocks();

        List<Transaction> transactions = getHandler("ReinvInt").convertRecord(account, record);

        assertThat(transactions).hasSize(1);
        Transaction transaction = transactions.get(0);
        assertThat(transaction.getDetails().get(1).getAssetQuantity()).isEqualTo(BigDecimal.TEN);

        assertThat(transaction.getDetails()).hasSize(3);
        assertThat(transaction.getSecurity()).isSameAs(security);
        checkDetail(transaction.getDetails().get(0), interestType, 103d, null);
        checkDetail(transaction.getDetails().get(1), reinvestType, -100d, null);
        checkDetail(transaction.getDetails().get(2), commissionType, -3d, null);
    }
}