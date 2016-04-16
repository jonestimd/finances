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

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.qif.QifRecord;
import io.github.jonestimd.finance.operations.TransactionCategoryOperations;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;

public class NetAmountSecurityDetailHandler implements SecurityDetailHandler {
    private final TransactionCategory TransactionCategory;

    public NetAmountSecurityDetailHandler(String actionCode,
                                          TransactionCategoryOperations TransactionCategoryOperations) {
        this.TransactionCategory = TransactionCategoryOperations.getSecurityAction(actionCode);
    }

    public void addDetails(QifRecord record, Transaction transaction, boolean isSharesOut) {
        TransactionDetail detail = new TransactionDetail();
        detail.setCategory(TransactionCategory);
        detail.setAmount(getAmount(record));
        transaction.addDetails(detail);
    }

    protected BigDecimal getAmount(QifRecord record) {
        BigDecimal amount = record.getBigDecimal(AMOUNT);
        return TransactionCategory.isIncome() ? amount : amount.negate();
    }
}