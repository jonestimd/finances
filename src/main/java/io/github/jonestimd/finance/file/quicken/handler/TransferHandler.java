// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.file.quicken.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.qif.CategoryParser;
import io.github.jonestimd.finance.file.quicken.qif.QifField;
import io.github.jonestimd.finance.file.quicken.qif.QifRecord;
import io.github.jonestimd.finance.file.quicken.qif.TransferDetailCache;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;

/**
 * TODO This class is only needed because the amount for XOut is positive instead of negative.
 *      Otherwise, could use {@link io.github.jonestimd.finance.file.quicken.qif.MoneyTransactionConverter}.
 */
public class TransferHandler implements SecurityTransactionHandler {
    private AccountOperations accountOperations;
    private TransactionGroupOperations transactionGroupOperations;
    private PayeeOperations payeeOperations;
    private TransferDetailCache pendingTransferDetails;
    private boolean negateAmount = true;

    public TransferHandler(AccountOperations accountOperations, TransactionGroupOperations txGroupOperations,
            PayeeOperations payeeOperations, TransferDetailCache pendingTransferDetails) {
        this(accountOperations, txGroupOperations, payeeOperations, pendingTransferDetails, true);
    }

    public TransferHandler(AccountOperations accountOperations, TransactionGroupOperations txGroupOperations,
            PayeeOperations payeeOperations, TransferDetailCache pendingTransferDetails, boolean transferIn) {
        this.accountOperations = accountOperations;
        this.transactionGroupOperations = txGroupOperations;
        this.payeeOperations = payeeOperations;
        this.pendingTransferDetails = pendingTransferDetails;
        this.negateAmount = ! transferIn;
    }

    public List<Transaction> convertRecord(Account account, QifRecord record) throws QuickenException {
        Date date = record.getDate(DATE);
        CategoryParser parser = new CategoryParser(record.getValue(QifField.TRANSFER_ACCOUNT));
        BigDecimal amount = getAmount(record);
        TransactionGroup group = getGroup(parser);

        List<Transaction> transactions = new ArrayList<Transaction>();
        if (account.getName().equals(parser.getAccountName())) {
            transactions.add(createNonTransfer(account, record, amount, group));
        }
        else {
            Account transferAccount = accountOperations.getAccount(null, parser.getAccountName());
            TransactionDetail detail = pendingTransferDetails.remove(account, parser.getAccountName(), date, amount, group);
            if (detail == null) {
                detail = createTransferDetail(account, transferAccount, group, record);
                transactions.add(new Transaction(transferAccount, detail.getTransaction(), detail.getRelatedDetail()));
                pendingTransferDetails.add(detail.getRelatedDetail());
            }
            Transaction transaction = detail.getTransaction();
            updateTransactionWithRecord(record, transaction);
            transactions.add(transaction);
        }

        return transactions;
    }

    private void updateTransactionWithRecord(QifRecord record, Transaction transaction) {
        if (record.hasValue(PAYEE)) {
            transaction.setPayee(getPayee(record));
        }
        transaction.setCleared(record.hasValue(CLEARED));
        transaction.setMemo(record.getValue(MEMO));
    }

    private Transaction createNonTransfer(Account account, QifRecord record, BigDecimal amount, TransactionGroup group) throws QuickenException {
        TransactionDetail detail = new TransactionDetail(null, amount, record.getMemo(0), group);
        return record.createTransaction(account, getPayee(record), detail);
    }

    private TransactionDetail createTransferDetail(Account account, Account transferAccount, TransactionGroup group, QifRecord record)
            throws QuickenException {
        TransactionDetail detail = new TransactionDetail(getAmount(record), record.getValue(MEMO), group);
        record.createTransaction(account, getPayee(record), detail);
        return detail;
    }

    private Payee getPayee(QifRecord record) {
        String name = record.getValue(PAYEE);
        return name == null ? null : getOrCreatePayee(name);
    }

    private Payee getOrCreatePayee(String name) {
        Payee payee = payeeOperations.getPayee(name);
        return payee == null ? payeeOperations.createPayee(name) : payee;
    }

    private BigDecimal getAmount(QifRecord record) {
        BigDecimal amount = record.getBigDecimal(AMOUNT);
        return negateAmount ? amount.negate() : amount;
    }

    private TransactionGroup getGroup(CategoryParser parser) {
        return parser.hasGroup() ? transactionGroupOperations.getTransactionGroup(parser.getGroupName()) : null;
    }
}