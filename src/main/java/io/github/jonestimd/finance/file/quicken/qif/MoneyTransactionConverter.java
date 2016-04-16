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
package io.github.jonestimd.finance.file.quicken.qif;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.operations.PayeeOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;
import io.github.jonestimd.finance.service.TransactionService;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;

public class MoneyTransactionConverter implements RecordConverter {

    private TransactionCategoryOperations TransactionCategoryOperations;
    private TransactionGroupOperations transactionGroupOperations;
    private PayeeOperations payeeOperations;
    private TransactionService transactionService;
    private AccountOperations accountOperations;
    private TransferDetailCache pendingTransferDetails;

    private static final String[] RECORD_TYPES = {
        "Type:Bank", "Type:Cash", "Type:CCard", "Type:Oth L",
    };

    public MoneyTransactionConverter(TransactionCategoryOperations txTypeOperations, TransactionGroupOperations txGroupOperations,
            PayeeOperations payeeOperations, TransactionService txService, AccountOperations accountOperations,
            TransferDetailCache pendingTransferDetails) {
        this.TransactionCategoryOperations = txTypeOperations;
        this.transactionGroupOperations = txGroupOperations;
        this.payeeOperations = payeeOperations;
        this.transactionService = txService;
        this.accountOperations = accountOperations;
        this.pendingTransferDetails = pendingTransferDetails;
    }

    public Set<String> getTypes() {
        return new HashSet<String>(Arrays.asList(RECORD_TYPES));
    }

    private void createDetail(Transaction transaction, String category, String memo, BigDecimal amount, BigDecimal categoryAmount) {
        CategoryParser parser = new CategoryParser(category);
        TransactionGroup group = getGroup(parser);
        // Quicken allows transfers with same from/to account.  Treat them as non-transfers
        if (parser.isTransfer() && ! parser.getAccountName().equals(transaction.getAccount().getName())) {
            addTransferDetail(transaction, parser.getAccountName(), amount, memo, group, categoryAmount);
        }
        else {
            TransactionCategory type = getTransactionCategory(parser);
            TransactionDetail detail = new TransactionDetail(type, amount, memo, group);
            transaction.addDetails(detail);
        }
    }

    private void addTransferDetail(Transaction transaction, String transferAccountName, BigDecimal amount, String memo,
            TransactionGroup group, BigDecimal categoryAmount) {
        TransactionDetail detail = pendingTransferDetails.remove(transaction.getAccount(), transferAccountName,
                transaction.getDate(), categoryAmount, group);
        if (detail == null) {
            createOrUpdateTransfer(transaction, transferAccountName, new TransactionDetail(amount, memo, group), categoryAmount);
        }
        else {
            unmergeCombinedSplitTransfer(detail, amount);
            Transaction pendingTransaction = detail.getTransaction();
            transaction.addDetails(new ArrayList<TransactionDetail>(pendingTransaction.getDetails()));
            transactionService.deleteTransaction(pendingTransaction);
        }
    }

    /**
     * If the transaction already contains a transfer for the same account+group then udpate the related transaction.
     * Otherwise, create a new related transaction for this transfer.
     */
    private void createOrUpdateTransfer(Transaction transaction, String transferAccountName, TransactionDetail detail, BigDecimal categoryAmount) {
        Account transferAccount = accountOperations.getAccount(null, transferAccountName);
        Transaction transfer = transaction.getTransfer(transferAccount, detail.getGroup());
        transaction.addDetails(detail);
        if (transfer == null) {
            createPendingTransfer(transaction, detail.getMemo(), detail, transferAccount, categoryAmount);
        }
        else {
            transfer.addDetails(detail.getRelatedDetail());
        }
        transactionService.saveDetail(detail);
        transactionService.saveDetail(detail.getRelatedDetail());
    }

    /**
     * Check if the transaction could be a combined transfer from a split and, if so, override the amount so the details
     * will remain separate on both related transactions. Quicken uses a single opposing transaction for transfers to
     * the same account and class in a split transaction.
     */
    private void unmergeCombinedSplitTransfer(TransactionDetail detail, BigDecimal amount) {
        if (detail.getTransaction().getDetails().size() == 1) {
            detail.setAmount(amount);
//            detail.getRelatedDetail().setAmount(amount.negate());
        }
    }

    private void createPendingTransfer(Transaction transaction, String memo, TransactionDetail detail, Account account,
            BigDecimal transferTotal) {
        Transaction relatedTransaction = new Transaction(account, transaction.getDate(),
                transaction.getPayee(), transaction.isCleared(), memo, detail.getRelatedDetail());
        transactionService.saveTransaction(relatedTransaction);
        pendingTransferDetails.add(detail.getRelatedDetail(), transferTotal.negate());
    }

    private TransactionCategory getTransactionCategory(CategoryParser parser) {
        return parser.hasCatetories() ? TransactionCategoryOperations.getTransactionCategory(parser.getCategoryNames()) : null;
    }

    private void addDetails(Transaction transaction, QifRecord record) {
        for (int i=0; i<record.getDetailCount(); i++) {
            createDetail(transaction, record.getCategory(i), record.getMemo(i), record.getAmount(i), record.getCategoryAmount(i));
        }
    }

    public void importRecord(AccountHolder accountHolder, QifRecord record) throws QuickenException {
        Account account = accountHolder.getAccount();

        Payee payee = getOrCreatePayee(record.getValue(PAYEE));
        BigDecimal amount = record.getBigDecimal(AMOUNT);

        Transaction transaction = record.createTransaction(account, payee);
        transactionService.saveTransaction(transaction);
        addDetails(transaction, record);
        if (! transaction.getAmount().equals(amount)) {
            throw new QuickenException("invalidSplit", amount, transaction.getAmount(), record.getStartingLine());
        }
    }

    private Payee getOrCreatePayee(String name) {
        if (name == null) return null;
        Payee payee = payeeOperations.getPayee(name);
        return payee == null ? payeeOperations.createPayee(name) : payee;
    }

    private TransactionGroup getGroup(CategoryParser parser) {
        return parser.hasGroup() ? transactionGroupOperations.getTransactionGroup(parser.getGroupName()) : null;
    }
}
