package io.github.jonestimd.finance.file.quicken.qif;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.github.jonestimd.cache.ArrayKey;
import io.github.jonestimd.finance.dao.SchemaBuilder;
import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.file.quicken.QuickenContext;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;
import io.github.jonestimd.finance.service.MockServiceContext;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.service.TransactionService;
import org.junit.Before;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.mockito.Mockito.*;

public abstract class QifTestFixture {
    protected PayeeOperations payeeOperations;
    protected TransactionCategoryOperations TransactionCategoryOperations;
    protected TransactionGroupOperations transactionGroupOperations;
    protected TransactionService transactionService;
    protected AssetOperations assetOperations;
    protected AccountOperations accountOperations;

    private ServiceLocator serviceLocator;
    private QuickenContext qifContext;
    protected List<TransactionDetail> pendingTransfers = new ArrayList<TransactionDetail>();
    protected TransferDetailCache pendingTransferDetails;

    protected Account account;
    protected TransactionCategory buyType;
    protected TransactionCategory sellType;
    protected TransactionCategory reinvestType;
    protected TransactionCategory shrsInType;
    protected TransactionCategory shrsOutType;
    protected TransactionCategory divType;
    protected TransactionCategory interestType;
    protected TransactionCategory commissionType;

    protected Map<String, Payee> payees = new HashMap<String, Payee>();
    protected Map<ArrayKey, TransactionCategory> transactionCategories = new HashMap<ArrayKey, TransactionCategory>();
    protected Map<String, TransactionCategory> securityActions = new HashMap<String, TransactionCategory>();
    protected Map<String, Security> securities = new HashMap<String, Security>();

    @Before
    public void setUpHandler() throws Exception {
        serviceLocator = new MockServiceContext();
        payeeOperations = serviceLocator.getPayeeOperations();
        TransactionCategoryOperations = serviceLocator.getTransactionCategoryOperations();
        transactionGroupOperations = serviceLocator.getTransactionGroupOperations();
        transactionService = serviceLocator.getTransactionService();
        assetOperations = serviceLocator.getAssetOperations();
        accountOperations = serviceLocator.getAccountOperations();
        account = TestDomainUtils.createAccount("account", getAccountType());
        resetMocks();
    }

    protected abstract AccountType getAccountType();

    public void resetMocks() throws Exception {
        reset(payeeOperations, TransactionCategoryOperations, transactionGroupOperations,
                transactionService, assetOperations, accountOperations);

        payees.clear();
        transactionCategories.clear();
        for (TransactionCategory action : SchemaBuilder.getSecurityActions(new Date())) {
            securityActions.put(action.getCode(), action);
            when(TransactionCategoryOperations.getSecurityAction(action.getCode())).thenReturn(action);
        }
        securities.clear();

        buyType = securityActions.get(SecurityAction.BUY.code());
        sellType = securityActions.get(SecurityAction.SELL.code());
        reinvestType = securityActions.get(SecurityAction.REINVEST.code());
        shrsInType = securityActions.get(SecurityAction.SHARES_IN.code());
        shrsOutType = securityActions.get(SecurityAction.SHARES_OUT.code());
        divType = securityActions.get(SecurityAction.DIVIDEND.code());
        interestType = securityActions.get(SecurityAction.INTEREST.code());
        commissionType = securityActions.get(SecurityAction.COMMISSION_AND_FEES.code());
    }

    protected QuickenContext getQifContext() {
        if (qifContext == null) {
            qifContext = new QuickenContext(serviceLocator);
            pendingTransferDetails = qifContext.getTransferDetailCache();
            for (TransactionDetail transactionDetail : pendingTransfers) {
                pendingTransferDetails.add(transactionDetail, transactionDetail.getTransaction().getAmount());
            }
        }
        return qifContext;
    }

    protected QifRecord createRecord(String date, String payee, String amount, String memo, boolean cleared) {
        QifRecord record = new QifRecord(1L);
        record.setValue(DATE, date);
        record.setValue(PAYEE, payee);
        record.setValue(AMOUNT, amount);
        record.setValue(MEMO, memo);
        if (cleared) {
            record.setValue(CLEARED, "X");
        }
        return record;
    }

    protected Payee addPayee(QifRecord record) throws NoSuchMethodException {
        String payeeName = record.getValue(PAYEE);
        Payee payee = payees.get(payeeName);
        if (payee == null) {
            payee = new Payee();
            payee.setName(payeeName);
            payees.put(payeeName, payee);
            when(payeeOperations.createPayee(payeeName)).thenReturn(payee);
        }
        return payee;
    }

    protected TransactionCategory addTransactionCategory(String description, boolean income, String ... codes) throws Exception {
        TransactionCategory type = transactionCategories.get(new ArrayKey(codes));
        if (type == null) {
            type = createTransactionCategory(description, income, codes);
            when(TransactionCategoryOperations.getTransactionCategory(codes)).thenReturn(type);
        }
        return type;
    }

    private TransactionCategory createTransactionCategory(String description, boolean income, String... codes) {
        TransactionCategory type = new TransactionCategory();
        type.setDescription(description);
        type.setCode(codes[0]);
        type.setIncome(income);
        type.setSecurity(false);
        transactionCategories.put(new ArrayKey(codes), type);
        return type;
    }

    protected Security addSecurity(QifRecord record) throws Exception {
        return addSecurity(record.getValue(SECURITY));
    }

    protected Security addSecurity(String securityName) throws Exception {
        Security security = securities.get(securityName);
        if (security == null) {
            security = new Security();
            security.setName(securityName);
            securities.put(securityName, security);
            when(assetOperations.findOrCreate(securityName)).thenReturn(security);
        }
        return security;
    }

    protected TransactionDetail addPendingTransfer(TransactionGroup group, QifRecord record) throws Exception {
        return addPendingTransfer(group, record, 0);
    }

    protected TransactionDetail addPendingTransfer(TransactionGroup group, QifRecord record, int detailIndex) throws Exception {
        String category = record.getCategory(detailIndex);
        BigDecimal amount = record.getAmount(detailIndex);
        CategoryParser parser = new CategoryParser(category);
        Account transferAccount = TestDomainUtils.createAccount(parser.getAccountName(), getAccountType());
        TransactionDetail transferDetail = new TransactionDetail(amount.negate(), null, group);
        record.createTransaction(transferAccount, null, transferDetail);
        record.createTransaction(account, null, transferDetail.getRelatedDetail());
        pendingTransfers.add(transferDetail.getRelatedDetail());
        return transferDetail.getRelatedDetail();
    }

    protected TransactionDetail mergeTransfers(List<TransactionDetail> transferDetails) {
        Iterator<TransactionDetail> iterator = transferDetails.iterator();
        TransactionDetail mergedDetail = iterator.next();
        while (iterator.hasNext()) {
            TransactionDetail detail = iterator.next();
            mergedDetail.setAmount(mergedDetail.getAmount().add(detail.getAmount()));
            pendingTransfers.remove(detail);
            iterator.remove();
        }
        return mergedDetail;
    }
}