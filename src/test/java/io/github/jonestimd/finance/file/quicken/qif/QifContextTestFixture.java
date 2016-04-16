package io.github.jonestimd.finance.file.quicken.qif;

import java.io.CharArrayReader;
import java.io.Reader;

import io.github.jonestimd.finance.dao.HsqlTestFixture;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.file.quicken.QuickenContext;
import io.github.jonestimd.finance.file.quicken.QuickenRecord;
import io.github.jonestimd.finance.service.ServiceContext;

public abstract class QifContextTestFixture extends HsqlTestFixture {
    protected QuickenContext qifContext;
    protected StringBuilder importText = new StringBuilder();

    public void initDatabase() throws Exception {
        super.initDatabase();
        qifContext = new QuickenContext(new ServiceContext(daoContext));
    }

    protected void appendAccounts(AccountType type, String ... names) {
        for (String name : names) {
            importText.append("!Account\n")
                .append(QifField.NAME.code()).append(name).append('\n')
                .append(QifField.TYPE.code()).append(type.toString()).append('\n');
            endRecord();
        }
    }

    protected void appendSecurity(String name, SecurityType type) {
        appendControl("Type", "Security");
        importText.append(QifField.NAME.code()).append(name).append('\n')
                  .append(QifField.TYPE.code()).append(type.getValue()).append('\n');
        endRecord();
    }

    protected void appendControl(String code, String value) {
        importText.append(QifField.CONTROL.code()).append(code).append(':').append(value).append('\n');
    }

    protected String getTransferCategory(String accountName) {
        return '[' + accountName + ']';
    }

    protected void appendRecord(String date, String amount, String category) {
        beginRecord(date, category, amount, null);
        endRecord();
    }

    protected void endRecord() {
        importText.append(QuickenRecord.END).append('\n');
    }

    protected void beginRecord(String date, String category, String amount, String payee) {
        importText.append(QifField.DATE.code()).append(date).append('\n')
            .append(QifField.AMOUNT.code()).append(amount).append('\n')
            .append(QifField.CATEGORY.code()).append(category).append('\n');
        if (payee != null) importText.append(QifField.PAYEE.code()).append(payee).append('\n');
    }

    protected void appendSplitItem(String category, String amount) {
        importText.append(QifField.SPLIT_CATEGORY.code()).append(category).append('\n')
            .append(QifField.SPLIT_AMOUNT.code()).append(amount).append('\n');
    }

    protected Reader getReader() {
        return new CharArrayReader(importText.toString().toCharArray());
    }
}