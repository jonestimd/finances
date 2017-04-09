package io.github.jonestimd.finance.file.quicken.qif;

import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.file.FileImport;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class QuickenContextTest extends QifContextTestFixture {

    protected List<QueryBatch> getInsertQueries() {
        return Arrays.asList(ASSET_BATCH);
    }

    @Test
    public void testDuplicateTransfers() throws Exception {
        appendAccounts(AccountType.BANK, "account 1", "account 2");
        appendControl("Type", AccountType.BANK.toString());
        appendRecord("9/29/98", "100.00", getTransferCategory("account 1"));
        appendRecord("9/29/98", "100.00", getTransferCategory("account 1"));
        FileImport qifImport = qifContext.newQifImport();
        long existing = daoContext.countAll(Transaction.class);

        qifImport.importFile(getReader());

        long imported = daoContext.countAll(Transaction.class) - existing;
        assertThat(imported).isEqualTo(4);
    }

    @Test
    public void testSplitWithPendingTransfer() throws Exception {
        appendAccounts(AccountType.BANK, "account 1", "account 3", "account 4");
        appendControl("Type", AccountType.BANK.toString());
        appendRecord("9/29/98", "100.00", getTransferCategory("account 3"));
        appendRecord("9/29/98", "100.00", getTransferCategory("account 1"));
        // next account
        appendAccounts(AccountType.BANK, "account 3");
        appendControl("Type", AccountType.BANK.toString());
        beginRecord("9/29/98", getTransferCategory("account 4"), "-100.50", "payee");
        appendSplitItem(getTransferCategory("account 4"), "-80.00");
        appendSplitItem(getTransferCategory("account 4"), "-20.00");
        appendSplitItem("Bank Chrg", "-0.50");
        endRecord();
        // next account
        appendAccounts(AccountType.BANK, "account 1");
        appendControl("Type", AccountType.BANK.toString());
        appendRecord("9/29/98", "-100.00", getTransferCategory("account 4"));
        FileImport qifImport = qifContext.newQifImport();
        long existing = daoContext.countAll(Transaction.class);

        qifImport.importFile(getReader());

        long imported = daoContext.countAll(Transaction.class) - existing;
        assertThat(imported).isEqualTo(4);
    }

    @Test
    public void testImportSecurity() throws Exception {
        appendSecurity("stock", SecurityType.STOCK);
        appendSecurity("money market", SecurityType.MONEY_MARKET);
        FileImport qifImport = qifContext.newQifImport();
        long existing = daoContext.countAll(Security.class);

        qifImport.importFile(getReader());

        long imported = daoContext.countAll(Security.class) - existing;
        assertThat(imported).isEqualTo(2);
    }
}