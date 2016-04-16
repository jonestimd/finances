package io.github.jonestimd.finance.file.quicken.handler;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;

import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.file.quicken.qif.AccountHolder;
import io.github.jonestimd.finance.file.quicken.qif.QifField;
import io.github.jonestimd.finance.file.quicken.qif.QifRecord;
import io.github.jonestimd.finance.file.quicken.qif.QifTestFixture;
import io.github.jonestimd.finance.file.quicken.qif.SecurityTransactionConverter;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class StockSplitSecurityHandlerTest extends QifTestFixture {
    private static final String SECURITY_NAME = "security name";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
    private AccountHolder accountHolder = new AccountHolder();

    protected AccountType getAccountType() {
        return AccountType.BROKERAGE;
    }

    @Test
    public void stockSplitTransaction() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(QifField.DATE, "07/08' 8");
        record.setValue(QifField.SECURITY, SECURITY_NAME);
        record.setValue(QifField.SECURITY_ACTION, "StkSplit");
        record.setValue(QifField.SHARES, "15");
        addSecurity(record);
        when(assetOperations.findOrCreateSplit(SECURITY_NAME, dateFormat.parse("07/08/2008"), new BigDecimal(2), new BigDecimal(3)))
            .thenReturn(null);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
        SecurityTransactionConverter transactionConverter = getQifContext().getSecurityTransactionConverter();

        transactionConverter.importRecord(accountHolder, record);

        verify(transactionService).saveTransactions(Collections.<Transaction>emptyList());
    }
}