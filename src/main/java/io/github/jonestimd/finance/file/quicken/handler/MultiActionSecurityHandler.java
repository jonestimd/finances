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

import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.file.quicken.qif.QifField;
import io.github.jonestimd.finance.file.quicken.qif.QifRecord;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;

public class MultiActionSecurityHandler implements SecurityTransactionHandler {
    private final AssetOperations assetOperations;
    private final TransactionCategory commissionType;
    private List<String> sharesOutActions;
    private SecurityDetailHandler[] detailHandlers;

    public MultiActionSecurityHandler(List<String> sharesOutActions, AssetOperations assetOperations,
                                      TransactionCategoryOperations transactionCategoryOperations, SecurityDetailHandler ... detailHandlers) {
        this.sharesOutActions = sharesOutActions;
        this.assetOperations = assetOperations;
        this.detailHandlers = detailHandlers;
        commissionType = transactionCategoryOperations.getSecurityAction(SecurityAction.COMMISSION_AND_FEES.code());
    }

    public final List<Transaction> convertRecord(Account account, QifRecord record) throws QuickenException {
        Transaction transaction = record.createSecurityTransaction(account);
        transaction.setSecurity(getSecurity(record));
        addDetails(record, transaction, isSharesOutAction(record));
        addCommissionDetail(record, transaction);
        return Collections.singletonList(transaction);
    }

    private Security getSecurity(QifRecord record) {
        return record.hasValue(QifField.SECURITY) ? assetOperations.findOrCreate(record.getValue(QifField.SECURITY)) : null;
    }

    protected void addDetails(QifRecord record, Transaction transaction, boolean isSharesOut) throws QuickenException {
        for (SecurityDetailHandler handler : detailHandlers) {
            handler.addDetails(record, transaction, isSharesOut);
        }
    }

    protected void addCommissionDetail(QifRecord record, Transaction transaction) {
        if (record.hasValue(COMMISSION)) {
            TransactionDetail detail = new TransactionDetail();
            detail.setCategory(commissionType);
            detail.setAmount(record.getBigDecimal(COMMISSION).negate());
            transaction.addDetails(detail);
        }
    }

    protected boolean isSharesOutAction(QifRecord record) {
        return sharesOutActions.contains(record.getValue(SECURITY_ACTION));
    }
}