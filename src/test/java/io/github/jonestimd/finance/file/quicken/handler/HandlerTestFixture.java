package io.github.jonestimd.finance.file.quicken.handler;

import java.text.SimpleDateFormat;

import io.github.jonestimd.finance.file.quicken.QuickenContext;
import io.github.jonestimd.finance.file.quicken.qif.QifTestFixture;

public abstract class HandlerTestFixture<HANDLER extends SecurityTransactionHandler> extends QifTestFixture {
    protected SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");

    @SuppressWarnings("unchecked")
    protected HANDLER getHandler(String name) throws Exception {
        QuickenContext qifContext = getQifContext();
        return (HANDLER) qifContext.getSecurityHandler(name);
    }
}